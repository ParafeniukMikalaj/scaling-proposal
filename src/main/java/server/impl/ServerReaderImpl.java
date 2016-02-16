package server.impl;

import common.network.AbstractReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ServerReaderListener;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ServerReaderImpl extends AbstractReader {

    private final ServerReaderListener listener;

    public ServerReaderImpl(SocketChannel channel, ByteBuffer buffer, ServerReaderListener listener) {
        super(channel, buffer);
        this.listener = listener;
    }

    @Override
    protected void handleMessage(String type, String message) {
        switch (type) {
            case "resolve":
                handleResolveMessage(message);
                break;
            default:
                logger.error("Unknown message type {}", type);
        }
    }

    private void handleResolveMessage(String clientId) {
        logger.info("Received resolve request from client {}", clientId);
        listener.onResolveServer(Integer.parseInt(clientId));
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerReaderImpl.class);
}
