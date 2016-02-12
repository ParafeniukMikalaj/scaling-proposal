package client.impl;

import client.Client;
import client.ClientContainer;
import common.network.Reader;
import client.ClientWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        logger.info("Client received resolution. Success: {}, host: {}, port: {}", success, host, port);
        if (!success) {
            resolveServer();
        }
        if (host.equals(this.host) || port != this.port) {
            logger.info("Client should be redirected to {}:{} while connected to {}:{}", host, port, this.host, this.port);
            reader.close();
            writer.close();
            container.requestReconnect(clientId, host, port);
        }
    }

    @Override
    public void onConnect() {
        logger.info("Client connected to {}:{}", host, port);
    }

    @Override
    public void onReadReady() {
        logger.info("Client is ready to read");
        reader.performRead();
    }

    @Override
    public void onWriteReady() {
        logger.info("Client is ready to write");
        writer.performWrite();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientImpl.class);
}
