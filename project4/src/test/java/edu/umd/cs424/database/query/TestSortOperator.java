package edu.umd.cs424.database.query;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.TimeoutScaling;
import edu.umd.cs424.database.categories.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.TestUtils;
import edu.umd.cs424.database.table.Record;

import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.junit.Assert.*;

@Category(ProjTests.class)
public class TestSortOperator {
    @Ignore
    public static class SortRecordComparator implements Comparator<Record> {
        private int columnIndex;

        public SortRecordComparator(int columnIndex) {
            this.columnIndex = columnIndex;

        }
        public int compare(Record o1, Record o2) {
            return o1.getValues().get(this.columnIndex).compareTo(
                       o2.getValues().get(this.columnIndex));
        }
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // 10 second max per method tested.
    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (10000 * TimeoutScaling.factor)));

    @Test
    @Category(PublicTests.class)
    public void testSortRun() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        List<Record> records = new ArrayList<>();
        List<Record> recordsToShuffle = new ArrayList<>();
        for (int i = 0; i < 288 * 3; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            records.add(r);
            recordsToShuffle.add(r);
        }
        Collections.shuffle(recordsToShuffle, new Random(42));
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        Run r = s.createRun();
        r.addRecords(recordsToShuffle);
        Run sortedRun = s.sortRun(r);
        Iterator<Record> iter = sortedRun.iterator();
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(records.get(i), iter.next());
            i++;
        }
        assertEquals(288 * 3, i);

    }

    @Test
    @Category(PublicTests.class)
    public void testMergeSortedRuns() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        List<Record> records = new ArrayList<>();
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        Run r1 = s.createRun();
        Run r2 = s.createRun();
        for (int i = 0; i < 288 * 3; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            records.add(r);
            if (i % 2 == 0) {
                r1.addRecord(r.getValues());
            } else {
                r2.addRecord(r.getValues());
            }

        }
        List<Run> runs = new ArrayList<>();
        runs.add(r1);
        runs.add(r2);
        Run mergedSortedRuns = s.mergeSortedRuns(runs);
        Iterator<Record> iter = mergedSortedRuns.iterator();
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(records.get(i), iter.next());
            i++;
        }
        assertEquals(288 * 3, i);
    }

    @Test
    @Category(PublicTests.class)
    public void testMergePass() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        List<Record> records1 = new ArrayList<>();
        List<Record> records2 = new ArrayList<>();
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        Run r1 = s.createRun();
        Run r2 = s.createRun();
        Run r3 = s.createRun();
        Run r4 = s.createRun();

        for (int i = 0; i < 288 * 4; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            if (i % 4 == 0) {
                r1.addRecord(r.getValues());
                records2.add(r);
            } else if (i % 4  == 1) {
                r2.addRecord(r.getValues());
                records1.add(r);
            } else if (i % 4  == 2) {
                r3.addRecord(r.getValues());
                records1.add(r);
            } else {
                r4.addRecord(r.getValues());
                records2.add(r);
            }

        }
        List<Run> runs = new ArrayList<>();
        runs.add(r3);
        runs.add(r2);
        runs.add(r1);
        runs.add(r4);
        List<Run> result = s.mergePass(runs);
        assertEquals(2, result.size());
        Iterator<Record> iter1 = result.get(0).iterator();
        Iterator<Record> iter2 = result.get(1).iterator();
        int i = 0;
        while (iter1.hasNext()) {
            assertEquals(records1.get(i), iter1.next());
            i++;
        }
        assertEquals(288 * 2, i);
        i = 0;
        while (iter2.hasNext()) {
            assertEquals(records2.get(i), iter2.next());
            i++;
        }
        assertEquals(288 * 2, i);

    }

    @Test
    @Category(PublicTests.class)
    public void testSortNoChange() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        Record[] records = new Record[288 * 3];
        for (int i = 0; i < 288 * 3; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            records[i] = r;
            transaction.addRecord("table", r.getValues());
        }
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        String sortedTableName = s.sort();
        Iterator<Record> iter = transaction.getRecordIterator(sortedTableName);
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(records[i], iter.next());
            i++;
        }
        assertEquals(288 * 3, i);

    }

    @Test
    @Category(PublicTests.class)
    public void testSortBackwards() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        Record[] records = new Record[288 * 3];
        for (int i = 288 * 3; i > 0; i--) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            records[i - 1] = r;
            transaction.addRecord("table", r.getValues());
        }
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        String sortedTableName = s.sort();
        Iterator<Record> iter = transaction.getRecordIterator(sortedTableName);
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(records[i], iter.next());
            i++;
        }
        assertEquals(288 * 3, i);

    }

    @Test
    @Category(PublicTests.class)
    public void testSortRandomOrder() throws QueryPlanException, DatabaseException, IOException {
        File tempDir = tempFolder.newFolder("sortTest");
        Database d = new Database(tempDir.getAbsolutePath(), 3);
        Database.Transaction transaction = d.beginTransaction();
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "table");
        List<Record> records = new ArrayList<>();
        List<Record> recordsToShuffle = new ArrayList<>();
        for (int i = 0; i < 288 * 3; i++) {
            Record r = TestUtils.createRecordWithAllTypesWithValue(i);
            records.add(r);
            recordsToShuffle.add(r);
        }
        Collections.shuffle(recordsToShuffle, new Random(42));
        for (Record r : recordsToShuffle) {
            transaction.addRecord("table", r.getValues());
        }
        SortOperator s = new SortOperator(transaction, "table", new SortRecordComparator(1));
        String sortedTableName = s.sort();
        Iterator<Record> iter = transaction.getRecordIterator(sortedTableName);
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(records.get(i), iter.next());
            i++;
        }
        assertEquals(288 * 3, i);

    }

}
