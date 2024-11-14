package edu.umd.cs424.database;

import java.util.Iterator;
import java.util.List;

import edu.umd.cs424.database.common.BacktrackingIterator;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.io.Page;
import edu.umd.cs424.database.io.PageAllocator.PageIterator;
import edu.umd.cs424.database.query.QueryPlan;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.RecordId;
import edu.umd.cs424.database.table.RecordIterator;
import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.table.stats.TableStats;

public abstract class BaseTransaction implements AutoCloseable {
    public abstract long getTransNum();
    public abstract boolean isActive();
    public abstract void end();

    /**
     * Create a new table in this database.
     *
     * @param s the table schema
     * @param tableName the name of the table
     * @throws DatabaseException
     */
    public abstract void createTable(Schema s, String tableName) throws DatabaseException;

    /**
     * Create a new table in this database with an index on each of the given column names.
     * @param s the table schema
     * @param tableName the name of the table
     * @param indexColumns the list of unique columnNames on the maintain an index on
     * @throws DatabaseException
     */
    public abstract void createTableWithIndices(Schema s, String tableName,
                                List<String> indexColumns) throws DatabaseException;

    /**
     * Delete a table in this database.
     *
     * @param tableName the name of the table
     * @return true if the database was successfully deleted
     */
    public abstract boolean deleteTable(String tableName);

    /**
     * Delete all tables from this database.
     */
    public abstract void deleteAllTables();

    /**
     * Allows the user to query a table. See query#QueryPlan
     *
     * @param tableName The name/alias of the table wished to be queried.
     * @throws DatabaseException if table does not exist
     */
    public abstract QueryPlan query(String tableName) throws DatabaseException;

    /**
     * Allows the user to provide an alias for a particular table. That alias is valid for the
     * remainder of the transaction. For a particular QueryPlan, once you specify an alias, you
     * must use that alias for the rest of the query.
     *
     * @param tableName The original name of the table.
     * @param alias The new Aliased name.
     * @throws DatabaseException if the alias already exists or the table does not.
     */
    public abstract void queryAs(String tableName, String alias) throws DatabaseException;

    /**
     * Create a temporary table within this transaction.
     *
     * @param schema the table schema
     * @throws DatabaseException
     * @return name of the tempTable
     */
    public abstract String createTempTable(Schema schema) throws DatabaseException;

    /**
     * Create a temporary table within this transaction.
     *
     * @param schema the table schema
     * @param tempTableName the name of the table
     * @throws DatabaseException
     */
    public abstract void createTempTable(Schema schema, String tempTableName) throws DatabaseException;

    /**
     * Perform a check to see if the database has an index on this (table,column).
     *
     * @param tableName the name of the table
     * @param columnName the name of the column
     * @return boolean if the index exists
     */
    public abstract boolean indexExists(String tableName, String columnName);

    public abstract Iterator<Record> sortedScan(String tableName, String columnName) throws DatabaseException;

    public abstract Iterator<Record> sortedScanFrom(String tableName, String columnName,
                                    DataBox startValue) throws DatabaseException;

    public abstract Iterator<Record> lookupKey(String tableName, String columnName,
                               DataBox key) throws DatabaseException;

    public abstract boolean contains(String tableName, String columnName, DataBox key) throws DatabaseException;

    public abstract RecordId addRecord(String tableName, List<DataBox> values) throws DatabaseException;

    public abstract int getNumMemoryPages() throws DatabaseException;

    public abstract RecordId deleteRecord(String tableName, RecordId rid)  throws DatabaseException;

    public abstract Record getRecord(String tableName, RecordId rid) throws DatabaseException;

    public abstract RecordIterator getRecordIterator(String tableName) throws DatabaseException;

    public abstract RecordId updateRecord(String tableName, List<DataBox> values,
                          RecordId rid)  throws DatabaseException;

    public abstract PageIterator getPageIterator(String tableName) throws DatabaseException;

    public abstract BacktrackingIterator<Record> getBlockIterator(String tableName,
            Page[] block) throws DatabaseException;

    public abstract BacktrackingIterator<Record> getBlockIterator(String tableName,
            BacktrackingIterator<Page> block) throws DatabaseException;

    public abstract BacktrackingIterator<Record> getBlockIterator(String tableName, Iterator<Page> block,
            int maxPages) throws DatabaseException;

    public abstract RecordId runUpdateRecordWhere(String tableName, String targetColumnName, DataBox targetVaue,
                                  String predColumnName, DataBox predValue)  throws DatabaseException;

    public abstract TableStats getStats(String tableName) throws DatabaseException;

    public abstract int getNumDataPages(String tableName) throws DatabaseException;

    public abstract int getNumEntriesPerPage(String tableName) throws DatabaseException;

    public abstract byte[] readPageHeader(String tableName, Page p) throws DatabaseException;

    public abstract int getPageHeaderSize(String tableName) throws DatabaseException;

    public abstract int getEntrySize(String tableName) throws DatabaseException;

    public abstract long getNumRecords(String tableName) throws DatabaseException;

    public abstract int getNumIndexPages(String tableName, String columnName) throws DatabaseException;

    public abstract Schema getSchema(String tableName) throws DatabaseException;

    public abstract Schema getFullyQualifiedSchema(String tableName) throws DatabaseException;

    public abstract void deleteTempTable(String tempTableName);

    public abstract void block();

    public abstract void unblock();

    public abstract boolean getBlocked();

    @Override
    public final void close() {
        end();
    }
}

