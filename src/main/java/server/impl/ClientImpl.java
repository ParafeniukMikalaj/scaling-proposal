package server.impl;

import client.Client;
import client.ClientContainer;
import client.Reader;
import client.Writer;
import client.impl.ReaderImpl;
import client.impl.WriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientImpl implements Client {
    private final int clientId;
    private final Reader reader;
    private final Writer writer;
    private final ClientContainer container;
    private SocketChannel channel;
    private final String host;
    private final int port;

    public ClientImpl(SocketChannel channel, int clientId, String host, int port, ClientContainer container) {
        this.clientId = clientId;
        this.reader = new ReaderImpl(this);
        this.writer = new WriterImpl(channel);
        this.channel = channel;
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
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("exception while closing socket connection");
            }
            container.requestReconnect(clientId, host, port);
        }
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onReadReady() {
        reader.performRead(channel);
    }

    @Override
    public void onWriteReady() {
        writer.performWrite(channel);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientImpl.class);
}
