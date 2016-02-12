package server.impl;

import common.network.Reader;
import model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientServerImpl implements ClientServer, ServerReaderListener {

    private final SocketChannel channel;
    private final Reader reader;
    private final ServerWriter writer;
    private final ClientServerListener listener;

    public ClientServerImpl(SocketChannel channel, ClientServerListener listener) {
        logger.info("Create server to client connection");
        this.channel = channel;
        writer = new ServerWriterImpl(channel);
        reader = new ServerReaderImpl(channel, this);
        this.listener = listener;
    }

    @Override
    public void sendResolutionInfo(int clientId, Node node) {
        logger.info("Request to send {} to {} as resolution info", node, clientId);
        writer.sendResolutionInfo(node.host(), node.port());
    }

    @Override
    public void sendUnknownResolutionInfo(int clientId) {
        logger.info("Request to send {} to {} as resolution info", "null", clientId);
        writer.sendUnknownResolutionInfo();
    }

    @Override
    public void sendMessage(String message) {
        logger.info("Request to send {} message to client", message);
        writer.sendMessage(message);
    }

    @Override
    public void onReadReady() {
        logger.info("Read ready");
        reader.performRead();
    }

    @Override
    public void onWriteReady() {
        logger.info("Write ready");
        writer.performWrite();
    }

    @Override
    public void close() {
        logger.info("Request to close client server connection");
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Error while closing connection", e);
        }
    }

    @Override
    public void onResolveServer(int clientId) {
        logger.info("Client {} requests resolution info", clientId);
        listener.onResolveServer(this, clientId);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientServerImpl.class);
}
