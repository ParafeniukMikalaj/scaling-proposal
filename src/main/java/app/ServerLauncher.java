package app;

import common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import server.ServerApplication;

import java.util.concurrent.CountDownLatch;

public class ServerLauncher {

    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        if (args.length < 3) {
            System.err.println("Usage <nodeId> <host> <port>");
            System.err.println("Example:");
            System.err.println("First  server: 0 localhost 12001");
            System.err.println("Second server: 1 localhost 12002");
            System.exit(1);
        }

        Integer nodeId  = Integer.parseInt(args[0]);
        String host = args[1];
        Integer port = Integer.parseInt(args[2]);
        beanFactory.registerSingleton("nodeId", nodeId);
        beanFactory.registerSingleton("host", host);
        beanFactory.registerSingleton("port", port);
        context.scan("spring.config.server");
        context.refresh();

        Service service = beanFactory.getBean(ServerApplication.class);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught error in thread " + t.getName(), e));

        service.start();
        latch.await();
        service.stop();
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);
}
