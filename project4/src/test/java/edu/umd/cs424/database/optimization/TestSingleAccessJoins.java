package edu.umd.cs424.database.optimization;

import edu.umd.cs424.database.*;
import edu.umd.cs424.database.categories.*;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.query.QueryPlan.PredicateOperator;
import edu.umd.cs424.database.query.QueryPlan;
import edu.umd.cs424.database.query.QueryOperator;
import edu.umd.cs424.database.query.QueryPlanException;

import edu.umd.cs424.database.table.Table;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.StringDataBox;
import edu.umd.cs424.database.databox.FloatDataBox;
import edu.umd.cs424.database.databox.BoolDataBox;

@Category(HW4Tests.class)
public class TestSingleAccessJoins {
    private Table table;
    private Schema schema;
    public static final String TABLENAME = "T";

    public static final String TestDir = "testDatabase";
    private Database db;
    private String filename;

    //Before every test you create a temporary table, after every test you close it
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void beforeEach() throws Exception {
        File testDir = tempFolder.newFolder(TestDir);
        this.filename = testDir.getAbsolutePath();
        this.db = new DatabaseWithTableStub(filename);
        BaseTransaction t = this.db.beginTransaction();
        t.deleteAllTables();

        this.schema = TestUtils.createSchemaWithAllTypes();

        t.createTable(this.schema, TABLENAME);

        t.createTableWithIndices(this.schema, TABLENAME + "I", Arrays.asList("int"));
        t.createTableWithIndices(this.schema, TABLENAME + "MI", Arrays.asList("int", "bool"));

        t.createTable(TestUtils.createSchemaWithAllTypes("one_"), TABLENAME + "o1");
        t.createTable(TestUtils.createSchemaWithAllTypes("two_"), TABLENAME + "o2");
        t.createTable(TestUtils.createSchemaWithAllTypes("three_"), TABLENAME + "o3");
        t.createTable(TestUtils.createSchemaWithAllTypes("four_"), TABLENAME + "o4");

        t.end();
    }

    @After
    public void afterEach() {
        BaseTransaction t = this.db.beginTransaction();
        t.deleteAllTables();
        t.end();
        this.db.close();
    }

    //creates a record with all specified types
    private static Record createRecordWithAllTypes(boolean a1, int a2, String a3, float a4) {
        Record r = TestUtils.createRecordWithAllTypes();
        r.getValues().set(0, new BoolDataBox(a1));
        r.getValues().set(1, new IntDataBox(a2));
        r.getValues().set(2, new StringDataBox(a3, 5));
        r.getValues().set(3, new FloatDataBox(a4));
        return r;
    }

    @Test
    @Category(PublicTests.class)
    public void testSequentialScanSelection() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME, "t1");

        QueryPlan query = transaction.query("t1");

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isSequentialScan());
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleIndexScanSelection() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME + "I");
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME + "I", "t1");

        QueryPlan query = transaction.query("t1");
        query.select("int", PredicateOperator.EQUALS, new IntDataBox(9));

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isIndexScan());
    }

    @Test
    @Category(PublicTests.class)
    public void testPushDownSelects() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME, "t1");

        QueryPlan query = transaction.query("t1");
        query.select("int", PredicateOperator.EQUALS, new IntDataBox(9));

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isSelect());
        assert(op.getSource().isSequentialScan());
    }

    @Test
    @Category(PublicTests.class)
    public void testPushDownMultipleSelects() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME, "t1");

        QueryPlan query = transaction.query("t1");
        query.select("int", PredicateOperator.EQUALS, new IntDataBox(9));
        query.select("bool", PredicateOperator.EQUALS, new BoolDataBox(false));

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isSelect());
        assert(op.getSource().isSelect());
        assert(op.getSource().getSource().isSequentialScan());
    }

    @Test
    @Category(PublicTests.class)
    public void testNoValidIndices() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME + "MI");
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME + "MI", "t1");

        QueryPlan query = transaction.query("t1");

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isSequentialScan());
    }

    @Test
    @Category(PublicTests.class)
    public void testIndexSelectionAndPushDown() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME + "MI");
        BaseTransaction transaction = this.db.beginTransaction();

        try {
            for (int i = 0; i < 10000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        table.buildStatistics(transaction, 10);

        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs(TABLENAME + "MI", "t1");

        QueryPlan query = transaction.query("t1");
        query.select("int", PredicateOperator.EQUALS, new IntDataBox(9));
        query.select("bool", PredicateOperator.EQUALS, new BoolDataBox(false));

        QueryOperator op = query.minCostSingleAccess("t1");

        assert(op.isSelect());
        assert(op.getSource().isIndexScan());
    }
}
