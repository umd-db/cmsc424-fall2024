package edu.umd.cs424.database.query;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.TestUtils;
import edu.umd.cs424.database.TimeoutScaling;
import edu.umd.cs424.database.categories.ProjTests;
import edu.umd.cs424.database.categories.PublicTests;
import edu.umd.cs424.database.table.Record;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

@Category(ProjTests.class)
public class TestHashJoinOperator {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // 10 second max per method tested.
    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (20000 * TimeoutScaling.factor)));

    Database.Transaction newTransaction() {
        return newTransaction(5);
    }

    Database.Transaction newTransaction(int numMemoryPages) {
        try {
            File tempDir = tempFolder.newFolder("hashJoinTest");
            return new Database(tempDir.getAbsolutePath(), numMemoryPages).beginTransaction();
        } catch (Exception e) {
            fail("An exception occurred. " + e.getMessage());
        }
        // unreachable
        return null;
    }

    @Test
    @Category(PublicTests.class)
    public void testHashJoin_randomSmall() throws QueryPlanException, DatabaseException {
        testHashJoin(5, 10, 5);
    }

    @Test
    @Category(PublicTests.class)
    public void testHashJoin_randomMedium() throws QueryPlanException, DatabaseException {
        testHashJoin(107, 20, 3);
    }

    @Test
    @Category(PublicTests.class)
    public void testHashJoin_randomLarge() throws QueryPlanException, DatabaseException {
        testHashJoin(503, 10, 5);
    }

    private void testHashJoin(int size, int bucketSizeLimit, int numSubRuns) throws DatabaseException, QueryPlanException {
        var transaction = newTransaction();
        var schema = TestUtils.createSchemaWithAllTypes();
        String left = "left", right = "right";
        transaction.createTable(schema, left);
        transaction.createTable(schema, right);

        Random random = new Random(42);
        Map<Integer, Integer> frequencies = new HashMap<>();
        for (int i = size - 1; i >= 0; i--) {
            int freq = random.nextInt(1, 4);
            frequencies.put(i, freq);
            String target = random.nextInt(2) == 0 ? left : right;
            String other = target.equals(left) ? right : left;
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            while (freq-- > 0) {
                transaction.addRecord(target, r.getValues());
            }
            transaction.addRecord(other, r.getValues());
        }

        for (int i = size; i < 2 * size; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            transaction.addRecord(left, r.getValues());
        }

        for (int i = size * 2; i < 3 * size; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            transaction.addRecord(right, r.getValues());
        }

        HashJoinOperator operator = new HashJoinOperator(
                new SequentialScanOperator(transaction, left),
                new SequentialScanOperator(transaction, right),
                "int",
                "int",
                transaction,
                (a, b) -> a * b);
        operator.setBucketSizeLimit(bucketSizeLimit);
        operator.setNumSubRuns(numSubRuns);
        var iter = operator.iterator();

        while (iter.hasNext()) {
            var r = iter.next();
            int value = r.getValues().get(1).getInt();
            assertTrue("Unexpected value %d from join result".formatted(value), frequencies.containsKey(value));
            int freq = frequencies.get(value);
            assertNotEquals(
                    "More occurrences of %d than expected".formatted(value),
                    0, freq);
            // One less element expected.
            frequencies.put(value, freq - 1);
        }

        for (var key : frequencies.keySet()) {
            int freq = frequencies.get(key);
            assertEquals(
                    "Record %d is expected to occur %d more times".formatted(key, freq),
                    0, freq);
        }

        transaction.close();
    }

}
