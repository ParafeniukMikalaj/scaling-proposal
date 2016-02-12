package server.impl;

import client.Client;
import client.ClientContainer;
import common.network.Reader;
import client.ClientWriter;
import client.impl.ClientReaderImpl;
import client.impl.ClientWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientImpl implements Client {
    private final int clientId;
    private final Reader reader;
    private final ClientWriter writer;
    private final ClientContainer container;
    private final String host;
    private final int port;

    public ClientImpl(SocketChannel channel, int clientId, String host, int port, ClientContainer container) {
        this.clientId = clientId;
        this.reader = new ClientReaderImpl(channel, this);
        this.writer = new ClientWriterImpl(channel);
        this.host = host;
        this.port = port;
        this.container = container;
    }

    @Override
    public void resolveServer() {
        writer.resolveServer(clientId);
    }

    @Override
    public void onMessage(String message) {
        logger.info("Client {} received message {}", clientId, message);
    }

    @Override
    public void onResolveServer(boolean success, String host, int port) {
        if (!success) {
            resolveServer();
        }
        if (host.equals(this.host) || port != this.port) {
            reader.close();
            writer.close();
            container.requestReconnect(clientId, host, port);
        }
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onReadReady() {
        reader.performRead();
    }

    @Override
    public void onWriteReady() {
        writer.performWrite();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientImpl.class);
}
