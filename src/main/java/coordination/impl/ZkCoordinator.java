package coordination.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import coordination.CoordinatedNode;
import coordination.Coordinator;
import coordination.CoordinatorListener;
import org.apache.zookeeper.*;
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
import java.util.stream.Collectors;

public class ZkCoordinator implements Coordinator {

    private class ZkStateWatcher implements Watcher {
        private boolean stopped;

        @Override
        public void process(WatchedEvent event) {
            if (stopped) {
                logger.warn("Unexpected behaviour. Stopped watcher continues to receive events.");
                return;
            }
            Event.KeeperState state = event.getState();
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
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                onChildNodesChanged();
            }
        }
    }

    private static final int SESSION_TIMEOUT = 6000;
    private final Collection<ZkConnection> zkConnections;
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

    public ZkCoordinator(Collection<ZkConnection> zkConnections, String zkPath) throws IOException {
        Preconditions.checkArgument(!zkConnections.isEmpty(), "ZooKeeper connections can't be empty");
        this.zkConnections = ImmutableList.copyOf(zkConnections);
        this.zkPath = zkPath;
        establishConnection();
    }

    @Override
    public void join(CoordinatedNode node) {
        synchronized (lock) {
            if (connected && !readOnly) {
                registerNode(node);
            } else {
                addPendingJoinOperation(node);
            }
        }
    }

    @Override
    public void leave(CoordinatedNode node) {
        synchronized (lock) {
            if (connected && !readOnly) {
                unregisterNode(node);
            } else {
                addPendingLeaveOperation(node);
            }
        }
    }

    @Override
    public void subscribe(CoordinatorListener listener) {
        synchronized (lock) {
            if (nodesState != null && !nodesState.isEmpty()) {
                listener.onStateUpdate(ImmutableList.copyOf(nodesState.values()));
            }
            listeners.add(listener);
        }
    }

    private void onZkConnected() {
        synchronized (lock) {
            this.connected = true;
            this.readOnly = false;

            while (!pendingOperations.isEmpty()) {
                performOperation(pendingOperations.poll());
            }

            readNodeStateAndNotifyListeners();
        }
    }

    private void onZkReadOnly() {
        synchronized (lock) {
            this.readOnly = true;

            readNodeStateAndNotifyListeners();
        }
    }

    private void onZkDisconnected() {
        synchronized (lock) {
            this.connected = false;
        }
    }

    private void onChildNodesChanged() {
        synchronized (lock) {
            readNodeStateAndNotifyListeners();
        }
    }

    private void readNodeStateAndNotifyListeners() {
        Map<Integer, CoordinatedNode> updatedNodesState = readState();
        if (updatedNodesState != null) {
            nodesState = updatedNodesState;
        }
        for (CoordinatorListener listener : listeners) {
            listener.onStateUpdate(ImmutableList.copyOf(nodesState.values()));
        }
    }

    private void performOperation(NodeOperation operation) {
        if (operation.type() == NodeOperation.Type.JOIN) {
            registerNode(operation.node());
        } else if (operation.type() == NodeOperation.Type.LEAVE) {
            unregisterNode(operation.node());
        }
    }

    private void registerNode(CoordinatedNode node) {
        Output output = new Output();
        kryo.writeObject(output, node);
        try {
            zk.create(zkPath + "/" + node, output.toBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException e) {
            addPendingJoinOperation(node);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<Integer, CoordinatedNode> readState() {
        try {
            Map<Integer, CoordinatedNode> result = Maps.newHashMap();
            List<String> children = zk.getChildren(zkPath, pathWatcher);
            for (String child : children) {
                Input input = new Input(zk.getData(child, pathWatcher, null));
                CoordinatedNode node = kryo.readObject(input, CoordinatedNode.class);
                result.put(node.id(), node);
            }
            return result;
        } catch (KeeperException e) {
            logger.warn("Error while reading available nodes", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void unregisterNode(CoordinatedNode node) {
        try {
            zk.delete(zkPath + "/" + node.id(), 0);
        } catch (KeeperException e) {
            addPendingLeaveOperation(node);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void establishConnection() {
        String zkConnectionString = createZkConnectionString(zkConnections);
        logger.debug("Establishing connection to ZooKeeper with connection string {}", zkConnectionString);
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
        pendingOperations.add(new NodeOperation(node, NodeOperation.Type.JOIN));
    }

    private void addPendingLeaveOperation(CoordinatedNode node) {
        pendingOperations.add(new NodeOperation(node, NodeOperation.Type.LEAVE));
    }

    private String createZkConnectionString(Collection<ZkConnection> zkConnections) {
        return Joiner.on(",").join(zkConnections.stream()
                .map(c -> c.host() + ":" + c.port())
                .collect(Collectors.toList()));
    }

    private static final Logger logger = LoggerFactory.getLogger(ZkCoordinator.class);
}