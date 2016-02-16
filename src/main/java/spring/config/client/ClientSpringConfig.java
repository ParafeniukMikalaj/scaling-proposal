package spring.config.client;

import client.ClientApplication;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
public class ClientSpringConfig {

    @Bean
    public Config config() {
        return ConfigFactory.load();
    }

    @Bean
    public ClientApplication periodicTestKafkaProducer(Config config) {
        int clientsCount = config.getInt("accounts.count");
        int spawnDelay = config.getInt("client.spawn.delay");
        int decommissionDelay = config.getInt("client.decommission.delay");

        List<InetSocketAddress> servers = Lists.newArrayList();
        for (String serverString : config.getStringList("client.servers")) {
            String[] parts = serverString.split(":");
            servers.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }

        return new ClientApplication(clientsCount, spawnDelay, decommissionDelay, servers);
    }
}
