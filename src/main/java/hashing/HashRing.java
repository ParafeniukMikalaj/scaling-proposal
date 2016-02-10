package hashing;

import java.util.Collection;

public interface HashRing<I, V> {
    I hash(V value);

    void remove(I node);

    Collection<Integer> add(I node);

    Collection<V> filterNotOwnedValues(I node, Collection<V> ownedValues);
}
