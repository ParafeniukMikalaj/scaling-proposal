package client.impl;

import client.Client;
import client.ClientContainer;
import common.network.Reader;
import client.ClientWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientImpl implements Client {
    private final int clientId;
    private final Reader reader;
    private final ClientWriter writer;
    private final ClientContainer container;
    private final String host;
    private final int port;
    private final ByteBuffer buffer = ByteBuffer.allocate(1000);

    public ClientImpl(SocketChannel channel, int clientId, String host, int port, ClientContainer container) {
        logger.info("Starting client to listen to {}:{}", host, port);
        this.clientId = clientId;
        this.reader = new ClientReaderImpl(channel, buffer, this);
        this.writer = new ClientWriterImpl(channel, buffer);
        this.host = host;
        this.port = port;
        this.container = container;
    }

    @Override
    public void resolveServer() {
        logger.info("Client {} on {}:{} requests resolution", clientId, host, port);
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
            logger.info("Resolution request retry");
            resolveServer();
        }
        if (!host.equals(this.host) || port != this.port) {
            logger.info("Client should be redirected to {}:{} while connected to {}:{}. Request reconnect.",
                    host, port, this.host, this.port);
            reader.close();
            writer.close();
            container.requestReconnect(clientId, host, port);
        }
    }

    @Override
    public void onConnect() {
        logger.info("Client connected to {}:{}", host, port);
        resolveServer();
    }

    @Override
    public void onConnectionFail() {
        logger.info("Client failed to connect to {}:{}", host, port);
        container.requestReconnect(clientId);
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
    public void close() {
        logger.info("Request to stop client");
        writer.close();
        reader.close();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientImpl.class);
}
