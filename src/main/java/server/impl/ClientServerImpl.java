package server.impl;

import coordination.CoordinatedNode;
import model.Node;
import server.*;

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
    public void onResolveServer(int clientId) {
        listener.onResolveServer(this, clientId);
    }
}
