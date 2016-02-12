package app;

import client.ClientApplication;
import common.Service;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CountDownLatch;

public class ClientLauncher {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.client");
        Service service = context.getBean(ClientApplication.class);

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));

        service.start();
        latch.await();
        service.stop();
    }
}
