package client;

public interface ClientContainer {
    void onConnectinEstablished(int clientId);
    void requestReconnect(int clientId);
    void requestReconnect(int clientId, String host, int port);
}
