package server.impl;

import common.network.BasicWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ServerWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ServerWriterImpl implements ServerWriter {

    private BasicWriter writer;

    public ServerWriterImpl(SocketChannel channel) {
        writer = new BasicWriter(channel);
    }

    @Override
    public void sendMessage(String message) {
        writer.writeMessage("data", message);
    }

    @Override
    public void sendResolutionInfo(String host, int port) {
        writer.writeMessage("resolve", host + ":" + port);
    }

    @Override
    public void sendUnknownResolutionInfo() {
        writer.writeMessage("resolve", "unknown");
    }

    @Override
    public void performWrite() {
        writer.performWrite();
    }

    @Override
    public void close() {
        writer.close();
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerWriterImpl.class);
}
