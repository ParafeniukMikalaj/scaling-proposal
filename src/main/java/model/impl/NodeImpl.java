package model.impl;

import model.Node;

public class NodeImpl implements Node {

    private final int id;
    private final String host;
    private final int port;

    public NodeImpl(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }
}
