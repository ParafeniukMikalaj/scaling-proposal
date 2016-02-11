package server;

import java.util.List;

public interface Server {
    List<Integer> getConnectedClients();
    void close(Integer client);
}
