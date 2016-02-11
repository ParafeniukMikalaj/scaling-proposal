package server.impl;

import model.Node;
import model.impl.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ClientServer;
import server.ClientServerListener;
import server.Server;
import server.ServerContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerImpl implements Server, ClientServerListener {

    private Selector serverSelector;
    private Selector clientSelector;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private ServerContainer container;
    private Map<Integer, ClientServer> clientServerMap;

    public ServerImpl(int port, ServerContainer container) {
        try {
            this.container = container;
            serverSelector = Selector.open();
            clientSelector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
            serverSocketChannel.bind(new InetSocketAddress(port));
            executor.execute(this::processConnections);
            executor.execute(this::processIo);
        } catch (IOException e) {
            logger.error("IO error while creating server socket", e);
        }
    }

    @Override
    public Collection<Integer> connectedClients() {
        return clientServerMap.keySet();
    }

    @Override
    public boolean containsClient(int clientId) {
        return clientServerMap.containsKey(clientId);
    }

    @Override
    public void sendMessage(int clientId, String message) {
        if (clientServerMap.containsKey(clientId)) {
            clientServerMap.get(clientId).sendMessage(message);
        }
    }

    @Override
    public void disconnectClient(int clientId) {
        if (clientServerMap.containsKey(clientId)) {
            clientServerMap.get(clientId).disconnect();
            clientServerMap.remove(clientId);
        }
    }

    private void processConnections() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                serverSelector.select();
                Set<SelectionKey> selectedKeys = serverSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = channel.accept();
                        ClientServer clientServer = new ClientServerImpl(clientChannel, this);
                        clientChannel.register(clientSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, clientServer);
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
        }
    }

    private void processIo() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                clientSelector.select();
                Set<SelectionKey> selectedKeys = clientSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    ClientServer clientServer = (ClientServer) key.attachment();
                    if (key.isWritable()) {
                        clientServer.onWriteReady();
                    } else if (key.isReadable()) {
                        clientServer.onReadReady();
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
        }
    }

    @Override
    public void onResolveServer(ClientServerImpl clientServer, int clientId) {
        Node node = container.getNode(clientId);
        if (node == null) {
            clientServer.sendUnknownResolutionInfo(clientId);
        } else {
            clientServer.sendResolutionInfo(clientId, node);
        }
        clientServer.sendResolutionInfo(clientId, new NodeImpl(0, "localhost", 0));
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerImpl.class);


}
