package server.impl;

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
    private Map<Integer, ClientServer> clientServerMap = Maps.newHashMap();

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
            clientServerMap.get(clientId).close();
            clientServerMap.remove(clientId);
        }
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
                } else if (key.isWritable()) {
                    clientServer.onWriteReady();
                }
                keyIterator.remove();
            }
        }
    }

    @Override
    public void onResolveServer(ClientServerImpl clientServer, int clientId) {
        Node node = container.getNode(clientId);
        if (node == null) {
            clientServer.sendUnknownResolutionInfo(clientId);
        } else {
            clientServer.sendResolutionInfo(clientId, new NodeImpl(node.id(), node.host(), node.port()));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerImpl.class);

}
