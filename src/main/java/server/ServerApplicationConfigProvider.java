package server;

public class ServerApplicationConfigProvider {

    private final String host;
    private final int port;
    private final int nodeId;

    public ServerApplicationConfigProvider(String host, int port, int nodeId) {
        this.host = host;
        this.port = port;
        this.nodeId = nodeId;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int nodeId() {
        return nodeId;
    }
}
