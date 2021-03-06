package common.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class AbstractReader implements Reader {

    private final SocketChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(100);

    protected AbstractReader(SocketChannel channel) {
        this.channel = channel;
        buffer.clear();
    }

    @Override
    public int performRead() {
        int bytesRead = 0;
        try {
            bytesRead = channel.read(buffer);
        } catch (IOException e) {
            logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
        }
        if (buffer.position() > 4) {
            buffer.mark();
            int messageLen = buffer.getInt(0);
            if (buffer.position() >= messageLen + 4) {
                buffer.flip();
                buffer.getInt();
                String message = new String(buffer.array(), buffer.position(), messageLen);
                buffer.position(buffer.position() + messageLen);
                buffer.compact();
                String[] parts = message.split("\\|");
                String type = parts[0];
                String dataMessage = parts[1];
                handleMessage(type, dataMessage);
            }
        }
        return bytesRead;
    }

    @Override
    public void close() {
        logger.info("Request to close channel");
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("IO error while closing channel", e);
        }
    }

    protected abstract void handleMessage(String type, String message);

    private static final Logger logger = LoggerFactory.getLogger(AbstractReader.class);
}
