package edu.umd.cs424.database.optimization;

import edu.umd.cs424.database.*;
import edu.umd.cs424.database.categories.*;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Iterator;

import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.query.QueryPlan;
import edu.umd.cs424.database.query.QueryOperator;
import edu.umd.cs424.database.query.QueryPlanException;

import edu.umd.cs424.database.table.Table;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.StringDataBox;
import edu.umd.cs424.database.databox.FloatDataBox;
import edu.umd.cs424.database.databox.BoolDataBox;

import org.junit.After;

@Category(HW4Tests.class)
public class TestOptimization2 {
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

        //t.createTableWithIndices(this.schema, TABLENAME, Arrays.asList("int"));

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
    public void test() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = db.beginTransaction();

        //creates a 100 records int 0 to 99
        try {
            for (int i = 0; i < 1000; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        //build the statistics on the table
        table.buildStatistics(transaction, 10);

        // end + create a new transaction
        transaction.end();
        transaction = this.db.beginTransaction();

        transaction.queryAs("T", "t1");
        transaction.queryAs("T", "t2");

        // add a join and a select to the QueryPlan
        QueryPlan query = transaction.query("t1");
        query.join("t2", "t1.int", "t2.int");
        //query.select("int", PredicateOperator.EQUALS, new IntDataBox(10));

        // execute the query and get the output
        query.executeOptimal();
        query.getFinalOperator();
    }
}
