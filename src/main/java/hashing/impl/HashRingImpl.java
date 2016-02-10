package hashing.impl;

import com.google.common.collect.Lists;
import coordination.CoordinatedNode;
import coordination.impl.CoordinatedNodeImpl;
import model.Node;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import hashing.HashRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HashRingImpl implements HashRing<Integer, Integer> {

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

    private final Set<Integer> nodeIds = Sets.newHashSet();
    private SortedSet<HashRingEntry> ringEntries;
    private final Random random;

    public HashRingImpl(int maxValue, int splitPointsNumber) {
        this.maxValue = maxValue;
        this.splitPointsNumber = splitPointsNumber;
        this.random = new Random();
        this.ringEntries = Sets.newTreeSet();
    }

    public Integer hash(Integer value) {
        if (ringEntries.isEmpty()) {
            logger.warn("Empty entries set can't hash value");
            return null;
        }

        int point = value.hashCode() % maxValue;
        SortedSet<HashRingEntry> tailEntries = ringEntries.tailSet(new HashRingEntry(Integer.MAX_VALUE, point));
        return tailEntries.isEmpty() ? ringEntries.first().nodeId() :tailEntries.first().nodeId();
    }

    public void remove(final Integer nodeId) {
        ringEntries = ringEntries.stream()
                .filter(e -> e.nodeId() != nodeId)
                .collect(Collectors.toCollection(TreeSet::new));

        nodeIds.remove(nodeId);
    }

    public Collection<Integer> add(Integer nodeId) {
        if (nodeId < 0 && nodeId >= maxValue) {
            logger.warn("Node ignored. Node id {} is not in range of managed nodes [0, {}).", nodeId, maxValue);
            return null;
        }


        if (nodeIds.contains(nodeId)) {
            logger.warn("Node with id {} is already added", nodeId);
            return null;
        }

        nodeIds.add(nodeId);

        List<Integer> splitPoints = Lists.newArrayList();
        for (int i = 0; i < splitPointsNumber; i++) {
            int splitPoint = random.nextInt(maxValue);
            splitPoints.add(splitPoint);
            ringEntries.add(new HashRingEntry(nodeId, splitPoint));
        }

        return splitPoints;
    }

    public Collection<Integer> filterNotOwnedValues(Integer nodeId, Collection<Integer> ownedValues) {
        return ownedValues.stream().filter(v -> !(hash(v).equals(nodeId))).collect(Collectors.toList());
    }

    public static final Logger logger = LoggerFactory.getLogger(HashRingImpl.class);
}
