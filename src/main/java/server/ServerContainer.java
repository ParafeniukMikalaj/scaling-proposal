package server;

import model.Node;

public interface ServerContainer {
    Node getNode(int clientId);
}
