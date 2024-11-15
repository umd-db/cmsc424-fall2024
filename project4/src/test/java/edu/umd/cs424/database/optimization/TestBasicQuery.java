package edu.umd.cs424.database.optimization;

import edu.umd.cs424.database.*;
import edu.umd.cs424.database.categories.*;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Iterator;
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

import static org.junit.Assert.*;
import org.junit.After;

@Category(HW4Tests.class)
public class TestBasicQuery {
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

        t.createTableWithIndices(this.schema, TABLENAME + "2", Arrays.asList("int"));

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
    public void testProject() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = this.db.beginTransaction();

        //creates a 10 records int 0 to 9
        try {
            for (int i = 0; i < 10; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        //build the statistics on the table
        table.buildStatistics(transaction, 10);

        // end transaction + create a new transaction
        transaction.end();
        transaction = this.db.beginTransaction();

        // add a project to the QueryPlan
        QueryPlan query = transaction.query("T");
        query.project(Arrays.asList("int"));

        // execute the query and get the output
        Iterator<Record> queryOutput = query.executeOptimal();

        QueryOperator finalOperator = query.getFinalOperator();

        //tests to see if projects are applied properly
        int count = 0;
        while(queryOutput.hasNext()) {
            Record r = queryOutput.next();
            assertEquals(r.getValues().get(0), new IntDataBox(count));
            count++;
        }

    }

    @Test
    @Category(PublicTests.class)
    public void testSelect() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = db.beginTransaction();

        //creates a 10 records int 0 to 9
        try {
            for (int i = 0; i < 10; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        //build the statistics on the table
        table.buildStatistics(transaction, 10);

        // end transaction + create a new transaction
        transaction.end();
        transaction = this.db.beginTransaction();

        // add a select to the QueryPlan
        QueryPlan query = transaction.query("T");
        query.select("int", PredicateOperator.EQUALS, new IntDataBox(9));

        // execute the query and get the output
        Iterator<Record> queryOutput = query.executeOptimal();

        QueryOperator finalOperator = query.getFinalOperator();

        //tests to see if projects are applied properly
        assert(queryOutput.hasNext());

        Record r = queryOutput.next();
        assertEquals(r.getValues().get(1), new IntDataBox(9));

    }

    @Test
    @Category(PublicTests.class)
    public void testGroupBy() throws DatabaseException, QueryPlanException {
        Table table = db.getTable(TABLENAME);
        BaseTransaction transaction = db.beginTransaction();

        //creates a 100 records int 0 to 9
        try {
            for (int i = 0; i < 100; ++i) {
                Record r = createRecordWithAllTypes(false, i % 10, "test", 0.0f);
                table.addRecord(transaction, r.getValues());
            }
        } catch(DatabaseException e) {}

        //build the statistics on the table
        table.buildStatistics(transaction, 10);

        // end + create a new transaction
        transaction.end();
        transaction = this.db.beginTransaction();

        // add a project and a groupby to the QueryPlan
        QueryPlan query = transaction.query("T");
        query.groupBy("T.int");
        query.count();

        // execute the query and get the output
        Iterator<Record> queryOutput = query.executeOptimal();

        QueryOperator finalOperator = query.getFinalOperator();

        //tests to see if projects/group by are applied properly
        int count = 0;
        while(queryOutput.hasNext()) {
            Record r = queryOutput.next();
            assertEquals(r.getValues().get(0).getInt(), 10);
            count++;
        }

        assertEquals(count, 10);
    }
}
