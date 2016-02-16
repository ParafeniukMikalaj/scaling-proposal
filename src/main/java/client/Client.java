package client;

public interface Client extends ClientReaderListener {
    void resolveServer();
    void onConnect();
    void onConnectionFail();
    void onReadReady();
    int onWriteReady();
    void close();
}
