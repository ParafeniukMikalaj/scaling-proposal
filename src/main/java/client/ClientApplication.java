package client;

import client.impl.ClientImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import common.Service;
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

public class ClientApplication implements ClientContainer, Service {

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

    @Override
    public void start() {
        logger.info("Starting client application");
        try {
            selector = Selector.open();
        } catch (IOException e) {
            logger.error("Error while opening selector", e);
        }
        executor.scheduleAtFixedRate(this::spawnClient, 0, spawnDelay, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::decommissionClient, 0, decommissionDelay, TimeUnit.MILLISECONDS);
        executor.execute(this::processIo);
    }

    @Override
    public void stop() {
        logger.info("Stopping client application");
        executor.shutdown();
    }

    private void spawnClient() {
        logger.info("Automatic request to spawn client");
        InetSocketAddress address = addresses.get(random.nextInt(addresses.size()));
        int clientId = random.nextInt(clientsCount);
        spawnClient(clientId, address.getHostName(), address.getPort());
    }

    private void spawnClient(int clientId, String host, int port) {
        logger.info("Request to spawn client {} with connection to {}:{}", clientId, host, port);
        try {
            if (clientKeys.containsKey(clientId)) {
                logger.info("Client {} is already connected", clientId);
                return;
            }
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Client client = new ClientImpl(socketChannel, clientId, host, port, this);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, client);
            socketChannel.connect(new InetSocketAddress(host, port));
            clientKeys.put(clientId, selectionKey);
        } catch (IOException e) {
            logger.error("IO while spawning client", e);
        }
    }

    private void decommissionClient() {
        logger.info("Automatic request to decommission client");
        decommissionClient(random.nextInt(clientsCount));
    }

    private void decommissionClient(int clientId) {
        logger.info("Request to decommission client {}", clientId);
        if (!clientKeys.containsKey(clientId)) {
            logger.info("Client {} is not active", clientId);
            return;
        }
        SelectionKey selectionKey = clientKeys.remove(clientId);
        Client client = (Client) selectionKey.attachment();
        client.close();
    }

    @Override
    public void requestReconnect(int clientId, String host, int port) {
        logger.info("Request from client {} on {}:{} to reconnect", clientId, host, port);
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
