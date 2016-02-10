import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import hashing.HashRing;
import hashing.impl.HashRingImpl;
import model.Node;
import model.impl.NodeImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class HashRingImplTest {

    private static final int PORT = 5222;

    @Test
    public void randomPartitionsWithDeletions() {
        HashRing<Node, Integer> ring = new HashRingImpl(100, 3);

        List<Integer> accountIds = getAccounts(100);
        List<Node> nodes = addNodesToRing(ring, 3);

        List<Integer> accountPartitions = getUniquePartitions(ring, accountIds);
        Assert.assertThat(accountPartitions.size(), lessThanOrEqualTo(3));

        ring.remove(nodes.get(1));

        accountPartitions = getUniquePartitions(ring, accountIds);
        Assert.assertThat(accountPartitions.size(), lessThanOrEqualTo(2));
    }

    @Test
    public void testMigrationRatio() {
        HashRing<Node, Integer> ring = new HashRingImpl(100, 3);
        int accountsCount = 1000;
        int nodesCount = 10;
        List<Integer> accountIds = getAccounts(accountsCount);
        List<Node> nodes = addNodesToRing(ring, nodesCount);

        Multimap<Integer, Integer> nodeAccounts = ArrayListMultimap.create();
        for (Integer accountId : accountIds) {
            nodeAccounts.put(ring.hash(accountId).id(), accountId);
        }

        ring.add(new NodeImpl(100, "host101", PORT));
        int totalMigratedAccounts = 0;

        for (Node node : nodes) {
            Collection<Integer> ownedAccounts = nodeAccounts.get(node.id());
            Collection<Integer> notOwnedAccounts = ring.filterNotOwnedValues(node, ownedAccounts);
            int migratedAccountsSize = notOwnedAccounts.size();
            totalMigratedAccounts += migratedAccountsSize;
            if (migratedAccountsSize > 0) {
                logger.info("node {} lost {} accounts", node.id(), migratedAccountsSize);
            }
        }
        double actualMigrationRatio = (double) totalMigratedAccounts / accountsCount;
        double expectedMigratinRadio = (double) 1 / nodesCount;
        logger.info("{} accounts migrated. Migration ratio is {}, while expected {}", totalMigratedAccounts,
                actualMigrationRatio, expectedMigratinRadio);
    }

    private List<Node> addNodesToRing(HashRing<Node, Integer> ring, int count) {
        List<Node> nodes = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            NodeImpl node = new NodeImpl(i, "host" + (i + 1), PORT);
            nodes.add(node);
            ring.add(node);
        }
        return nodes;
    }

    private List<Integer> getAccounts(int count) {
        return IntStream.range(0, count).boxed().collect(Collectors.toList());
    }

    private List<Integer> getUniquePartitions(HashRing<Node, Integer> ring, List<Integer> accountIds) {
        return accountIds.stream().map(a -> ring.hash(a).id()).distinct().sorted().collect(Collectors.toList());
    }

    private static final Logger logger = LoggerFactory.getLogger(HashRingImplTest.class);
}
