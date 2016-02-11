package server.impl;

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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerImpl implements Server, ClientServerListener {

    private Selector serverSelector;
    private Selector clientSelector;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private ServerContainer container;

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
    public List<Integer> getConnectedClients() {
        return null;
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
        // TODO extract coordination info
        clientServer.sendResolutionInfo(clientId, new NodeImpl(0, "localhost", 0));
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerImpl.class);
}
