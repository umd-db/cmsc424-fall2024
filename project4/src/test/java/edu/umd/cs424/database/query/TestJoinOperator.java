package edu.umd.cs424.database.query;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.TimeoutScaling;
import edu.umd.cs424.database.categories.*;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.TestUtils;
import edu.umd.cs424.database.databox.BoolDataBox;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.FloatDataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.StringDataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.Schema;

import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static edu.umd.cs424.database.TestUtils.iteratorToList;
import static edu.umd.cs424.database.TestUtils.largeSchemaRecordSimplify;
import static org.junit.Assert.*;

@Category(ProjTests.class)
public class TestJoinOperator {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // 10 second max per method tested.
    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (10000 * TimeoutScaling.factor)));

    Database.Transaction newTransaction() {
        return newTransaction(5);
    }

    Database.Transaction newTransaction(int numMemoryPages) {
        try {
            File tempDir = tempFolder.newFolder("joinTest");
            return new Database(tempDir.getAbsolutePath(), numMemoryPages).beginTransaction();
        } catch (Exception e) {
            fail("An exception occurred. " + e.getMessage());
        }
        // unreachable
        return null;
    }

    @Test
    @Category(PublicTests.class)
    public void testOperatorSchema() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator sourceOperator = new TestSourceOperator();
        var transaction = newTransaction();
        JoinOperator joinOperator = new SNLJOperator(sourceOperator, sourceOperator, "int", "int",
                transaction);

        List<String> expectedSchemaNames = new ArrayList<String>();
        expectedSchemaNames.add("bool");
        expectedSchemaNames.add("int");
        expectedSchemaNames.add("string");
        expectedSchemaNames.add("float");
        expectedSchemaNames.add("bool");
        expectedSchemaNames.add("int");
        expectedSchemaNames.add("string");
        expectedSchemaNames.add("float");

        List<Type> expectedSchemaTypes = new ArrayList<Type>();
        expectedSchemaTypes.add(Type.boolType());
        expectedSchemaTypes.add(Type.intType());
        expectedSchemaTypes.add(Type.stringType(5));
        expectedSchemaTypes.add(Type.floatType());
        expectedSchemaTypes.add(Type.boolType());
        expectedSchemaTypes.add(Type.intType());
        expectedSchemaTypes.add(Type.stringType(5));
        expectedSchemaTypes.add(Type.floatType());

        Schema expectedSchema = new Schema(expectedSchemaNames, expectedSchemaTypes);

        assertEquals(expectedSchema, joinOperator.getOutputSchema());
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleJoin() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator sourceOperator = new TestSourceOperator();
        var transaction = newTransaction();
        JoinOperator joinOperator = new SNLJOperator(sourceOperator, sourceOperator, "int", "int",
                transaction);

        Iterator<Record> outputIterator = joinOperator.iterator();
        int numRecords = 0;

        List<DataBox> expectedRecordValues = new ArrayList<DataBox>();
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        Record expectedRecord = new Record(expectedRecordValues);

        while (outputIterator.hasNext()) {
            assertEquals(expectedRecord, outputIterator.next());
            numRecords++;
        }

        assertEquals(100 * 100, numRecords);
    }

    @Test
    @Category(PublicTests.class)
    public void testEmptyJoin() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator leftSourceOperator = new TestSourceOperator();

        List<Integer> values = new ArrayList<Integer>();
        TestSourceOperator rightSourceOperator = TestUtils.createTestSourceOperatorWithInts(values);
        var transaction = newTransaction();
        JoinOperator joinOperator = new SNLJOperator(leftSourceOperator, rightSourceOperator, "int", "int",
                transaction);
        Iterator<Record> outputIterator = joinOperator.iterator();

        assertFalse(outputIterator.hasNext());
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleJoinBNLJ() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator sourceOperator = new TestSourceOperator();
        var transaction = newTransaction();
        JoinOperator joinOperator = new BNLJOperator(sourceOperator, sourceOperator, "int", "int",
                transaction);

        Iterator<Record> outputIterator = joinOperator.iterator();
        int numRecords = 0;

        List<DataBox> expectedRecordValues = new ArrayList<DataBox>();
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        Record expectedRecord = new Record(expectedRecordValues);

        while (outputIterator.hasNext()) {
            assertEquals(expectedRecord, outputIterator.next());
            numRecords++;
        }

        assertEquals(100 * 100, numRecords);
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleJoinBNLJOptimized() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator sourceOperator = new TestSourceOperator();
        var transaction = newTransaction();
        JoinOperator joinOperator = new BNLJOptimizedOperator(sourceOperator, sourceOperator, "int", "int",
                transaction);

        Iterator<Record> outputIterator = joinOperator.iterator();
        int numRecords = 0;

        List<DataBox> expectedRecordValues = new ArrayList<DataBox>();
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        Record expectedRecord = new Record(expectedRecordValues);

        while (outputIterator.hasNext()) {
            assertEquals(expectedRecord, outputIterator.next());
            numRecords++;
        }

        assertEquals(100 * 100, numRecords);
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleBNLJ_simple() throws DatabaseException, QueryPlanException {
        String leftTableName = "left", rightTableName = "right";
        var transaction = newTransaction();
        Schema schema = TestUtils.createLargeSchema();
        Record r1 = TestUtils.createLargeSchemaRecord(1);
        Record r2 = TestUtils.createLargeSchemaRecord(2);
        List<DataBox> r1Values = r1.getValues(), r2Values = r2.getValues();
        transaction.createTable(schema, leftTableName);
        transaction.createTable(schema, rightTableName);

        int rowsPerPage = TestUtils.LARGE_SCHEMA_ROWS_PER_PAGE;

        // Each table has 2 pages of values: half a page of r1, half a page of r2, half a page of r2, half a page of r1.
        for (var values : List.of(r1Values, r2Values, r2Values, r1Values)) {
            for (int i = 0; i < rowsPerPage / 2; i++) {
                transaction.addRecord(leftTableName, values);
                transaction.addRecord(rightTableName, values);
            }
        }

        var joinedR1 = new Record(Stream.concat(r1.getValues().stream(), r1.getValues().stream()).toList());
        var joinedR2 = new Record(Stream.concat(r2.getValues().stream(), r2.getValues().stream()).toList());
        var expectedPattern = List.of(
                joinedR1, joinedR2, joinedR1, joinedR2,
                joinedR2, joinedR1, joinedR2, joinedR1);
        var expected = new ArrayList<Record>();
        for (Record r : expectedPattern) {
            for (int i = 0; i < rowsPerPage * rowsPerPage / 4; i++) {
                expected.add(r);
            }
        }

        QueryOperator s1 = new SequentialScanOperator(transaction, leftTableName);
        QueryOperator s2 = new SequentialScanOperator(transaction, rightTableName);
        QueryOperator joinOperator = new BNLJOperator(s1, s2, "column5", "column100", transaction);

        var actual = iteratorToList(joinOperator.iterator());

        assertEquals(
                largeSchemaRecordSimplify(expected),
                largeSchemaRecordSimplify(actual));
        assertEquals(expected, actual);

        transaction.close();
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleBNLJOutputOrder() throws QueryPlanException, DatabaseException, IOException {
        var transaction = newTransaction();
        Record r1 = TestUtils.createRecordWithAllTypesWithValue(1);
        List<DataBox> r1Vals = r1.getValues();
        Record r2 = TestUtils.createRecordWithAllTypesWithValue(2);
        List<DataBox> r2Vals = r2.getValues();

        var expectedRecordValues1 = new ArrayList<DataBox>();
        var expectedRecordValues2 = new ArrayList<DataBox>();
        for (int i = 0; i < 2; i++) {
            expectedRecordValues1.addAll(r1Vals);
            expectedRecordValues2.addAll(r2Vals);
        }

        Record expectedRecord1 = new Record(expectedRecordValues1);
        Record expectedRecord2 = new Record(expectedRecordValues2);
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "leftTable");
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "rightTable");

        int size = 144;
        for (int i = 0; i < size * 2; i++) {
            List<DataBox> vals;
            if (i < size) {
                vals = r1Vals;
            } else {
                vals = r2Vals;
            }
            transaction.addRecord("leftTable", vals);
            transaction.addRecord("rightTable", vals);
        }

        for (int i = 0; i < size * 2; i++) {
            if (i < size) {
                transaction.addRecord("leftTable", r2Vals);
                transaction.addRecord("rightTable", r1Vals);
            } else {
                transaction.addRecord("leftTable", r1Vals);
                transaction.addRecord("rightTable", r2Vals);
            }
        }

        QueryOperator s1 = new SequentialScanOperator(transaction, "leftTable");
        QueryOperator s2 = new SequentialScanOperator(transaction, "rightTable");
        QueryOperator joinOperator = new BNLJOperator(s1, s2, "int", "int", transaction);

        int count = 0;
        Iterator<Record> outputIterator = joinOperator.iterator();

        while (outputIterator.hasNext()) {
            if (count < size * size) {
                assertEquals(expectedRecord1, outputIterator.next());
            } else if (count < size * size * 2) {
                assertEquals(expectedRecord2, outputIterator.next());
            } else if (count < size * size * 3) {
                assertEquals(expectedRecord1, outputIterator.next());
            } else if (count < size * size * 4) {
                assertEquals(expectedRecord2, outputIterator.next());
            } else if (count < size * size * 5) {
                assertEquals(expectedRecord2, outputIterator.next());
            } else if (count < size * size * 6) {
                assertEquals(expectedRecord1, outputIterator.next());
            } else if (count < size * size * 7) {
                assertEquals(expectedRecord2, outputIterator.next());
            } else {
                assertEquals(expectedRecord1, outputIterator.next());
            }
            count++;
        }

        assertEquals(count, size * size * 8);
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleSortMergeJoin() throws QueryPlanException, DatabaseException, IOException {
        TestSourceOperator sourceOperator = new TestSourceOperator();
        var transaction = newTransaction();
        JoinOperator joinOperator = new SortMergeOperator(sourceOperator, sourceOperator, "int", "int",
                transaction);

        Iterator<Record> outputIterator = joinOperator.iterator();
        int numRecords = 0;

        List<DataBox> expectedRecordValues = new ArrayList<DataBox>();
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        expectedRecordValues.add(new BoolDataBox(true));
        expectedRecordValues.add(new IntDataBox(1));
        expectedRecordValues.add(new StringDataBox("abcde", 5));
        expectedRecordValues.add(new FloatDataBox(1.2f));
        Record expectedRecord = new Record(expectedRecordValues);

        while (outputIterator.hasNext()) {
            assertEquals(expectedRecord, outputIterator.next());
            numRecords++;
        }

        assertEquals(100 * 100, numRecords);
    }

    @Test
    @Category(PublicTests.class)
    public void testSortMergeJoinUnsortedInputs() throws QueryPlanException, DatabaseException,
        IOException {
        var transaction = newTransaction(3);
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "leftTable");
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "rightTable");
        Record r1 = TestUtils.createRecordWithAllTypesWithValue(1);
        List<DataBox> r1Vals = r1.getValues();
        Record r2 = TestUtils.createRecordWithAllTypesWithValue(2);
        List<DataBox> r2Vals = r2.getValues();
        Record r3 = TestUtils.createRecordWithAllTypesWithValue(3);
        List<DataBox> r3Vals = r3.getValues();
        Record r4 = TestUtils.createRecordWithAllTypesWithValue(4);
        List<DataBox> r4Vals = r4.getValues();
        List<DataBox> expectedRecordValues1 = new ArrayList<DataBox>();
        List<DataBox> expectedRecordValues2 = new ArrayList<DataBox>();
        List<DataBox> expectedRecordValues3 = new ArrayList<DataBox>();
        List<DataBox> expectedRecordValues4 = new ArrayList<DataBox>();

        for (int i = 0; i < 2; i++) {
            expectedRecordValues1.addAll(r1Vals);
            expectedRecordValues2.addAll(r2Vals);
            expectedRecordValues3.addAll(r3Vals);
            expectedRecordValues4.addAll(r4Vals);
        }
        Record expectedRecord1 = new Record(expectedRecordValues1);
        Record expectedRecord2 = new Record(expectedRecordValues2);
        Record expectedRecord3 = new Record(expectedRecordValues3);
        Record expectedRecord4 = new Record(expectedRecordValues4);
        List<Record> leftTableRecords = new ArrayList<>();
        List<Record> rightTableRecords = new ArrayList<>();
        for (int i = 0; i < 288 * 2; i++) {
            Record r;
            if (i % 4 == 0) {
                r = r1;
            } else if (i % 4  == 1) {
                r = r2;
            } else if (i % 4  == 2) {
                r = r3;
            } else {
                r = r4;
            }
            leftTableRecords.add(r);
            rightTableRecords.add(r);
        }
        Collections.shuffle(leftTableRecords, new Random(10));
        Collections.shuffle(rightTableRecords, new Random(20));
        for (int i = 0; i < 288 * 2; i++) {
            transaction.addRecord("leftTable", leftTableRecords.get(i).getValues());
            transaction.addRecord("rightTable", rightTableRecords.get(i).getValues());
        }

        QueryOperator s1 = new SequentialScanOperator(transaction, "leftTable");
        QueryOperator s2 = new SequentialScanOperator(transaction, "rightTable");

        JoinOperator joinOperator = new SortMergeOperator(s1, s2, "int", "int", transaction);

        Iterator<Record> outputIterator = joinOperator.iterator();
        int numRecords = 0;
        Record expectedRecord;

        while (outputIterator.hasNext()) {
            if (numRecords < (288 * 288 / 4)) {
                expectedRecord = expectedRecord1;
            } else if (numRecords < (288 * 288 / 2)) {
                expectedRecord = expectedRecord2;
            } else if (numRecords < 288 * 288 - (288 * 288 / 4)) {
                expectedRecord = expectedRecord3;
            } else {
                expectedRecord = expectedRecord4;
            }
            Record r = outputIterator.next();
            assertEquals(r, expectedRecord);
            numRecords++;
        }

        assertEquals(288 * 288, numRecords);
    }

    @Test
    @Category(PublicTests.class)
    public void testBNLJOptimizedDiffOutPutThanBNLJ() throws QueryPlanException, DatabaseException, IOException {
        var transaction = newTransaction(4);
        Record r1 = TestUtils.createRecordWithAllTypesWithValue(1);
        List<DataBox> r1Vals = r1.getValues();
        Record r2 = TestUtils.createRecordWithAllTypesWithValue(2);
        List<DataBox> r2Vals = r2.getValues();
        Record r3 = TestUtils.createRecordWithAllTypesWithValue(3);
        List<DataBox> r3Vals = r3.getValues();
        Record r4 = TestUtils.createRecordWithAllTypesWithValue(4);
        List<DataBox> r4Vals = r4.getValues();
        var expectedRecordValues1 = new ArrayList<DataBox>();
        var expectedRecordValues2 = new ArrayList<DataBox>();
        var expectedRecordValues3 = new ArrayList<DataBox>();
        var expectedRecordValues4 = new ArrayList<DataBox>();

        for (int i = 0; i < 2; i++) {
            expectedRecordValues1.addAll(r1Vals);
            expectedRecordValues2.addAll(r2Vals);
            expectedRecordValues3.addAll(r3Vals);
            expectedRecordValues4.addAll(r4Vals);
        }
        Record expectedRecord1 = new Record(expectedRecordValues1);
        Record expectedRecord2 = new Record(expectedRecordValues2);
        Record expectedRecord3 = new Record(expectedRecordValues3);
        Record expectedRecord4 = new Record(expectedRecordValues4);
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "leftTable");
        transaction.createTable(TestUtils.createSchemaWithAllTypes(), "rightTable");
        for (int i = 0; i < 2 * 288; i++) {
            if (i < 144) {
                transaction.addRecord("leftTable", r1Vals);
                transaction.addRecord("rightTable", r3Vals);
            } else if (i < 288) {
                transaction.addRecord("leftTable", r2Vals);
                transaction.addRecord("rightTable", r4Vals);
            } else if (i < 432) {
                transaction.addRecord("leftTable", r3Vals);
                transaction.addRecord("rightTable", r1Vals);
            } else {
                transaction.addRecord("leftTable", r4Vals);
                transaction.addRecord("rightTable", r2Vals);
            }
        }
        QueryOperator s1 = new SequentialScanOperator(transaction, "leftTable");
        QueryOperator s2 = new SequentialScanOperator(transaction, "rightTable");
        QueryOperator joinOperator = new BNLJOptimizedOperator(s1, s2, "int", "int", transaction);
        Iterator<Record> outputIterator = joinOperator.iterator();
        int count = 0;
        while (outputIterator.hasNext()) {
            Record r = outputIterator.next();
            if (count < 144 * 144) {
                assertEquals(expectedRecord3, r);
            } else if (count < 2 * 144 * 144) {
                assertEquals(expectedRecord4, r);
            } else if (count < 3 * 144 * 144) {
                assertEquals(expectedRecord1, r);
            } else {
                assertEquals(expectedRecord2, r);
            }
            count++;
        }
        assertEquals(82944, count);
    }
}
