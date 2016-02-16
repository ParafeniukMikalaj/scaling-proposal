package hashing;

import com.google.common.collect.Multimap;
import model.Range;

import java.util.Collection;

public interface HashRing<I, V> {
    I hash(V value, Multimap<I, Integer> coordinatorState);

    Collection<Integer> generateSplitPoints(I node);

    Collection<Range> getPartitions(I nodeId, Multimap<I, Integer> coordinatorState);
}
