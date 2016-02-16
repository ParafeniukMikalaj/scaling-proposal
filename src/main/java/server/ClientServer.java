package server;

import model.Node;

public interface ClientServer {
    void sendResolutionInfo(int clientId, Node node);
    void sendUnknownResolutionInfo(int clientId);
    void sendMessage(String message);
    void doRead();
    int doWrite();
    void close();
}
