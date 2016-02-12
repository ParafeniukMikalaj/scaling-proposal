package app;

import client.ClientApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CountDownLatch;

public class ClientLauncher {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.client");
        ClientApplication clients = context.getBean(ClientApplication.class);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        clients.start();
        latch.await();
        clients.stop();
    }
}
