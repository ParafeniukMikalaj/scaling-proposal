package client;

public interface ClientReaderListener {
    void onMessage(String message);
    void onResolveServer(boolean success, String host, int port);
}
