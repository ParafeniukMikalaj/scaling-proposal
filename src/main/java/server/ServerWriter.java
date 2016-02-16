package server;

import common.network.Writer;

public interface ServerWriter extends Writer {
    int sendMessage(String message);
    int sendResolutionInfo(String host, int port);
    int sendUnknownResolutionInfo();
}
