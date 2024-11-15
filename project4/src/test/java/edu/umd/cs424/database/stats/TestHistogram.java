package edu.umd.cs424.database.stats;

import edu.umd.cs424.database.categories.*;
import edu.umd.cs424.database.concurrency.DummyLockContext;
import edu.umd.cs424.database.table.TableStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import edu.umd.cs424.database.table.stats.Histogram;
import edu.umd.cs424.database.TestUtils;
import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.query.QueryPlan.PredicateOperator;

import edu.umd.cs424.database.table.Table;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.StringDataBox;
import edu.umd.cs424.database.databox.FloatDataBox;
import edu.umd.cs424.database.databox.BoolDataBox;

import static org.junit.Assert.*;
import org.junit.After;

@Category(HW4Tests.class)
public class TestHistogram {
    private Table table;
    private Schema schema;
    public static final String TABLENAME = "testtable";

    //Before every test you create a temporary table, after every test you close it
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void beforeEach() throws Exception {
        this.schema = TestUtils.createSchemaWithAllTypes();
        this.table = createTestTable(this.schema, TABLENAME);
    }

    @After
    public void afterEach() {
        this.table.close();
    }

    private Table createTestTable(Schema schema, String tableName) throws DatabaseException {
        try {
            File file = tempFolder.newFile(tableName + Table.FILENAME_EXTENSION);
            return new TableStub(tableName, schema, file.getAbsolutePath(), new DummyLockContext(), null);
        } catch (IOException e) {
            throw new DatabaseException(e.getMessage());
        }
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

    private String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    @Test
    @Category(PublicTests.class)
    public void testBuildHistogramBasic() {
        //creates a 101 records int 0 to 100
        try {
            for (int i = 0; i < 101; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 1); //build on the integer col

        assertEquals(101, h.getCount()); //count updated properly

        assertEquals(101, h.getNumDistinct()); //distinct count updated properly

        for (int i = 0; i < 9; i++) {
            assertEquals(10, h.get(i).getCount());
        }

        assertEquals(11, h.get(9).getCount());
    }

    @Test
    @Category(PublicTests.class)
    public void testBuildHistogramString() {
        //creates a 101 records int 0 to 100
        try {
            for (int i = 0; i < 101; ++i) {
                Record r = createRecordWithAllTypes(false, 0, getSaltString(), 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 2); //build on the integer col

        assertEquals(101, h.getCount()); //count updated properly

        assertEquals(101, h.getNumDistinct()); //distinct count updated properly
    }

    @Test
    @Category(PublicTests.class)
    public void testBuildHistogramEdge() {
        //creates a 101 records int 0 to 100
        try {
            for (int i = 0; i < 101; ++i) {
                Record r = createRecordWithAllTypes(false, 0, "test", 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 1); //build on the integer col

        assertEquals(101, h.getCount()); //count updated properly

        assertEquals(1, h.getNumDistinct()); //distinct count updated properly

        for (int i = 0; i < 9; i++) {
            assertEquals(0, h.get(i).getCount());
        }

        assertEquals(101, h.get(9).getCount());
    }

    @Test
    @Category(PublicTests.class)
    public void testEquality() {
        //creates a 100 records int 0 to 99
        try {
            for (int i = 0; i < 100; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 1); //build on the integer col

        //Should return [0.1,0,0,0,0,0,0,0,0,0,0]
        float [] result = h.filter(PredicateOperator.EQUALS, new IntDataBox(5));
        assert(Math.abs(result[0] - 0.1) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [0.9,1,1,1,1,1,1,1,1,1,1]
        result = h.filter(PredicateOperator.NOT_EQUALS, new IntDataBox(5));
        assert(Math.abs(result[0] - 0.9) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

        //Should return [0,0,0,0,0,0,0,0,0,0,0.1]
        result = h.filter(PredicateOperator.EQUALS, new IntDataBox(99));
        assert(Math.abs(result[9] - 0.1) < 0.00001);
        for (int i = 0; i < 9; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [0,0,0,0,0,0,0,0,0,0,0.0]
        result = h.filter(PredicateOperator.EQUALS, new IntDataBox(100));
        for (int i = 0; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [0,0,0,0,0,0,0,0,0,0,0.0]
        result = h.filter(PredicateOperator.EQUALS, new IntDataBox(-1));
        for (int i = 0; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]
        result = h.filter(PredicateOperator.NOT_EQUALS, new IntDataBox(-1));
        for (int i = 0; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

    }

    @Test
    @Category(PublicTests.class)
    public void testGreaterThan() {
        //creates a 101 records int 0 to 100
        try {
            for (int i = 0; i <= 100; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 1); //build on the integery col

        //Should return [0.1,1,1,1,1,1,1,1,1,1,1]
        float [] result = h.filter(PredicateOperator.GREATER_THAN, new IntDataBox(9));
        assert(Math.abs(result[0] - 0.1) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

        //Should return [0.0,1,1,1,1,1,1,1,1,1,1]
        result = h.filter(PredicateOperator.GREATER_THAN, new IntDataBox(10));
        assert(Math.abs(result[0] - 0.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

        //Should return [1,1,1,1,1,1,1,1,1,1,1]
        result = h.filter(PredicateOperator.GREATER_THAN, new IntDataBox(-1));
        assert(Math.abs(result[0] - 1.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

        //Should return [0,0,0,0,0,0,0,0,0,0,0.0]
        result = h.filter(PredicateOperator.GREATER_THAN, new IntDataBox(101));
        assert(Math.abs(result[0] - 0.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

    }

    @Test
    @Category(PublicTests.class)
    public void testLessThan() {
        //creates a 101 records int 0 to 100
        try {
            for (int i = 0; i <= 100; ++i) {
                Record r = createRecordWithAllTypes(false, i, "test", 0.0f);
                table.addRecord(null, r.getValues());
            }
        } catch(DatabaseException e) {}

        //creates a histogram of 10 buckets
        Histogram h = new Histogram(10);
        h.buildHistogram(null, table, 1); //build on the integery col

        //Should return [0.9,0,0,0,0,0,0,0,0,0,0]
        float [] result = h.filter(PredicateOperator.LESS_THAN, new IntDataBox(9));
        assert(Math.abs(result[0] - 0.9) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [1.0,0,0,0,0,0,0,0,0,0,0]
        result = h.filter(PredicateOperator.LESS_THAN, new IntDataBox(10));
        assert(Math.abs(result[0] - 1.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

        //Should return [1,1,1,1,1,1,1,1,1,1,1]
        result = h.filter(PredicateOperator.LESS_THAN, new IntDataBox(101));
        assert(Math.abs(result[0] - 1.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 1.0) < 0.00001);
        }

        //Should return [0,0,0,0,0,0,0,0,0,0,0.0]
        result = h.filter(PredicateOperator.LESS_THAN, new IntDataBox(-1));
        assert(Math.abs(result[0] - 0.0) < 0.00001);
        for (int i = 1; i < 10; i++) {
            assert(Math.abs(result[i] - 0.0) < 0.00001);
        }

    }
}
