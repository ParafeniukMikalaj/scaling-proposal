package client.impl;

import client.Address;

public class AddressImpl implements Address {
    private final String host;
    private final int port;

    public AddressImpl(String host, int port) {
        this.host = host;
        this.port = port;
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
