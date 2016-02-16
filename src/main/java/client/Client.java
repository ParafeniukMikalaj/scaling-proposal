package client;

public interface Client extends ClientReaderListener {
    void resolveServer();
    void onConnect();
    void onConnectionFail();
    void doRead();
    int doWrite();
    void close();
}
