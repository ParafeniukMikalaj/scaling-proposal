package app;

import common.Service;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import producer.PeriodicTestKafkaProducer;

import java.util.concurrent.CountDownLatch;

public class ProducerLauncher {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.service");
        Service service = context.getBean(PeriodicTestKafkaProducer.class);

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));

        service.start();
        latch.await();
        service.stop();
    }
}
