package client;

import common.network.Writer;

public interface ClientWriter extends Writer {
    void resolveServer(int clientId);
}
