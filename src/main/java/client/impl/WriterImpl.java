package client.impl;

import client.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void performWrite(SocketChannel channel) {
        if (buffer != null) {
            try {
                channel.write(buffer);
            } catch (IOException e) {
                logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
            }
        }
    }

    @Override
    public void resolveServer(int clientId) {
        String message = "resolve|" + clientId;
        byte[] messageBytes = message.getBytes();
        buffer = ByteBuffer.allocate(messageBytes.length + 4);
        buffer.putInt(messageBytes.length);
        buffer.put(messageBytes);
        performWrite(channel);
    }

    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);
}
