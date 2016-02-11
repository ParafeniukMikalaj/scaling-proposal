package server;

import server.impl.ClientServerImpl;

public interface ClientServerListener {
    void onResolveServer(ClientServerImpl clientServer, int clientId);
}
