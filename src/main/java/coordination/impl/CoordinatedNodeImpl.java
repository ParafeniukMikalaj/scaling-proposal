package coordination.impl;

import coordination.CoordinatedNode;
import model.Node;
import model.impl.NodeImpl;

import java.util.Collection;

public class CoordinatedNodeImpl extends NodeImpl implements CoordinatedNode {

    private final Collection<Integer> splitPoints;

    public CoordinatedNodeImpl(Node node, Collection<Integer> splitPoints) {
        super(node.id(), node.host(), node.port());
        this.splitPoints = splitPoints;
    }

    @Override
    public Collection<Integer> splitPoints() {
        return splitPoints;
    }
}
