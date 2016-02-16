package hashing.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hashing.HashRing;
import model.NodeRange;
import model.Range;
import model.impl.NodeRangeImpl;
import model.impl.RangeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        List<Integer> splitPoints = IntStream
                .range(0, splitPointsNumber)
                .map(x -> random.nextInt(partitionsCount) + 1)
                .boxed()
                .collect(Collectors.toList());
        logger.info("Generated {} split points for node {}", splitPoints, nodeId);
        return splitPoints;
    }

    @Override
    public Integer hash(Integer value, Multimap<Integer, Integer> coordinatorState) {
        int partition = value.hashCode() % partitionsCount;
        SortedSet<HashRingEntry> ringEntries = buildRingEntries(coordinatorState);
        Integer partitionOwner = getPartitionOwner(ringEntries, partition);
        logger.info("Request to hash value {}: {} belongs to partition {}, which belongs to node {}", value, value,
                partition, partitionOwner);
        return partitionOwner;
    }

    @Override
    public Collection<Range> getPartitions(Integer nodeId, Multimap<Integer, Integer> coordinatorState) {
        SortedSet<HashRingEntry> ringEntries = buildRingEntries(coordinatorState);
        Collection<NodeRange> ranges = Lists.newArrayList();
        Integer rangeStart = 0;
        Integer rangeEnd = 0;
        Integer previousNode = ringEntries.first().nodeId();
        for (HashRingEntry entry : ringEntries) {
            int splitPoint = entry.splitPoint();
            int currentNode = entry.nodeId();
            if (splitPoint - rangeEnd > 0) {
                if (currentNode != previousNode) {
                    ranges.add(new NodeRangeImpl(rangeStart, rangeEnd, previousNode));
                    rangeStart = rangeEnd;
                    rangeEnd = splitPoint;
                    previousNode = currentNode;
                } else {
                    rangeEnd = splitPoint;
                }
            }
        }

        int firstNode = ringEntries.first().nodeId();
        if (firstNode != previousNode) {
            ranges.add(new NodeRangeImpl(rangeStart, rangeEnd, previousNode));
            ranges.add(new NodeRangeImpl(rangeEnd, partitionsCount, firstNode));
        } else {
            ranges.add(new NodeRangeImpl(rangeStart, partitionsCount, previousNode));
        }

        return ranges.stream().filter(r -> r.node() == nodeId).map(r -> new RangeImpl(r.from(), r.to())).collect(Collectors.toList());
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
