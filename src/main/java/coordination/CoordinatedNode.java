package coordination;

import model.Node;

import java.util.List;

public interface CoordinatedNode extends Node {
    List<Integer> splitPoints();
}
