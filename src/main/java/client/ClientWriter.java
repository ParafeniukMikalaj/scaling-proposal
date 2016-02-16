package client;

import common.network.Writer;

public interface ClientWriter extends Writer {
    int resolveServer(int clientId);
}
