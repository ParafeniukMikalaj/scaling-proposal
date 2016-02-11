package hashing.impl;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hashing.HashRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final int partitionsCount;
    private final int splitPointsNumber;

    private final Random random;

    public HashRingImpl(int partitionsCount, int splitPointsNumber) {
        this.partitionsCount = partitionsCount;
        this.splitPointsNumber = splitPointsNumber;
        this.random = new Random();
    }

    @Override
    public Collection<Integer> generateSplitPoints(Integer nodeId) {
        return IntStream
                .range(0, splitPointsNumber)
                .map(x -> random.nextInt(partitionsCount))
                .boxed()
                .collect(Collectors.toList());
    }

    @Override
    public Integer hash(Integer value, Multimap<Integer, Integer> coordinatorState) {
        int partition = value.hashCode() % partitionsCount;
        SortedSet<HashRingEntry> ringEntries = buildRingEntries(coordinatorState);
        return getPartitionOwner(ringEntries, partition);
    }

    @Override
    public Collection<Integer> getPartitions(Integer nodeId, Multimap<Integer, Integer> coordinatorState) {
        SortedSet<HashRingEntry> ringEntries = buildRingEntries(coordinatorState);
        return IntStream.range(0, partitionsCount)
                .filter(p -> getPartitionOwner(ringEntries, p).equals(nodeId))
                .boxed()
                .collect(Collectors.toList());
    }

    private Integer getPartitionOwner(SortedSet<HashRingEntry> ringEntries, int partition) {
        Integer ownerId;
        SortedSet<HashRingEntry> tailEntries = ringEntries.tailSet(new HashRingEntry(Integer.MAX_VALUE, partition));
        ownerId = tailEntries.isEmpty() ? ringEntries.first().nodeId() : tailEntries.first().nodeId();
        return ownerId;
    }

    private SortedSet<HashRingEntry> buildRingEntries(Multimap<Integer, Integer> states) {
        SortedSet<HashRingEntry> ringEntries = Sets.newTreeSet();
        for (Map.Entry<Integer, Integer> entry : states.entries()) {
            Integer currentNodeId = entry.getKey();
            Integer splitPoint = entry.getValue();
            ringEntries.add(new HashRingEntry(currentNodeId, splitPoint));
        }
        return ringEntries;
    }

    public static final Logger logger = LoggerFactory.getLogger(HashRingImpl.class);
}
