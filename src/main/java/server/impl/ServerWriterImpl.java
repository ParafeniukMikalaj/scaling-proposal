package server.impl;

import common.network.BasicWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ServerWriter;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ServerWriterImpl implements ServerWriter {
    private BasicWriter writer;

    public ServerWriterImpl(SocketChannel channel, ByteBuffer buffer) {
        writer = new BasicWriter(channel, buffer);
    }

    @Override
    public void sendMessage(String message) {
        logger.info("Request to send {} message", message);
        writer.writeMessage("data", message);
    }

    @Override
    public void sendResolutionInfo(String host, int port) {
        logger.info("Request to send resolution info {}:{}", host, port);
        writer.writeMessage("resolve", host + ":" + port);
    }

    @Override
    public void sendUnknownResolutionInfo() {
        logger.info("Request to send unknown resolution info");
        writer.writeMessage("resolve", "unknown");
    }

    @Override
    public void performWrite() {
        logger.info("Perform write");
        writer.performWrite();
    }

    @Override
    public void close() {
        logger.info("Request to close writer");
        writer.close();
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerWriterImpl.class);
}
