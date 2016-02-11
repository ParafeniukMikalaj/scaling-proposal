package client;

public interface Client extends ReaderListener {
    void resolveServer();
    void onConnect();
    void onReadReady();
    void onWriteReady();
}
