package client;

import client.impl.ClientImpl;

public interface ClientContainer {
    void onWriteSuffer(ClientImpl client);
    void onConnectinEstablished(int clientId);
    void requestReconnect(int clientId);
    void requestReconnect(int clientId, String host, int port);
}
