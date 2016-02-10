package coordination.impl;

public class ZkConnectionImpl implements ZkConnection {

    private final String host;
    private final int port;

    public ZkConnectionImpl(String host, int port) {
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
