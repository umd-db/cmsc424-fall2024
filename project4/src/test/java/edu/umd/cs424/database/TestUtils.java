package edu.umd.cs424.database;

import edu.umd.cs424.database.databox.*;
import edu.umd.cs424.database.query.QueryPlanException;
import edu.umd.cs424.database.query.TestSourceOperator;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestUtils {

    public final static int LARGE_SCHEMA_NUM_COLUMNS = 120;
    public final static int LARGE_SCHEMA_ROWS_PER_PAGE = 8;

    public static Schema createLargeSchema() {
        // A very large schema with few rows per table
        var names = new ArrayList<String>();
        var types = new ArrayList<Type>();
        for (int i = 0; i < LARGE_SCHEMA_NUM_COLUMNS; i++) {
            names.add("column" + i);
            types.add(Type.intType());
        }
        return new Schema(names, types);
    }

    public static Record createLargeSchemaRecord(int val) {
        var values = new ArrayList<DataBox>();
        var box = new IntDataBox(val);
        for (int i = 0; i < LARGE_SCHEMA_NUM_COLUMNS; i++) {
            values.add(box);
        }
        return new Record(values);
    }

    public static List<Integer> largeSchemaRecordSimplify(List<Record> lst) {
        return lst.stream().map(r -> r.getValues().get(0).getInt()).toList();
    }

    public static <T> ArrayList<T> iteratorToList(Iterator<T> it) {
        ArrayList<T> res = new ArrayList<>();
        while (it.hasNext()) {
            res.add(it.next());
        }
        return res;
    }

    public static Schema createSchemaWithAllTypes() {
        List<String> names = Arrays.asList("bool", "int", "string", "float");
        List<Type> types = Arrays.asList(Type.boolType(), Type.intType(),
                                         Type.stringType(5), Type.floatType());
        return new Schema(names, types);
    }

    public static Schema createSchemaWithAllTypes(String prefix) {
        List<String> names = Arrays.asList(prefix + "bool", prefix + "int", prefix + "string",
                                           prefix + "float");
        List<Type> types = Arrays.asList(Type.boolType(), Type.intType(),
                                         Type.stringType(5), Type.floatType());
        return new Schema(names, types);
    }

    public static Schema createSchemaWithTwoInts() {
        List<Type> dataBoxes = new ArrayList<Type>();
        List<String> fieldNames = new ArrayList<String>();

        dataBoxes.add(Type.intType());
        dataBoxes.add(Type.intType());

        fieldNames.add("int1");
        fieldNames.add("int2");

        return new Schema(fieldNames, dataBoxes);
    }

    public static Schema createSchemaOfBool() {
        List<Type> dataBoxes = new ArrayList<Type>();
        List<String> fieldNames = new ArrayList<String>();

        dataBoxes.add(Type.boolType());

        fieldNames.add("bool");

        return new Schema(fieldNames, dataBoxes);
    }

    public static Schema createSchemaOfString(int len) {
        List<Type> dataBoxes = new ArrayList<Type>();
        List<String> fieldNames = new ArrayList<String>();

        dataBoxes.add(Type.stringType(len));
        fieldNames.add("string");

        return new Schema(fieldNames, dataBoxes);
    }

    public static Record createRecordWithAllTypes() {
        List<DataBox> dataValues = new ArrayList<DataBox>();
        dataValues.add(new BoolDataBox(true));
        dataValues.add(new IntDataBox(1));
        dataValues.add(new StringDataBox("abcde", 5));
        dataValues.add(new FloatDataBox((float) 1.2));

        return new Record(dataValues);
    }

    public static Record createRecordWithAllTypesWithValue(int val) {
        List<DataBox> dataValues = new ArrayList<DataBox>();
        dataValues.add(new BoolDataBox(true));
        dataValues.add(new IntDataBox(val));
        dataValues.add(new StringDataBox(String.format("%05d", val), 5));
        dataValues.add(new FloatDataBox((float) val));
        return new Record(dataValues);
    }

    public static TestSourceOperator createTestSourceOperatorWithInts(List<Integer> values)
    throws QueryPlanException {
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("int");
        List<Type> columnTypes = new ArrayList<Type>();
        columnTypes.add(Type.intType());
        Schema schema = new Schema(columnNames, columnTypes);

        List<Record> recordList = new ArrayList<Record>();

        for (int v : values) {
            List<DataBox> recordValues = new ArrayList<DataBox>();
            recordValues.add(new IntDataBox(v));
            recordList.add(new Record(recordValues));
        }

        return new TestSourceOperator(recordList, schema);
    }

    public static TestSourceOperator createTestSourceOperatorWithFloats(List<Float> values)
    throws QueryPlanException {
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("float");
        List<Type> columnTypes = new ArrayList<Type>();
        columnTypes.add(Type.floatType());
        Schema schema = new Schema(columnNames, columnTypes);

        List<Record> recordList = new ArrayList<Record>();

        for (float v : values) {
            List<DataBox> recordValues = new ArrayList<DataBox>();
            recordValues.add(new FloatDataBox(v));
            recordList.add(new Record(recordValues));
        }

        return new TestSourceOperator(recordList, schema);
    }
}
