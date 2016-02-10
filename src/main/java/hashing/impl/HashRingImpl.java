package hashing.impl;

import model.Node;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import hashing.HashRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HashRingImpl implements HashRing<Node, Integer> {

    private static class HashRingEntry implements Comparable<HashRingEntry> {
        private final int nodeId;
        private final int splitPoint;

        public HashRingEntry(int nodeId, int splitPoint) {
            this.nodeId = nodeId;
            this.splitPoint = splitPoint;
        }

        public int nodeId() {
            return nodeId;
        }

        public int splitPoint() {
            return splitPoint;
        }

        public int compareTo(HashRingEntry other) {
            if (this.splitPoint != other.splitPoint) {
                return this.splitPoint - other.splitPoint;
            }
            return this.nodeId - other.nodeId;
        }

        @Override
        public String toString() {
            return splitPoint + ":" + nodeId;
        }
    }

    private final int maxValue;
    private final int splitPointsNumber;

    private final Map<Integer, Node> nodeMap;
    private SortedSet<HashRingEntry> ringEntries;
    private final Random random;

    public HashRingImpl(int maxValue, int splitPointsNumber) {
        this.maxValue = maxValue;
        this.splitPointsNumber = splitPointsNumber;
        this.nodeMap = Maps.newHashMap();
        this.random = new Random();
        this.ringEntries = Sets.newTreeSet();
    }

    public Node hash(Integer value) {
        if (ringEntries.isEmpty()) {
            logger.warn("Empty entries set can't hash value");
            return null;
        }

        int point = value.hashCode() % maxValue;
        SortedSet<HashRingEntry> tailEntries = ringEntries.tailSet(new HashRingEntry(Integer.MAX_VALUE, point));
        return tailEntries.isEmpty() ? getNode(ringEntries.first()) : getNode(tailEntries.first());
    }

    public void remove(final Node node) {
        ringEntries = ringEntries.stream()
                .filter(e -> e.nodeId() != node.id())
                .collect(Collectors.toCollection(TreeSet::new));

        nodeMap.remove(node.id());
    }

    public void add(Node node) {
        int nodeId = node.id();
        if (nodeId < 0 && nodeId >= maxValue) {
            logger.warn("Node ignored. Node id {} is not in range of managed nodes [0, {}).", nodeId, maxValue);
            return;
        }

        if (nodeMap.containsKey(nodeId)) {
            logger.warn("Node with id {} is already added", nodeId);
            return;
        }

        nodeMap.put(nodeId, node);

        for (int i = 0; i < splitPointsNumber; i++) {
            int splitPoint = random.nextInt(maxValue);
            ringEntries.add(new HashRingEntry(nodeId, splitPoint));
        }
    }

    public Collection<Integer> filterNotOwnedValues(Node node, Collection<Integer> ownedValues) {
        return ownedValues.stream().filter(v -> hash(v).id() != node.id()).collect(Collectors.toList());
    }

    private Node getNode(HashRingEntry entry) {
        return nodeMap.get(entry.nodeId);
    }

    public static final Logger logger = LoggerFactory.getLogger(HashRingImpl.class);
}
