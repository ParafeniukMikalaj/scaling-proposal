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
    }

    @Override
    public void performRead() {
        try {
            // TODO maybe while loop should be used
            channel.write(buffer);
        } catch (IOException e) {
            logger.error("Unexpected error while writing buffer to channel. It should be already connected", e);
        }
        if (buffer.position() > 4) {
            int messageLen = buffer.getInt();
            if (buffer.position() > messageLen + 4) {
                logger.info("Reader will try to parse message");
                logger.info("Buffer before read position {} limit {}", buffer.position(), buffer.limit());
                String message = new String(buffer.array(), 4, messageLen);
                buffer.position(messageLen + 4);
                buffer.compact();
                logger.info("Buffer after read position {} limit {}", buffer.position(), buffer.limit());
                String[] parts = message.split("|");
                String type = parts[0];
                String dataMessage = parts[1];
                handleMessage(type, dataMessage);
            }
        }

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
