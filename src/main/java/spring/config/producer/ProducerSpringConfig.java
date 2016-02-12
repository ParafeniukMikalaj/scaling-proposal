package spring.config.producer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import coordination.Coordinator;
import coordination.impl.ZkCoordinator;
import hashing.HashRing;
import hashing.impl.HashRingImpl;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaProducer;
import kafka.impl.TestKafkaConsumerImpl;
import kafka.impl.TestKafkaProducerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import producer.PeriodicTestKafkaProducer;
import server.ServerApplication;

@Configuration
public class ProducerSpringConfig {

    @Bean
    public Config config() {
        return ConfigFactory.load();
    }

    @Bean
    public TestKafkaProducer testKafkaProducer(Config config) {
        String bootstrap = config.getString("kafka.bootstrap");
        String topic = config.getString("kafka.topic");
        int partitions = config.getInt("kafka.partitions");
        return new TestKafkaProducerImpl(bootstrap, topic, partitions);
    }

    @Bean
    public PeriodicTestKafkaProducer periodicTestKafkaProducer(Config config) {
        int accountsCount = config.getInt("kafka.producer.accounts.count");
        int delay = config.getInt("kafka.producer.delay");
        return new PeriodicTestKafkaProducer(accountsCount, delay);
    }
}
