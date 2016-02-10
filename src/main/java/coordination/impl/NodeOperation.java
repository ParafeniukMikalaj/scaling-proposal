package coordination.impl;

import coordination.CoordinatedNode;

class NodeOperation {
    enum Type {
        JOIN, LEAVE
    }

    private final CoordinatedNode node;
    private final Type type;

    public NodeOperation(CoordinatedNode node, Type type) {
        this.node = node;
        this.type = type;
    }

    public CoordinatedNode node() {
        return node;
    }

    public Type type() {
        return type;
    }
}
