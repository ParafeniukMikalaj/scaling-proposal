package spring.config.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import coordination.Coordinator;
import coordination.impl.ZkCoordinator;
import hashing.HashRing;
import hashing.impl.HashRingImpl;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaProducer;
import kafka.impl.TestKafkaProducerImpl;
import producer.PeriodicTestKafkaProducer;
import kafka.impl.TestKafkaConsumerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.ServerApplication;

@Configuration
public class ServerSpringConfig {

    @Bean
    public Config config() {
        return ConfigFactory.load();
    }

    @Bean
    public Integer partitionsCount(Config config) {
        return config.getInt("partitions.count");
    }

    @Bean
    public HashRing<Integer, Integer> hashRing(Config config, Integer partitionsCount) {
        int splitPointsCount = config.getInt("hash.ring.split.points.count");
        return new HashRingImpl(partitionsCount, splitPointsCount);
    }

    @Bean
    public Coordinator coordinator(Config config) {
        String zkPath = config.getString("zk.path");
        String zkConnectionString = config.getString("zk.connection");
        return new ZkCoordinator(zkConnectionString, zkPath);
    }

    @Bean
    public TestKafkaConsumer testKafkaConsumer(Config config, Integer nodeId) {
        String bootstrap = config.getString("kafka.bootstrap");
        String topic = config.getString("kafka.topic");
        return new TestKafkaConsumerImpl(bootstrap, topic, nodeId);
    }

    @Bean
    public ServerApplication serverApplication(Integer nodeId, String host, Integer port) {
        return new ServerApplication();
    }
}
