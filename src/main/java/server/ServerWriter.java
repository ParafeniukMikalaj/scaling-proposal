package server;

import common.network.Writer;

public interface ServerWriter extends Writer {
    void sendMessage(String message);
    void sendResolutionInfo(String host, int port);
    void sendUnknownResolutionInfo();
}
