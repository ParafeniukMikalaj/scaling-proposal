package server.impl;

import model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientServerImpl implements ClientServer, ReaderListener {

    private final SocketChannel channel;
    private final Reader reader;
    private final Writer writer;
    private final ClientServerListener listener;

    public ClientServerImpl(SocketChannel channel, ClientServerListener listener) {
        this.channel = channel;
        writer = new WriterImpl(channel);
        reader = new ReaderImpl(channel, this);
        this.listener = listener;
    }

    @Override
    public void sendResolutionInfo(int clientId, Node node) {
        writer.sendResolutionInfo(node.host(), node.port());
    }

    @Override
    public void sendUnknownResolutionInfo(int clientId) {
        writer.sendUnknownResolutionInfo();
    }

    @Override
    public void sendMessage(String message) {
        writer.sendMessage(message);
    }

    @Override
    public void onReadReady() {
        reader.performRead();
    }

    @Override
    public void onWriteReady() {
        writer.performWrite();
    }

    @Override
    public void disconnect() {
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Error while closing connection", e);
        }
    }

    @Override
    public void onResolveServer(int clientId) {
        listener.onResolveServer(this, clientId);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientServerImpl.class);
}
