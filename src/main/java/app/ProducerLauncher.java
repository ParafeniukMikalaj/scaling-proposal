package app;

import common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import producer.PeriodicTestKafkaProducer;

import java.util.concurrent.CountDownLatch;

public class ProducerLauncher {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.producer");
        Service service = context.getBean(PeriodicTestKafkaProducer.class);

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught error in thread " + t.getName(), e));

        service.start();
        latch.await();
        service.stop();
    }

    private static final Logger logger = LoggerFactory.getLogger(ProducerLauncher.class);
}
