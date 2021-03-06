package common.network;

import client.impl.ClientWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BasicWriter implements Writer {

    private final ByteBuffer buffer = ByteBuffer.allocate(100);
    private final SocketChannel channel;
    private int bytesToSend = 0;

    private final Object lock = new Object();

    public BasicWriter(SocketChannel channel) {
        this.channel = channel;
        buffer.clear();
    }

    @Override
    public int performWrite() {
        synchronized (lock) {
            int bytesWrite = -1;
            try {
                while (buffer.hasRemaining() && bytesToSend > 0) {
                    bytesWrite = channel.write(buffer);
                    bytesToSend -= bytesWrite;
                    if (bytesWrite == 0) {
                        break;
                    }
                }
                buffer.compact();
            } catch (IOException e) {
                logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
            }
            return bytesWrite;
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

    public int writeMessage(String type, String message) {
        String messageToSend = type + "|" + message;
        byte[] messageBytes = messageToSend.getBytes();
        synchronized (lock) {
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);
            buffer.flip();
            bytesToSend += messageBytes.length + 4;
        }
        return performWrite();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientWriterImpl.class);
}
