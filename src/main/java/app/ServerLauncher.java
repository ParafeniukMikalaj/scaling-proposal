package app;

import common.Service;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import server.ServerApplication;

import java.util.concurrent.CountDownLatch;

public class ServerLauncher {

    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
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

        service.start();
        latch.await();
        service.stop();
    }
}
