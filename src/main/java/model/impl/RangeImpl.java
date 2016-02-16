package model.impl;

import model.Range;

public class RangeImpl implements Range {
    private final int from;
    private final int to;

    public RangeImpl(int from, int to) {
        this.to = to;
        this.from = from;
    }

    @Override
    public int from() {
        return from;
    }

    @Override
    public int to() {
        return to;
    }

    @Override
    public String toString() {
        return "[" + from + ", " + to + ")";
    }
}
