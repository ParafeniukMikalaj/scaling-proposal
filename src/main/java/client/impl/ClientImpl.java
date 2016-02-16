package client.impl;

import client.Client;
import client.ClientContainer;
import client.ClientReaderListener;
import client.ClientWriter;
import common.network.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

public class ClientImpl implements Client, ClientReaderListener {
    private final int clientId;
    private final Reader reader;
    private final ClientWriter writer;
    private final ClientContainer container;
    private final String host;
    private final int port;

    public ClientImpl(SocketChannel channel, int clientId, String host, int port, ClientContainer container) {
        logger.info("Starting client to listen to {}:{}", host, port);
        this.clientId = clientId;
        this.reader = new ClientReaderImpl(channel, this);
        this.writer = new ClientWriterImpl(channel);
        this.host = host;
        this.port = port;
        this.container = container;
    }

    @Override
    public void resolveServer() {
        logger.info("Client {} on {}:{} requests resolution", clientId, host, port);
        handleWriteResult(writer.resolveServer(clientId));
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
    public void doRead() {
        int bytesRead = reader.performRead();
        if (bytesRead == -1) {
            logger.info("Server closed connection of client {}", clientId);
            container.requestReconnect(clientId);
        }
    }

    @Override
    public int doWrite() {
        return handleWriteResult(writer.performWrite());
    }

    private int handleWriteResult(int writeBytes) {
        if (writeBytes == 0) {
            container.onWriteSuffer(this);
        }
        return writeBytes;
    }

    @Override
    public void close() {
        logger.info("Request to stop client");
        writer.close();
        reader.close();
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
            return;
        }
        if (!host.equals(this.host) || port != this.port) {
            logger.info("Client should be redirected to {}:{} while connected to {}:{}. Request reconnect.",
                    host, port, this.host, this.port);
            reader.close();
            writer.close();
            container.requestReconnect(clientId, host, port);
            return;
        }
        container.onConnectionEstablished(clientId);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientImpl.class);
}
