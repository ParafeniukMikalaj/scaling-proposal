package server.impl;

import common.network.Reader;
import model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ClientServer;
import server.ClientServerListener;
import server.ServerReaderListener;
import server.ServerWriter;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientServerImpl implements ClientServer, ServerReaderListener {

    private final Reader reader;
    private final ServerWriter writer;
    private final ClientServerListener listener;

    public ClientServerImpl(SocketChannel channel, ClientServerListener listener) {
        logger.info("Create server to client connection");
        writer = new ServerWriterImpl(channel);
        reader = new ServerReaderImpl(channel, this);
        this.listener = listener;
    }

    @Override
    public void sendResolutionInfo(int clientId, Node node) {
        logger.info("Request to send {} to {} as resolution info", node, clientId);
        handleWriteResult(writer.sendResolutionInfo(node.host(), node.port()));
    }

    @Override
    public void sendUnknownResolutionInfo(int clientId) {
        logger.info("Request to send {} to {} as resolution info", "null", clientId);
        handleWriteResult(writer.sendUnknownResolutionInfo());
    }

    @Override
    public void sendMessage(String message) {
        logger.info("Request to send {} message to client", message);
        handleWriteResult(writer.sendMessage(message));
    }

    @Override
    public void doRead() {
        int bytesRead = reader.performRead();
        if (bytesRead == -1) {
            listener.onClientDisconnect(this);
        }
    }

    @Override
    public int doWrite() {
        return handleWriteResult(writer.performWrite());
    }

    private int handleWriteResult(int writeBytes) {
        if (writeBytes == 0) {
            listener.onWriteSuffer(this);
        }
        return writeBytes;
    }

    @Override
    public void close() {
        logger.info("Request to close client server connection");
        reader.close();
        writer.close();
    }

    @Override
    public void onResolveServer(int clientId) {
        logger.info("Client {} requests resolution info", clientId);
        listener.onResolveServer(this, clientId);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientServerImpl.class);
}
