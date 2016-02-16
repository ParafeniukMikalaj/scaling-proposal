package common.network;

import client.impl.ClientWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;

public class BasicWriter implements Writer {

    private final ByteBuffer buffer;
    private final SocketChannel channel;
    private int bytesToSend = 0;

    private final Object lock = new Object();

    public BasicWriter(SocketChannel channel, ByteBuffer buffer) {
        this.channel = channel;
        this.buffer = buffer;
    }

    @Override
    public void performWrite() {
        try {
            synchronized (lock) {
                while (buffer.hasRemaining() && bytesToSend > 0) {
                    bytesToSend -= channel.write(buffer);
                }
                buffer.compact();
            }
        } catch (IOException e) {
            logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
        }
    }

    @Override
    public void close() {
        logger.info("Request to close channel");
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("IO exception while closing channel", e);
        }
    }

    public void writeMessage(String type, String message) {
        String messageToSend = type + "|" + message;
        byte[] messageBytes = messageToSend.getBytes();
        synchronized (lock) {
            buffer.clear();
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);
            buffer.flip();
            bytesToSend += buffer.remaining();
        }
        performWrite();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientWriterImpl.class);
}
