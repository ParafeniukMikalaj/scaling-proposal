package server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import common.Service;
import coordination.CoordinatedNode;
import coordination.Coordinator;
import coordination.CoordinatorListener;
import coordination.impl.CoordinatedNodeImpl;
import hashing.HashRing;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaConsumerListener;
import model.Node;
import model.Range;
import model.impl.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import server.impl.ServerImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerApplication implements CoordinatorListener, TestKafkaConsumerListener, ServerContainer, Service {

    private Server server;

    @Autowired
    private HashRing<Integer, Integer> hashRing;

    @Autowired
    private Coordinator coordinator;

    @Autowired
    private TestKafkaConsumer consumer;

    @Autowired
    private Integer nodeId;

    @Autowired
    private String host;

    @Autowired
    private Integer port;

    private Multimap<Integer, Integer> hashRingState;
    private Map<Integer, Node> latestCoordinationInfo = Maps.newHashMap();

    @Override
    public void start() {
        logger.info("Starting application");
        server = new ServerImpl(port, this);
        Collection<Integer> splitPoints = hashRing.generateSplitPoints(nodeId);
        Node node = new NodeImpl(nodeId, host, port);
        CoordinatedNode coordinatedNode = new CoordinatedNodeImpl(node, splitPoints);
        coordinator.join(coordinatedNode);
        coordinator.subscribe(this);
        consumer.setListener(this);

        coordinator.start();
        consumer.start();
    }

    @Override
    public void stop() {
        logger.warn("Stopping application");
        consumer.stop();
        coordinator.stop();
    }

    @Override
    public void onStateUpdate(Collection<CoordinatedNode> nodes) {
        latestCoordinationInfo.clear();
        nodes.stream().forEach(n -> latestCoordinationInfo.put(n.id(), n));
        logger.info("Received state update {}", nodes);
        hashRingState = buildCoordinationState(nodes);
        updateConsumerPartitions(hashRingState);
        disconnectNotOwnedClients(hashRingState);
    }

    private Multimap<Integer, Integer> buildCoordinationState(Collection<CoordinatedNode> nodes) {
        Multimap<Integer, Integer> coordinationState = ArrayListMultimap.create();
        for (CoordinatedNode node : nodes) {
            for (Integer splitPoints : node.splitPoints()) {
                coordinationState.put(node.id(), splitPoints);
            }
        }
        return coordinationState;
    }

    private void updateConsumerPartitions(Multimap<Integer, Integer> coordinationState) {
        Collection<Range> ownedPartitions = hashRing.getPartitions(nodeId, coordinationState);
        logger.info("Updating owned partition of consumer to {}", ownedPartitions);
        consumer.setPartitions(ownedPartitions);
    }

    private void disconnectNotOwnedClients(Multimap<Integer, Integer> coordinationState) {
        List<Integer> notOwnedClients = server.connectedClients().stream()
                .filter(client -> hashRing.hash(client, coordinationState).equals(nodeId))
                .collect(Collectors.toList());
        logger.info("Disconnecting {} not owned clients", notOwnedClients.size());
        notOwnedClients.stream().forEach(client -> server.disconnectClient(client));
    }

    @Override
    public void consume(String record) {
        logger.info("received record from consumer");
        String[] parts = record.split(" ");
        int clientId = Integer.parseInt(parts[0]);
        String message = parts[1];
        if (server.containsClient(clientId)) {
            server.sendMessage(clientId, message);
        }
    }

    @Override
    public Node getNode(int clientId) {
        logger.info("Client {} requests owner node", clientId);
        if (latestCoordinationInfo == null) {
            logger.info("Don't have coordination info. Owner request failure");
            return null;
        }
        Integer nodeId = hashRing.hash(clientId, hashRingState);
        Node ownerNode = latestCoordinationInfo.get(nodeId);
        logger.info("Resolving client {} to {} node", clientId, nodeId);
        return ownerNode;
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
}
