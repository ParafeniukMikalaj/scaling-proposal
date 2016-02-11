package server;

public interface Server {
    boolean containsClient(int clientId);
    void sendMessage(int clientId, String message);
}
