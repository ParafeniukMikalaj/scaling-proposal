package client;

import java.nio.channels.SocketChannel;

public interface Writer {
    void performWrite(SocketChannel channel);
    void resolveServer(int clientId);
}
