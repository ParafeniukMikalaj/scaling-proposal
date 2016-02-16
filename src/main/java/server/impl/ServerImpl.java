package server.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ServerImpl implements Server, ClientServerListener {

    private Selector serverSelector;
    private Selector clientSelector;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private ServerContainer container;
    private BiMap<Integer, ClientServer> clientServerMap = HashBiMap.create();
    private Map<ClientServer, SelectionKey> clientKeyMap = Maps.newHashMap();
    private final String host;
    private final int port;

    public ServerImpl(String host, int port, ServerContainer container) {
        this.host = host;
        this.port = port;
        this.container = container;
        try {
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
        logger.info("disonnecting client {}", clientId);
        ClientServer clientServer = clientServerMap.remove(clientId);
        if (clientServer == null) {
            return;
        }
        clientServer.close();

        SelectionKey selectionKey = clientKeyMap.remove(clientServer);
        if (selectionKey != null) {
            selectionKey.cancel();
        }
        clientServerMap.remove(clientId);
    }

    private void processConnections() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                serverSelector.select();
                Set<SelectionKey> selectedKeys = serverSelector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = channel.accept();
                        clientChannel.configureBlocking(false);
                        ClientServer clientServer = new ClientServerImpl(clientChannel, this);
                        clientSelector.wakeup();
                        SelectionKey selectionKey = clientChannel.register(clientSelector, SelectionKey.OP_READ, clientServer);
                        clientKeyMap.put(clientServer, selectionKey);
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
        }
    }

    private void processIo() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int selectCount = clientSelector.select();
                if (selectCount == 0) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
            Set<SelectionKey> selectedKeys = clientSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                ClientServer clientServer = (ClientServer) key.attachment();
                if (key.isReadable()) {
                    clientServer.onReadReady();
                }
                if (key.isWritable()) {
                    int writeBytes = clientServer.onWriteReady();
                    if (writeBytes != 0) {
                        logger.info("Removing OP_WRITE flag from selection");
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
                keyIterator.remove();
            }
        }
    }

    @Override
    public void onWriteSuffer(ClientServerImpl clientServer) {
        logger.info("Server writes suffer");
        SelectionKey key = clientKeyMap.get(clientServer);
        if (key != null) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void onResolveServer(ClientServerImpl clientServer, int clientId) {
        Node node = container.getNode(clientId);
        if (node == null) {
            clientServer.sendUnknownResolutionInfo(clientId);
        } else {
            if (this.port == node.port() && this.host.equals(node.host())) {
                clientServerMap.put(clientId, clientServer);
                logger.info("Client {} is now connected", clientId);
            }
            clientServer.sendResolutionInfo(clientId, new NodeImpl(node.id(), node.host(), node.port()));
        }
    }

    @Override
    public void onClientDisconnect(ClientServerImpl clientServer) {
        if (clientServer == null) {
            return;
        }
        Integer clientId = clientServerMap.inverse().get(clientServer);
        if (clientId == null) {
            return;
        }
        logger.info("client {} refused connection", clientId);
        disconnectClient(clientId);
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerImpl.class);

}
