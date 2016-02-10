package hashing;

import java.util.Collection;

public interface HashRing<N, V> {
    N hash(V value);

    void remove(N node);

    void add(N node);

    Collection<V> filterNotOwnedValues(N node, Collection<V> ownedValues);
}
