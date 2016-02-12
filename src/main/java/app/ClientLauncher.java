package app;

import client.ClientApplication;
import common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CountDownLatch;

public class ClientLauncher {
    public static void main(String[] args) throws InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught error in thread " + t.getName(), e));
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("spring.config.client");
        Service service = context.getBean(ClientApplication.class);

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));

        service.start();
        latch.await();
        service.stop();
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientLauncher.class);
}
