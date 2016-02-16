package server.impl;

import common.network.BasicWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ServerWriter;

import java.nio.channels.SocketChannel;

public class ServerWriterImpl implements ServerWriter {
    private BasicWriter writer;

    public ServerWriterImpl(SocketChannel channel) {
        writer = new BasicWriter(channel);
    }

    @Override
    public int sendMessage(String message) {
        logger.info("Request to send {} message", message);
        return writer.writeMessage("data", message);
    }

    @Override
    public int sendResolutionInfo(String host, int port) {
        logger.info("Request to send resolution info {}:{}", host, port);
        return writer.writeMessage("resolve", host + ":" + port);
    }

    @Override
    public int sendUnknownResolutionInfo() {
        logger.info("Request to send unknown resolution info");
        return writer.writeMessage("resolve", "unknown");
    }

    @Override
    public int performWrite() {
        return writer.performWrite();
    }

    @Override
    public void close() {
        logger.info("Request to close writer");
        writer.close();
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerWriterImpl.class);
}
