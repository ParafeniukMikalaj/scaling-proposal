package kafka.impl;

import com.google.common.collect.Sets;
import common.Service;
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

public class TestKafkaConsumerImpl implements TestKafkaConsumer, Service {

    private final String topic;
    private Set<Integer> topicPartitions;
    private ExecutorService executor;
    private final String bootstrapServers;
    private volatile TestKafkaConsumerListener listener;
    private final int consumerId;

    public TestKafkaConsumerImpl(String bootstrapServers, String topic, int consumerId) {
        logger.info("Creating kafka consumer with id {} for topic {}", consumerId, topic);
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.consumerId = consumerId;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        logger.info("Shutting down current executor");
        executor.shutdown();
    }

    @Override
    public void setListener(TestKafkaConsumerListener listener) {
        this.listener = listener;
    }

    @Override
    public void setPartitions(Collection<Integer> partitions) {
        logger.info("Request to update partitions from {} to {}", topicPartitions, partitions);
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

        logger.info("Starting new consumer executor");
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                if (!records.isEmpty()) {
                    logger.info("Received {} records from kafka. Listener is {}", records.count(), listener != null ? "not empty" : "empty");
                }
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
