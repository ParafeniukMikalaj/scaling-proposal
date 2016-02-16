package client.impl;

import client.ClientWriter;
import common.network.BasicWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

public class ClientWriterImpl implements ClientWriter {

    private final BasicWriter writer;

    public ClientWriterImpl(SocketChannel channel) {
        writer = new BasicWriter(channel);
    }

    @Override
    public int performWrite() {
        logger.info("Request to perform write");
        return writer.performWrite();
    }

    @Override
    public void close() {
        logger.info("Request to close writer");
        writer.close();
    }

    @Override
    public int resolveServer(int clientId) {
        logger.info("Request to send resolve message");
        return writer.writeMessage("resolve", String.valueOf(clientId));
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientWriterImpl.class);
}
