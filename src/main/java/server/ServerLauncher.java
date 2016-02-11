package server;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ServerLauncher {

    public static void main(String[] args) throws InterruptedException {
        Integer nodeId  = Integer.parseInt(args[0]);
        String host = args[1];
        Integer port = Integer.parseInt(args[2]);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton("nodeId", nodeId);
        beanFactory.registerSingleton("host", host);
        beanFactory.registerSingleton("port", port);
        context.scan("spring.config");
        context.refresh();

        ServerApplication application = beanFactory.getBean(ServerApplication.class);

    }
}
