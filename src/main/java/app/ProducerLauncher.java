package app;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import producer.PeriodicTestKafkaProducer;

import java.util.concurrent.CountDownLatch;

public class ProducerLauncher {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.producer");
        PeriodicTestKafkaProducer producer = context.getBean(PeriodicTestKafkaProducer.class);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        producer.start();
        latch.await();
        producer.stop();
    }
}
