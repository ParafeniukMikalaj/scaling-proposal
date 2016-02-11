package client.impl;

import client.Reader;
import client.ReaderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ReaderImpl implements Reader {

    private ByteBuffer buffer = ByteBuffer.allocate(100);
    private final ReaderListener listener;

    public ReaderImpl(ReaderListener listener) {
        this.listener = listener;
    }

    @Override
    public void performRead(SocketChannel channel) {
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
            case "data":
                handleDataMesasge(parts[1]);
                break;
            default:
                logger.error("Unknown message type {}", type);

        }
    }

    private void handleResolveMessage(String resolveMessage) {
        if (resolveMessage.equals("unknown")) {
            listener.onResolveServer(false, null, 0);
        } else {
            String[] parts = resolveMessage.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            listener.onResolveServer(true, host, port);
        }
    }

    private void handleDataMesasge(String dataMessage) {
        listener.onMessage(dataMessage);
    }

    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);
}
