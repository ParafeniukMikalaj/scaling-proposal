package hashing;

import coordination.CoordinatedNode;

import java.util.Collection;

public interface HashRing<I, V> {
    I hash(V value);

    void remove(I node);

    Collection<Integer> add(I node);

    Collection<V> getPartitions(int partitionsCount, I node, Collection<CoordinatedNode> coordinatedNodes);

    Collection<V> filterNotOwnedValues(I node, Collection<V> ownedValues);
}
