package kafka.impl;

import kafka.TestKafkaProducer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

public class TestKafkaProducerImpl implements TestKafkaProducer, Callback {

    private final KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final int partitionsCount;
    private final Random random = new Random(137);

    public TestKafkaProducerImpl(String bootstrapServers, String topic, int partitionsCount) {
        Properties producerProperties = KafkaProperties.producerProperties(bootstrapServers);
        this.topic = topic;
        this.partitionsCount = partitionsCount;
        kafkaProducer = new KafkaProducer<>(producerProperties);
    }

    @Override
    public void produce(int accountId, int value) {
        String stringValue = accountId + "|" + value;
        int partition = Integer.hashCode(accountId) % partitionsCount;
        logger.info("Publish value {} to client {} at partition {}", value, accountId, partition);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, partition, stringValue, stringValue);
        kafkaProducer.send(record, this);
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            logger.error("Error while publishing message to kafka", exception);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(TestKafkaProducerImpl.class);
}
