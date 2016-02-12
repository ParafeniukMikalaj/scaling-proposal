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
    public void performWrite() {
        writer.performWrite();
    }

    @Override
    public void close() {
        writer.close();
    }

    @Override
    public void resolveServer(int clientId) {
        writer.writeMessage("resolve", String.valueOf(clientId));
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientWriterImpl.class);
}
