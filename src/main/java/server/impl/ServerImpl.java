package server.impl;

import server.Server;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.List;

public class ServerImpl implements Server {

    public ServerImpl(String host, int port) {
//        AsynchronousServerSocketChannel
    }

    @Override
    public List<Integer> getConnectedClients() {
        return null;
    }

    @Override
    public void close(Integer client) {

    }
}
