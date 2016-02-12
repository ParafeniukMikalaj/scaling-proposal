package server;

import model.Node;

public interface ClientServer {
    void sendResolutionInfo(int clientId, Node node);
    void sendUnknownResolutionInfo(int clientId);
    void sendMessage(String message);
    void onReadReady();
    void onWriteReady();
    void close();
}
