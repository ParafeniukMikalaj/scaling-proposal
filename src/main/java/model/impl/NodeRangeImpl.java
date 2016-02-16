package model.impl;

import model.NodeRange;

public class NodeRangeImpl extends RangeImpl implements NodeRange {

    private final int node;

    public NodeRangeImpl(int from, int to, int node) {
        super(from, to);
        this.node = node;
    }

    @Override
    public int node() {
        return node;
    }

    @Override
    public int from() {
        return super.from();
    }

    @Override
    public int to() {
        return super.to();
    }

    @Override
    public String toString() {
        return super.toString() + " " + node;
    }
}
