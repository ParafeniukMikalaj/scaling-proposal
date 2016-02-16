package kafka.impl;

import com.google.common.collect.Sets;
import common.Service;
import kafka.TestKafkaConsumer;
import kafka.TestKafkaConsumerListener;
import model.Range;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestKafkaConsumerImpl implements TestKafkaConsumer, Service {

    private final String topic;
    private Set<Range> topicPartitionRanges;
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
    public void setPartitions(Collection<Range> partitionRanges) {
        logger.info("Request to update partitionRanges from {} to {}", topicPartitionRanges, partitionRanges);
        stopPreviousExecutor();

        Set<Integer> newPartitions = expandRanges(partitionRanges);

        if (topicPartitionRanges != null) {
            Set<Integer> oldPartitions = expandRanges(topicPartitionRanges);
            int acquiredPartitionsCount = Sets.difference(newPartitions, oldPartitions).size();
            int lostPartitionsCount = Sets.difference(oldPartitions, newPartitions).size();
            logger.info("Consumer {} lost {} and acquired {} partitionRanges", consumerId, lostPartitionsCount, acquiredPartitionsCount);
        }

        topicPartitionRanges = Sets.newHashSet(partitionRanges);

        Set<Integer> topicPartitions = expandRanges(topicPartitionRanges);

        Properties consumerProperties = KafkaProperties.consumerProperties(bootstrapServers);
        final KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.assign(topicPartitions.stream().map(p -> new TopicPartition(topic, p)).collect(Collectors.toList()));

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

    private void stopPreviousExecutor() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor.shutdownNow();
        }
    }

    private Set<Integer> expandRanges(Collection<Range> ranges) {
        Set<Integer> result = Sets.newHashSet();
        ranges.forEach(r -> result.addAll(IntStream.range(r.from(), r.to()).boxed().collect(Collectors.toList())));
        return result;
    }

    private static final Logger logger = LoggerFactory.getLogger(TestKafkaConsumerImpl.class);
}
