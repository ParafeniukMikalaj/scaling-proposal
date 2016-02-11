package client;

public interface ReaderListener {
    void onMessage(String message);
    void onResolveServer(boolean success, String host, int port);
}
