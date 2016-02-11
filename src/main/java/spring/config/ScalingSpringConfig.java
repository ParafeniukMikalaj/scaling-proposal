package spring.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hashing.HashRing;
import hashing.impl.HashRingImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.ServerApplication;
import server.ServerApplicationConfigProvider;

@Configuration
public class ScalingSpringConfig {

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
    public ServerApplication serverApplication(Integer nodeId, String host, Integer port) {
        return new ServerApplication(nodeId, host, port);
    }
}
