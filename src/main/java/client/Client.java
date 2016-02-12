package client;

public interface Client extends ClientReaderListener {
    void resolveServer();
    void onConnect();
    void onReadReady();
    void onWriteReady();
}
