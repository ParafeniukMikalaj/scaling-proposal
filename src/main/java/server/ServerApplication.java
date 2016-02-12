package server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import coordination.CoordinatedNode;
import coordination.Coordinator;
import coordination.CoordinatorListener;
import coordination.impl.CoordinatedNodeImpl;
import hashing.HashRing;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaConsumerListener;
import model.Node;
import model.impl.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import server.impl.ServerImpl;

import java.util.Collection;

public class ServerApplication implements CoordinatorListener, TestKafkaConsumerListener, ServerContainer {

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

    public ServerApplication() {
    }

    public void start() {
        server  = new ServerImpl(port, this);
        Collection<Integer> splitPoints = hashRing.generateSplitPoints(nodeId);
        Node node = new NodeImpl(nodeId, host, port);
        CoordinatedNode coordinatedNode = new CoordinatedNodeImpl(node, splitPoints);
        coordinator.join(coordinatedNode);
        coordinator.subscribe(this);
        consumer.setListener(this);
    }

    public void stop() {
        logger.warn("Stopping application");
    }

    @Override
    public void onStateUpdate(Collection<CoordinatedNode> nodes) {
        Multimap<Integer, Integer> coordinationState = buildCoordinationState(nodes);
        updateConsumerPartitions(coordinationState);
        disconnectNotOwnedClients(coordinationState);
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
        Collection<Integer> ownedPartitions = hashRing.getPartitions(nodeId, coordinationState);
        consumer.setPartitions(ownedPartitions);
    }

    private void disconnectNotOwnedClients(Multimap<Integer, Integer> coordinationState) {
        server.connectedClients().stream()
                .filter(client -> hashRing.hash(client, coordinationState) != nodeId)
                .forEach(client -> server.disconnectClient(client));
    }

    @Override
    public void consume(String record) {
        String[] parts = record.split(" ");
        int clientId = Integer.parseInt(parts[0]);
        String message = parts[1];
        if (server.containsClient(clientId)) {
            server.sendMessage(clientId, message);
        }
    }

    @Override
    public Node getNode(int clientId) {
        return null;
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
}
