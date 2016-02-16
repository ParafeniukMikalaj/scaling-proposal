package producer;

import common.Service;
import kafka.TestKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeriodicTestKafkaProducer implements Service {

    private final int delay;
    private int counter;
    private final int accountsCount;

    @Autowired
    private TestKafkaProducer producer;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PeriodicTestKafkaProducer(int delay, int accountsCount) {
        this.accountsCount = accountsCount;
        this.delay = delay;
    }

    @Override
    public void start() {
        executor.execute(this::producePeriodically);
    }

    @Override
    public void stop() {
        executor.shutdown();
    }

    private void producePeriodically() {
        while (!Thread.currentThread().isInterrupted()) {
            int value = ++counter;
            int account = value % accountsCount;
            producer.produce(account, value);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PeriodicTestKafkaProducer.class);
}
