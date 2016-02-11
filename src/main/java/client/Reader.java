package client;

import java.nio.channels.SocketChannel;

public interface Reader {
    void performRead(SocketChannel channel);
}
