package coordination;

import model.Node;

import java.util.Collection;
import java.util.List;

public interface CoordinatedNode extends Node {
    Collection<Integer> splitPoints();
}
