package producer;

import kafka.TestKafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeriodicTestKafkaProducer {

    private final int accountCount;
    private final int delay;
    private int counter;

    @Autowired
    private TestKafkaProducer producer;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PeriodicTestKafkaProducer(int accountsCount, int delay) {
        this.accountCount = accountsCount;
        this.delay = delay;
    }

    public void start() {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                producer.produce(++counter);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

}
