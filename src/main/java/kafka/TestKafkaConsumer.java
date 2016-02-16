package kafka;

import common.Service;
import model.Range;

import java.util.Collection;

public interface TestKafkaConsumer extends Service {
    void setListener(TestKafkaConsumerListener listener);
    void setPartitions(Collection<Range> partitions);
}
