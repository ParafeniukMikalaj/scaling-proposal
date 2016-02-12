package client.impl;

import common.network.AbstractReader;
import client.ClientReaderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

public class ClientReaderImpl extends AbstractReader {

    private final ClientReaderListener listener;

    public ClientReaderImpl(SocketChannel channel, ClientReaderListener listener) {
        super(channel);
        this.listener = listener;
    }

    @Override
    protected void handleMessage(String type, String message) {
        switch (type) {
            case "resolve":
                handleResolveMessage(message);
                break;
            case "data":
                handleDataMesasge(message);
                break;
            default:
                logger.error("Unknown message type {}", type);
        }
    }

    private void handleResolveMessage(String resolveMessage) {
        logger.info("Received resolve message {}", resolveMessage);
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
        logger.info("Received data message {}", dataMessage);
        listener.onMessage(dataMessage);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientReaderImpl.class);
}
