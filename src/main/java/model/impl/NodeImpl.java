package model.impl;

import model.Node;

public class NodeImpl implements Node {

    private int id;
    private String host;
    private int port;

    public NodeImpl() {

    }

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

    @Override
    public String toString() {
        return id + " " + host + ":" + port;
    }
}
