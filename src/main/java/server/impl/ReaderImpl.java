package server.impl;

import client.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ReaderListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ReaderImpl implements server.Reader {

    private final SocketChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(100);
    private final ReaderListener listener;

    public ReaderImpl(SocketChannel channel, ReaderListener listener) {
        this.listener = listener;
        this.channel = channel;
    }

    @Override
    public void performRead() {
        try {
            channel.write(buffer);
        } catch (IOException e) {
            logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
        }
        if (buffer.position() > 4) {
            int messageLen = buffer.getInt();
            if (buffer.position() > messageLen + 4) {
                String message = new String(buffer.array(), 4, messageLen);
                buffer.position(messageLen + 4);
                buffer.compact();
                handleMessage(message);
            }
        }

    }

    private void handleMessage(String message) {
        String[] parts = message.split("|");
        String type = parts[0];
        switch (type) {
            case "resolve":
                handleResolveMessage(parts[1]);
                break;
            default:
                logger.error("Unknown message type {}", type);

        }
    }

    private void handleResolveMessage(String clientId) {
        listener.onResolveServer(Integer.parseInt(clientId));
    }

    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);
}
