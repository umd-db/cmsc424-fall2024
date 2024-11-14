package edu.umd.cs424.database;

import edu.umd.cs424.database.categories.*;
import edu.umd.cs424.database.concurrency.LockType;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.query.QueryPlan;
import edu.umd.cs424.database.query.QueryPlanException;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.RecordId;
import edu.umd.cs424.database.table.Schema;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

@Category({HW5Tests.class, HW5Part2Tests.class})
public class TestDatabaseLocking {
    public static final String TestDir = "testDatabaseLocking";
    private Database db;
    private LoggingLockManager lockManager;
    private String filename;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // 10 second max per method tested.
    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (10000 * TimeoutScaling.factor)));

    private void reloadDatabase() throws DatabaseException {
        if (this.db != null) {
            this.db.close();
        }
        if (this.lockManager != null && this.lockManager.isLogging()) {
            List<String> oldLog = this.lockManager.log;
            this.lockManager = new LoggingLockManager();
            this.lockManager.log = oldLog;
            this.lockManager.startLog();
        } else {
            this.lockManager = new LoggingLockManager();
        }
        this.db = new DatabaseWithTableStub(this.filename, 5, this.lockManager);
    }

    @Before
    public void beforeEach() throws Exception {
        File testDir = tempFolder.newFolder(TestDir);
        this.filename = testDir.getAbsolutePath();
        this.reloadDatabase();
        BaseTransaction t = this.db.beginTransaction();
        t.deleteAllTables();
        t.end();
    }

    @After
    public void afterEach() throws Exception {
        this.lockManager.endLog();
        this.reloadDatabase();
        BaseTransaction t = this.db.beginTransaction();
        t.deleteAllTables();
        t.end();
        this.db.close();
    }

    public static <T extends Comparable<? super T>> void assertSameItems(List<T> expected,
            List<T> actual) {
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public static <T> void assertSubsequence(List<T> expected, List<T> actual) {
        if (expected.size() == 0) {
            return;
        }
        Iterator<T> ei = expected.iterator();
        Iterator<T> ai = actual.iterator();
        while (ei.hasNext()) {
            T next = ei.next();
            boolean found = false;
            while (ai.hasNext()) {
                if (ai.next().equals(next)) {
                    found = true;
                    break;
                }
            }
            assertTrue(expected + " not subsequence of " + actual, found);
        }
    }

    private List<RecordId> createTable(String tableName, int pages) throws DatabaseException {
        Schema s = TestUtils.createSchemaWithAllTypes();
        Record input = TestUtils.createRecordWithAllTypes();
        List<DataBox> values = input.getValues();
        List<RecordId> rids = new ArrayList<>();
        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.createTable(s, tableName);
            int numRecords = pages * db.getTable(tableName).getNumRecordsPerPage();
            for (int i = 0; i < numRecords; ++i) {
                rids.add(t1.addRecord(tableName, values));
            }
        }

        return rids;
    }

    private List<RecordId> createTableWithIndices(String tableName, int pages,
            List<String> indexColumns) throws DatabaseException {
        return createTableWithIndices(tableName, pages, indexColumns, true);
    }

    private List<RecordId> createTableWithIndices(String tableName, int pages,
            List<String> indexColumns,
            boolean disableChildLocks) throws DatabaseException {
        if (!disableChildLocks) {
            for (String indexCol : indexColumns) {
                toggleIndexDisableChildLocking(tableName + "," + indexCol, false);
            }
        }

        Schema s = TestUtils.createSchemaWithTwoInts();
        List<RecordId> rids = new ArrayList<>();
        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.createTableWithIndices(s, tableName, indexColumns);
            int numRecords = pages * db.getTable(tableName).getNumRecordsPerPage();
            for (int i = 0; i < numRecords; ++i) {
                rids.add(t1.addRecord(tableName, Arrays.asList(new IntDataBox(i), new IntDataBox(i))));
            }
        }

        if (!disableChildLocks) {
            for (String indexCol : indexColumns) {
                toggleIndexDisableChildLocking(tableName + "," + indexCol, true);
            }
            this.reloadDatabase();
        }

        return rids;
    }

    // Turns on/off disableChildLocking to allow some tests
    // to set things up and run before all locking integration has been completed.
    private void toggleIndexDisableChildLocking(String indexName, boolean enable) {
        try {
            Method getIndexContext = Database.class.getDeclaredMethod("getIndexContext", String.class);
            getIndexContext.setAccessible(true);
            LoggingLockContext indexContext = (LoggingLockContext) getIndexContext.invoke(db, indexName);
            getIndexContext.setAccessible(false);
            indexContext.allowDisableChildLocks(enable);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testRecordRead() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);
        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.getRecord(tableName, rids.get(0));
            t1.getRecord(tableName, rids.get(3 * rids.size() / 4 - 1));
            t1.getRecord(tableName, rids.get(rids.size() - 1));

            assertEquals(Arrays.asList(
                    "acquire 2 database IS",
                    "acquire 2 database/table-testTable1 IS",
                    "acquire 2 database/table-testTable1/1 S",
                    "acquire 2 database/table-testTable1/3 S",
                    "acquire 2 database/table-testTable1/4 S"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleTransactionCleanup() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);

        BaseTransaction t1 = db.beginTransaction();
        try {
            t1.getRecord(tableName, rids.get(0));
            t1.getRecord(tableName, rids.get(3 * rids.size() / 4 - 1));
            t1.getRecord(tableName, rids.get(rids.size() - 1));

            assertEquals("did not acquire all required locks", 5, lockManager.getLocks(t1).size());

            lockManager.startLog();
        } finally {
            t1.end();
        }

        assertTrue("did not free all required locks", lockManager.getLocks(t1).isEmpty());
        assertSubsequence(Arrays.asList(
                "release 2 database/table-testTable1/3",
                "release 2 database"
        ), lockManager.log);

    }

    @Test
    @Category(PublicTests.class)
    public void testRecordWrite() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);
        Record input = TestUtils.createRecordWithAllTypes();
        List<DataBox> values = input.getValues();

        try (BaseTransaction t0 = db.beginTransaction()) {
            t0.deleteRecord(tableName, rids.get(rids.size() - 1));
        }

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.addRecord(tableName, values);

            assertEquals(Arrays.asList(
                    "acquire 3 database IX",
                    "acquire 3 database/table-testTable1 IX",
                    "acquire 3 database/table-testTable1/4 X"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testRecordUpdate() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);
        Record input = TestUtils.createRecordWithAllTypes();
        List<DataBox> values = input.getValues();

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.updateRecord(tableName, values, rids.get(rids.size() - 1));

            assertEquals(Arrays.asList(
                    "acquire 2 database IX",
                    "acquire 2 database/table-testTable1 IX",
                    "acquire 2 database/table-testTable1/4 X"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testTableCleanup() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            db.getTable(tableName).cleanup(t1);

            assertEquals(Arrays.asList(
                    "acquire 2 database IX",
                    "acquire 2 database/table-testTable1 X"
            ), lockManager.log.subList(0, 2));
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testRecordDelete() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.deleteRecord(tableName, rids.get(rids.size() - 1));

            assertEquals(Arrays.asList(
                    "acquire 2 database IX",
                    "acquire 2 database/table-testTable1 IX",
                    "acquire 2 database/table-testTable1/4 X"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testTableScan() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 4);

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            Iterator<Record> r = t1.getRecordIterator(tableName);
            while (r.hasNext()) {
                r.next();
            }

            assertEquals(Arrays.asList(
                    "acquire 2 database IS",
                    "acquire 2 database/table-testTable1 S"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSortedScanNoIndexLocking() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 1);

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            Iterator<Record> r = t1.sortedScan(tableName, "int");
            while (r.hasNext()) {
                r.next();
            }

            assertEquals(Arrays.asList(
                    "acquire 2 database IS",
                    "acquire 2 database/table-testTable1 S"
            ), lockManager.log.subList(0, 2));
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testBPlusTreeRestrict() throws DatabaseException {
        String tableName = "testTable1";
        lockManager.startLog();

        createTableWithIndices(tableName, 0, Arrays.asList("int1"), false);
        assertTrue(lockManager.log.contains("disable-children database/index-testTable1,int1"));
    }

    @Test
    @Category(PublicTests.class)
    public void testSortedScanLocking() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTableWithIndices(tableName, 1, Arrays.asList("int1", "int2"), false);

        lockManager.startLog();
        try (BaseTransaction t1 = db.beginTransaction()) {
            Iterator<Record> r = t1.sortedScan(tableName, "int1");
            while (r.hasNext()) {
                r.next();
            }
            assertEquals(3, lockManager.log.size());
            assertEquals(Arrays.asList(
                "acquire 0 database IS",
                "acquire 0 database/table-testTable1 S",
                "acquire 0 database/index-testTable1,int1 S"
            ), lockManager.log);
        }

        lockManager.clearLog();
        try (BaseTransaction t2 = db.beginTransaction()) {
            Iterator<Record> r = t2.sortedScanFrom(tableName, "int2", new IntDataBox(rids.size() / 2));
            while (r.hasNext()) {
                r.next();
            }
            assertEquals(3, lockManager.log.size());
            assertEquals(Arrays.asList(
                    "acquire 1 database IS",
                    "acquire 1 database/table-testTable1 S",
                    "acquire 1 database/index-testTable1,int2 S"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSearchOperationLocking() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTableWithIndices(tableName, 1, Arrays.asList("int1", "int2"), false);

        lockManager.startLog();
        try (BaseTransaction t1 = db.beginTransaction()) {
            t1.lookupKey(tableName, "int1", new IntDataBox(rids.size() / 2));
            assertEquals(Arrays.asList(
                    "acquire 0 database IS",
                    "acquire 0 database/index-testTable1,int1 S"
            ), lockManager.log);
        }

        lockManager.clearLog();
        try (BaseTransaction t2 = db.beginTransaction()) {
            t2.contains(tableName, "int2", new IntDataBox(rids.size() / 2 - 1));
            assertEquals(Arrays.asList(
                    "acquire 1 database IS",
                    "acquire 1 database/index-testTable1,int2 S"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testQueryWithIndex() throws DatabaseException, QueryPlanException {
        String tableName = "testTable1";
        createTableWithIndices(tableName, 2, Arrays.asList("int1", "int2"), false);

        try (BaseTransaction t0 = db.beginTransaction()) {
            // This line only needs to be called if you have implemented Histogram and uncommented the
            // calls to estimateStats/estimateIOCost in the various operators.
            db.getTable("testTable1").buildStatistics(t0, 10);
        } catch (UnsupportedOperationException e) {
            /* do nothing, unimplemented */
        }

        lockManager.startLog();

        try (BaseTransaction t1 = db.beginTransaction()) {
            QueryPlan q = t1.query(tableName);
            q.select("int1", QueryPlan.PredicateOperator.EQUALS, new IntDataBox(2));
            q.project(Arrays.asList("int2"));
            Iterator<Record> iter = q.execute();

            assertEquals(Arrays.asList(
                    "acquire 1 database IS",
                    "acquire 1 database/index-testTable1,int1 S"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testTempTable() throws DatabaseException {
        lockManager.startLog();

        try (BaseTransaction t0 = db.beginTransaction()) {
            String tableName = t0.createTempTable(TestUtils.createSchemaWithAllTypes());

            assertTrue(lockManager.log.contains("disable-children temp-" + tableName));
            assertTrue(LockType.substitutable(lockManager.orphanContext("temp-" +
                    tableName).getExplicitLockType(
                    t0), LockType.S));
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testPageAllocatorInitCapacity() throws DatabaseException {
        String tableName = "testTable1";
        createTable(tableName, 0);
        db.close();
        this.lockManager = new LoggingLockManager();
        lockManager.startLog();

        this.reloadDatabase();
        assertTrue(lockManager.log.contains("set-capacity database/table-testTable1 2"));
    }

    @Test
    @Category(PublicTests.class)
    public void testAutoEscalateS() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 18);

        lockManager.startLog();

        try (BaseTransaction t0 = db.beginTransaction()) {
            t0.getRecord(tableName, rids.get(0));
            t0.getRecord(tableName, rids.get(rids.size() / 5));
            t0.getRecord(tableName, rids.get(rids.size() / 5 * 2));
            t0.getRecord(tableName, rids.get(rids.size() / 5 * 3));
            t0.getRecord(tableName, rids.get(rids.size() - 1));

            assertEquals(Arrays.asList(
                    "acquire 2 database IS",
                    "acquire 2 database/table-testTable1 IS",
                    "acquire 2 database/table-testTable1/1 S",
                    "acquire 2 database/table-testTable1/4 S",
                    "acquire 2 database/table-testTable1/8 S",
                    "acquire 2 database/table-testTable1/11 S",
                    "acquire-and-release 2 database/table-testTable1 S [database/table-testTable1, " +
                            "database/table-testTable1/1, database/table-testTable1/11, " +
                            "database/table-testTable1/4, database/table-testTable1/8]"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testAutoEscalateX() throws DatabaseException {
        String tableName = "testTable1";
        List<RecordId> rids = createTable(tableName, 18);
        List<DataBox> values = TestUtils.createRecordWithAllTypes().getValues();

        lockManager.startLog();

        try (BaseTransaction t0 = db.beginTransaction()) {
            t0.updateRecord(tableName, values, rids.get(0));
            t0.deleteRecord(tableName, rids.get(rids.size() / 5));
            t0.updateRecord(tableName, values, rids.get(rids.size() / 5 * 2));
            t0.deleteRecord(tableName, rids.get(rids.size() / 5 * 3));
            t0.updateRecord(tableName, values, rids.get(rids.size() - 1));

            assertEquals(Arrays.asList(
                    "acquire 2 database IX",
                    "acquire 2 database/table-testTable1 IX",
                    "acquire 2 database/table-testTable1/1 X",
                    "acquire 2 database/table-testTable1/4 X",
                    "acquire 2 database/table-testTable1/8 X",
                    "acquire 2 database/table-testTable1/11 X",
                    "acquire-and-release 2 database/table-testTable1 X [database/table-testTable1, " +
                            "database/table-testTable1/1, database/table-testTable1/11, " +
                            "database/table-testTable1/4, database/table-testTable1/8]"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testCreateTableSimple() throws DatabaseException {
        lockManager.startLog();
        createTable("testTable1", 4);
        assertEquals(Arrays.asList(
                         "acquire 1 database IX",
                         "acquire 1 database/table-testTable1 X"
                     ), lockManager.log.subList(0, 2));
    }

    @Test
    @Category(PublicTests.class)
    public void testDeleteTableSimple() throws DatabaseException {
        String tableName = "testTable1";
        createTable(tableName, 0);

        lockManager.startLog();

        try (BaseTransaction t0 = db.beginTransaction()) {
            t0.deleteTable(tableName);

            assertEquals(Arrays.asList(
                    "acquire 2 database IX",
                    "acquire 2 database/table-testTable1 X"
            ), lockManager.log);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testDeleteAllTables() {
        lockManager.startLog();

        try (BaseTransaction t0 = db.beginTransaction()) {
            t0.deleteAllTables();

            assertEquals(Arrays.asList(
                    "acquire 1 database X"
            ), lockManager.log);
        }
    }
}
