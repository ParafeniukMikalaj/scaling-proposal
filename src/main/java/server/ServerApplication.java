package server;

import com.typesafe.config.Config;
import coordination.Coordinator;
import hashing.HashRing;
import kafka.impl.TestKafkaConsumerImpl;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ServerApplication {

    public ServerApplication() {
//        consumer = new TestKafkaConsumerImpl()
    }

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("spring.config");
        Config config = context.getBean(Config.class);
        logger.info(config.getString("foo.test"));
    }

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
}
