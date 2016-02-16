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
import java.util.concurrent.locks.LockSupport;

public class ClientApplication implements ClientContainer, Service {

    private final int clientsCount;
    private List<InetSocketAddress> addresses;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);

    private final Random random = new Random(137);
    private Map<Integer, SelectionKey> clientKeys = Maps.newHashMap();
    private Map<Integer, Integer> clientReconnectCount = Maps.newHashMap();

    private Selector selector;
    private final long spawnDelay;
    private final long decommissionDelay;

    public ClientApplication(int clientsCount, int spawnDelay, int decommissionDelay,
                             Collection<InetSocketAddress> addresses) {
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
        executor.execute(this::processIo);
        executor.scheduleAtFixedRate(this::spawnClient, 0, spawnDelay, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::decommissionClient, 0, decommissionDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        logger.info("Stopping client application");
        executor.shutdown();
    }

    private void spawnClient() {
        logger.info("Automatic request to spawn client");
        int clientId = random.nextInt(clientsCount);
        spawnClient(clientId);
    }

    private void spawnClient(int clientId) {
        logger.info("Spawning client {}", clientId);
        InetSocketAddress address = addresses.get(random.nextInt(addresses.size()));
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
            selector.wakeup();
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, client);
            socketChannel.connect(new InetSocketAddress(host, port));
            clientKeys.put(clientId, selectionKey);
            clientReconnectCount.put(clientId, 0);
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
        logger.info("Killing client client {}", clientId);
        if (!clientKeys.containsKey(clientId)) {
            logger.info("Client {} is not active", clientId);
            return;
        }
        clientReconnectCount.remove(clientId);
        SelectionKey selectionKey = clientKeys.remove(clientId);
        Client client = (Client) selectionKey.attachment();
        client.close();
    }

    @Override
    public void onWriteSuffer(ClientImpl client) {
        logger.info("Client writes suffer");
        Optional<SelectionKey> selectionKeyOption = selector.keys().stream().filter(k -> k.attachment().equals(client)).findFirst();
        if (selectionKeyOption.isPresent()) {
            SelectionKey key = selectionKeyOption.get();
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void onConnectinEstablished(int clientId) {
        clientReconnectCount.put(clientId, 0);
    }

    @Override
    public void requestReconnect(int clientId) {
        int reconnectCount = clientReconnectCount.get(clientId);
        logger.info("Request #{} from client {} to reconnect", reconnectCount, clientId);
        decommissionClient(clientId);
        if (reconnectCount < 3) {
            spawnClient(clientId);
            clientReconnectCount.put(clientId, reconnectCount + 1);
        } else {
            logger.warn("Client {} exceeded number of reconnects.", clientId);
        }
    }

    @Override
    public void requestReconnect(int clientId, String host, int port) {
        logger.info("Request from client {} to reconnect to {}:{}", clientId, host, port);
        decommissionClient(clientId);
        spawnClient(clientId, host, port);
    }

    private void processIo() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int selectCount = selector.select();
                if (selectCount == 0) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
            } catch (IOException e) {
                logger.error("Error while performing select", e);
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                Client client = (Client) key.attachment();
                SocketChannel channel = (SocketChannel) key.channel();

                if (key.isConnectable()) {
                    try {
                        channel.finishConnect();
                        client.onConnect();
                    } catch (IOException e) {
                        logger.error("Connection error. Client will retry.", e);
                        client.onConnectionFail();
                        continue;
                    }
                }
                if (key.isReadable()) {
                    client.onReadReady();
                }
                if (key.isWritable()) {
                    int writeBytes = client.onWriteReady();
                    if (writeBytes != 0) {
                        logger.info("Removing OP_WRITE flag from selection");
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
                keyIterator.remove();
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);
}
