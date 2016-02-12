package kafka;

import common.Service;

import java.util.Collection;

public interface TestKafkaConsumer extends Service {
    void setListener(TestKafkaConsumerListener listener);
    void setPartitions(Collection<Integer> partitions);
}
