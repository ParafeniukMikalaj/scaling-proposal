package kafka.impl;

import com.google.common.collect.Sets;
import coordination.CoordinatedNode;
import coordination.impl.CoordinatedNodeImpl;
import hashing.HashRing;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaConsumerListener;
import model.impl.NodeImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TestKafkaConsumerImpl implements TestKafkaConsumer {

    private HashRing<Integer, Integer> hashRing;
    private final String topic;
    private Set<Integer> topicPartitions;
    private ExecutorService executor;
    private final String bootstrapServers;
    private volatile TestKafkaConsumerListener listener;
    private final int consumerId;

    public TestKafkaConsumerImpl(String bootstrapServers, String topic, int consumerId, String host, int port) {
        Collection<Integer> splitPoints = hashRing.generateSplitPoints(consumerId);
        CoordinatedNode coordinatedNode = new CoordinatedNodeImpl(new NodeImpl(consumerId, host, port), splitPoints);
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.consumerId = consumerId;
    }

    @Override
    public void setListener(TestKafkaConsumerListener listener) {
        this.listener = listener;
    }

    @Override
    public void setPartitions(Collection<Integer> partitions) {
        if (executor != null) {
            executor.shutdown();
        }

        if (topicPartitions != null) {
            Set<Integer> newPartitions = Sets.newHashSet(partitions);
            int acquiredPartitionsCount = Sets.difference(newPartitions, topicPartitions).size();
            int lostPartitionsCount = Sets.difference(topicPartitions, newPartitions).size();
            logger.info("Consumer {} lost {} and acquired {} partitions", consumerId, lostPartitionsCount, acquiredPartitionsCount);
            topicPartitions = newPartitions;
        }

        Properties consumerProperties = KafkaProperties.consumerProperties(bootstrapServers);
        final KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.assign(partitions.stream().map(p -> new TopicPartition(topic, p)).collect(Collectors.toList()));

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    if (listener != null) {
                        listener.consume(record.value());
                    }
                }
            }
            logger.info("Consumer thread interrupted");
        });
    }

    private static final Logger logger = LoggerFactory.getLogger(TestKafkaConsumerImpl.class);

}
