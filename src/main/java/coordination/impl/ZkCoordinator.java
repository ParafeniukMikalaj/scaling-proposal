package coordination.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import common.Service;
import coordination.CoordinatedNode;
import coordination.Coordinator;
import coordination.CoordinatorListener;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ZkCoordinator implements Coordinator, Service {

    private class ZkStateWatcher implements Watcher {
        private boolean stopped;

        @Override
        public void process(WatchedEvent event) {
            if (stopped) {
                logger.warn("Unexpected behaviour. Stopped watcher continues to receive events.");
                return;
            }
            Event.KeeperState state = event.getState();
            logger.info("Zookeeper state changed to {}", state);
            switch (state) {
                case SyncConnected:
                    onZkConnected();
                    break;
                case ConnectedReadOnly:
                    onZkReadOnly();
                    break;
                case Disconnected:
                    onZkDisconnected();
                    break;
                case Expired:
                    this.stopped = true;
                    establishConnection();
                    break;
                default:
                    logger.info("ZooKeeper connection changed state to {}. "
                            + "We don't care about SaslAuthenticated, AuthFailed events", state);
            }
        }
    }

    private class ZkPathWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            logger.info("Event {} on {} path occurred: ", event.getPath(), event.getType());
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                onChildNodesChanged();
            }
        }
    }

    private static final int SESSION_TIMEOUT = 6000;
    private final String zkConnectionString;
    private final String zkPath;

    private ZooKeeper zk;
    private ZkStateWatcher stateWatcher;
    private ZkPathWatcher pathWatcher;
    private Kryo kryo = new Kryo();

    private boolean connected;
    private boolean readOnly;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private Map<Integer, CoordinatedNode> nodesState;
    private Queue<NodeOperation> pendingOperations = Queues.newArrayDeque();

    private Collection<CoordinatorListener> listeners = Lists.newArrayList();

    private final Object lock = new Object();

    public ZkCoordinator(String zkConnectionString, String zkPath) {
        logger.info("Creating zookeeper coordinator");
        Preconditions.checkArgument(!zkConnectionString.isEmpty(), "ZooKeeper connections can't be empty");
        this.zkConnectionString = zkConnectionString;
        this.zkPath = zkPath.startsWith("/") ? zkPath : "/" + zkPath;
    }

    @Override
    public void start() {
        logger.info("Starting zookeeper coordinator");
        establishConnection();
    }

    @Override
    public void stop() {
        logger.info("Stopping zookeeper coordinator");
        try {
            zk.close();
        } catch (InterruptedException e) {
            logger.error("Error while closing zk connection", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void join(CoordinatedNode node) {
        logger.info("Coordinated node {} is joining", node);
        synchronized (lock) {
            if (connected && !readOnly) {
                logger.info("zk is connected and writable. Will attempt registering node");
                registerNode(node);
            } else {
                logger.info("zk is not writable, postpone join operation");
                addPendingJoinOperation(node);
            }
        }
    }

    @Override
    public void leave(CoordinatedNode node) {
        logger.info("Coordinated node {} is leaving", node);
        synchronized (lock) {
            if (connected && !readOnly) {
                logger.info("zk is connected and writable. Will attempt unregister node");
                unregisterNode(node);
            } else {
                logger.info("zk is not writable, postpone leave operation");
                addPendingLeaveOperation(node);
            }
        }
    }

    @Override
    public void subscribe(CoordinatorListener listener) {
        logger.info("Subscription event");
        synchronized (lock) {
            if (nodesState != null && !nodesState.isEmpty()) {
                logger.info("Have nodes state info. Subscriber will be notified immediately");
                listener.onStateUpdate(ImmutableList.copyOf(nodesState.values()));
            }
            logger.info("Empty nodes state info. Subscriber will be notified later");
            listeners.add(listener);
        }
    }

    private void onZkConnected() {
        logger.info("Zk connection established");
        synchronized (lock) {
            this.connected = true;
            this.readOnly = false;

            verifyRootPathExists();

            logger.info("Have {} pending operations", pendingOperations.size());
            while (!pendingOperations.isEmpty()) {
                performOperation(pendingOperations.poll());
            }

            logger.info("Will attempt to read state and notify listeners");
            readNodeStateAndNotifyListeners();
        }
    }

    private void verifyRootPathExists() {
        try {
            Stat stat = zk.exists(zkPath, false);
            logger.info("Zk path {} {}", zkPath, stat != null ? "exists" : "doesn't exist");
            if (stat == null) {
                logger.info("Creating zk path {}", zkPath);
                zk.create(zkPath, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            logger.error("Error while checking path {} for existence", zkPath);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while checking {} path for existence", zkPath);
            Thread.currentThread().interrupt();
        }
    }

    private void onZkReadOnly() {
        logger.info("Zk connection established. Read only!");
        synchronized (lock) {
            this.readOnly = true;

            logger.info("Will attempt to read state and notify listeners");
            readNodeStateAndNotifyListeners();
        }
    }

    private void onZkDisconnected() {
        logger.info("Zk disconnected");
        synchronized (lock) {
            this.connected = false;
        }
    }

    private void onChildNodesChanged() {
        logger.info("Child nodes changed");
        synchronized (lock) {
            readNodeStateAndNotifyListeners();
        }
    }

    private void readNodeStateAndNotifyListeners() {
        Map<Integer, CoordinatedNode> updatedNodesState = readState();
        logger.info("Reading {} nodes state from zookeeper", updatedNodesState != null ? "not empty" : "empty");
        if (updatedNodesState != null) {
            nodesState = updatedNodesState;
            logger.info("Notifying {} listeners about state changes", listeners.size());
            for (CoordinatorListener listener : listeners) {
                listener.onStateUpdate(ImmutableList.copyOf(nodesState.values()));
            }
        }
    }

    private void performOperation(NodeOperation operation) {
        if (operation.type() == NodeOperation.Type.JOIN) {
            logger.info("Performing postponed join operation");
            registerNode(operation.node());
        } else if (operation.type() == NodeOperation.Type.LEAVE) {
            logger.info("Performing postponed leave operation");
            unregisterNode(operation.node());
        }
    }

    private void registerNode(CoordinatedNode node) {
        logger.info("Registering node {} in zookeeper", node);
        Output output = new Output(1000);
        kryo.writeObject(output, node);
        try {
            zk.create(zkPath + "/" + node, output.toBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            logger.info("Registration success");
        } catch (KeeperException e) {
            logger.info("Error during node registration. Adding operation to retry queue", e);
            addPendingJoinOperation(node);
        } catch (InterruptedException e) {
            logger.info("Interrupted exception while registering node", e);
            Thread.currentThread().interrupt();
        }
    }

    private Map<Integer, CoordinatedNode> readState() {
        logger.info("Reading nodes states");
        try {
            Map<Integer, CoordinatedNode> result = Maps.newHashMap();
            List<String> children = zk.getChildren(zkPath, pathWatcher);
            logger.info("Got {} child nodes", children.size());
            for (String child : children) {
                logger.info("Reading state of node {}", child);
                Input input = new Input(zk.getData(zkPath + "/" + child, pathWatcher, null));
                CoordinatedNode node = kryo.readObject(input, CoordinatedNodeImpl.class);
                result.put(node.id(), node);
            }
            logger.info("Read nodes state success");
            return result;
        } catch (KeeperException e) {
            logger.warn("Error while reading available nodes", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted exception while reading available nodes", e);
        }
        return null;
    }

    private void unregisterNode(CoordinatedNode node) {
        logger.info("Unregistering node {}", node);
        try {
            zk.delete(zkPath + "/" + node.id(), 0);
            logger.info("Node unregistered");
        } catch (KeeperException e) {
            logger.info("Error during node unregistration. Adding operation to retry queue", e);
            addPendingLeaveOperation(node);
        } catch (InterruptedException e) {
            logger.info("Interrupted exception while unregistering node", e);
            Thread.currentThread().interrupt();
        }
    }

    private void establishConnection() {
        logger.info("Establishing connection to ZooKeeper with connection string {}", zkConnectionString);
        stateWatcher = new ZkStateWatcher();
        pathWatcher = new ZkPathWatcher();
        try {
            zk = new ZooKeeper(zkConnectionString, SESSION_TIMEOUT, stateWatcher);
        } catch (IOException e) {
            logger.error("Network error while connecting to ZooKeeper. Retry in 2 seconds.", e);
            executor.schedule((Runnable) this::establishConnection, 2, TimeUnit.SECONDS);
        }
    }

    private void addPendingJoinOperation(CoordinatedNode node) {
        logger.info("Add pending join operation");
        pendingOperations.add(new NodeOperation(node, NodeOperation.Type.JOIN));
    }

    private void addPendingLeaveOperation(CoordinatedNode node) {
        logger.info("Add pending leave operation");
        pendingOperations.add(new NodeOperation(node, NodeOperation.Type.LEAVE));
    }

    private static final Logger logger = LoggerFactory.getLogger(ZkCoordinator.class);
}