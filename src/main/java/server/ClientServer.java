package server;

import coordination.CoordinatedNode;
import model.Node;

public interface ClientServer {
    void sendResolutionInfo(int clientId, Node node);
    void sendMessage(String message);
    void onReadReady();
    void onWriteReady();
}
