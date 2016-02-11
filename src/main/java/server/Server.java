package server;

import java.util.Collection;

public interface Server {
    Collection<Integer> connectedClients();
    boolean containsClient(int clientId);
    void sendMessage(int clientId, String message);
    void disconnectClient(int clientId);
}
