package server;

import server.impl.ClientServerImpl;

public interface ClientServerListener {
    void onWriteSuffer(ClientServerImpl clientServer);
    void onResolveServer(ClientServerImpl clientServer, int clientId);
    void onClientDisconnect(ClientServerImpl clientServer);
}
