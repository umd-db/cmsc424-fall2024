package edu.umd.cs424.database.query;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.Schema;

import java.util.Iterator;
import java.util.List;

/**
 * This class defines a run in sort merge and hash join. Do not instantiate this class directly. Instead, use the
 * <code>createRun</code> method provided to you.
 */
public class Run {
    private final Database.Transaction transaction;
    private final String tempTableName;
    private int size = 0;

    public Run(Database.Transaction transaction, Schema tempSchema) throws DatabaseException {
        this.transaction = transaction;
        this.tempTableName = transaction.createTempTable(tempSchema);
    }

    public void addRecord(List<DataBox> values) throws DatabaseException {
        transaction.addRecord(this.tempTableName, values);
        size++;
    }

    public void addRecords(List<edu.umd.cs424.database.table.Record> records) throws DatabaseException {
        for (edu.umd.cs424.database.table.Record r : records) {
            this.addRecord(r.getValues());
        }
    }

    public Iterator<Record> iterator() {
        try {
            return transaction.getRecordIterator(this.tempTableName);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public String tableName() {
        return this.tempTableName;
    }

    public int getSize() {
        return size;
    }
}
