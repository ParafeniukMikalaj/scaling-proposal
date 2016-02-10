package kafka;

import java.util.Collection;

public interface TestKafkaConsumer {
    void setListener(TestKafkaConsumerListener listener);
    void setPartitions(Collection<Integer> partitions);
}
