package client.impl;

import client.ClientWriter;
import common.network.BasicWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientWriterImpl implements ClientWriter {

    private final BasicWriter writer;

    public ClientWriterImpl(SocketChannel channel, ByteBuffer buffer) {
        writer = new BasicWriter(channel, buffer);
    }

    @Override
    public void performWrite() {
        logger.info("Request to perform write");
        writer.performWrite();
    }

    @Override
    public void close() {
        logger.info("Request to close writer");
        writer.close();
    }

    @Override
    public void resolveServer(int clientId) {
        logger.info("Request to send resolve message");
        writer.writeMessage("resolve", String.valueOf(clientId));
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientWriterImpl.class);
}
