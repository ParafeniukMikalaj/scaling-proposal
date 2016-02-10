package kafka;

public interface KafkaFactory {
    TestKafkaConsumer createConsumer(int consumerId, String topic);
    TestKafkaProducer createProducer(String topic);
}
