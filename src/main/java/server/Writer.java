package server;

public interface Writer {
    void performWrite();
    void sendMessage(String message);
    void sendResolutionInfo(String host, int port);
    void sendUnknownResolutionInfo();
}
