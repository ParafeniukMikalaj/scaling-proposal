package server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WriterImpl implements Writer {

    private ByteBuffer buffer;
    private SocketChannel channel;

    public WriterImpl(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void performWrite() {
        if (buffer != null) {
            try {
                channel.write(buffer);
            } catch (IOException e) {
                logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
            }
        }
    }

    @Override
    public void sendMessage(String message) {
        sendMessageToBuffer("data|" + message);
    }

    @Override
    public void sendResolutionInfo(String host, int port) {
        sendMessageToBuffer(String.format("resolve|%s:%d", host, port));
    }

    @Override
    public void sendUnknownResolutionInfo() {
        sendMessageToBuffer("resolve|unknown");
    }

    private void sendMessageToBuffer(String message) {
        byte[] messageBytes = message.getBytes();
        buffer = ByteBuffer.allocate(messageBytes.length + 4);
        buffer.putInt(messageBytes.length);
        buffer.put(messageBytes);
        performWrite();
    }

    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);
}
