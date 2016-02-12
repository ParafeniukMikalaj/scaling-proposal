package client;

import client.impl.ClientImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientApplication implements ClientContainer {

    private final int clientsCount;
    private List<InetSocketAddress> addresses;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);

    private final Random random = new Random(137);
    private Map<Integer, SelectionKey> clientKeys = Maps.newHashMap();

    private Selector selector;
    private final long spawnDelay;
    private final long decommissionDelay;

    public ClientApplication(int clientsCount, int spawnDelay, int decommissionDelay, Collection<InetSocketAddress> addresses) {
        this.clientsCount = clientsCount;
        this.addresses = Lists.newArrayList(addresses);
        this.spawnDelay = spawnDelay;
        this.decommissionDelay = decommissionDelay;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::spawnClient, 0, spawnDelay, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::decommissionClient, 0, decommissionDelay, TimeUnit.MILLISECONDS);
        executor.execute(this::processIo );
    }

    public void stop() {
        executor.shutdown();
    }

    private void spawnClient() {
        InetSocketAddress address = addresses.get(random.nextInt(addresses.size()));
        int clientId = random.nextInt(clientsCount);
        spawnClient(clientId, address.getHostName(), address.getPort());
    }

    private void spawnClient(int clientId, String host, int port) {
        try {
            if (clientKeys.containsKey(clientId)) {
                return;
            }
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Client client = new ClientImpl(socketChannel, clientId, host, port, this);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, client);
            socketChannel.connect(new InetSocketAddress(host, port));
            clientKeys.put(clientId, selectionKey);
        } catch (IOException e) {
            logger.error("IO exception", e);
        }
    }

    private void decommissionClient() {
        decommissionClient(random.nextInt(clientsCount));
    }

    private void decommissionClient(int clientId) {
        if (!clientKeys.containsKey(clientId)) {
            return;
        }
        SelectionKey selectionKey = clientKeys.remove(clientId);
        try {
            selectionKey.channel().close();
        } catch (IOException e) {
            logger.error("IO exception while decommissioning client", e);
        }
    }

    @Override
    public void requestReconnect(int clientId, String host, int port) {
        decommissionClient(clientId);
        spawnClient(clientId, host, port);
    }

    private void processIo() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    Client client = (Client) key.attachment();

                    if (key.isConnectable()) {
                        client.onConnect();
                    } else if (key.isReadable()) {
                        client.onReadReady();
                    } else if (key.isWritable()) {
                        client.onWriteReady();
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);
}
