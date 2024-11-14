package edu.umd.cs424.database;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs424.database.common.BacktrackingIterator;
import edu.umd.cs424.database.common.Pair;
import edu.umd.cs424.database.concurrency.*;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.index.BPlusTree;
import edu.umd.cs424.database.index.BPlusTreeException;
import edu.umd.cs424.database.io.Page;
import edu.umd.cs424.database.query.QueryPlan;
import edu.umd.cs424.database.query.QueryPlanException;
import edu.umd.cs424.database.query.SortOperator;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.RecordId;
import edu.umd.cs424.database.table.RecordIterator;
import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.table.Table;
import edu.umd.cs424.database.table.stats.TableStats;
import edu.umd.cs424.database.io.PageAllocator.PageIterator;

public class Database {
    private Map<String, Table> tableLookup;
    private Map<String, BPlusTree> indexLookup;
    private Map<String, List<String>> tableIndices;
    private Map<Long, Transaction> activeTransactions;
    private long numTransactions;
    private String fileDir;
    private LockManager lockManager;
    private int numMemoryPages;

    /**
     * Creates a new database with locking disabled.
     *
     * @param fileDir the directory to put the table files in
     * @throws DatabaseException
     */
    public Database(String fileDir) throws DatabaseException {
        this (fileDir, 5);
    }

    /**
     * Creates a new database with locking disabled.
     *
     * @param fileDir the directory to put the table files in
     * @param numMemoryPages the number of pages of memory Database Operations should use when executing Queries
     * @throws DatabaseException
     */
    public Database(String fileDir, int numMemoryPages) throws DatabaseException {
        this(fileDir, numMemoryPages, new DummyLockManager());
    }

    /**
     * Creates a new database.
     *
     * @param fileDir the directory to put the table files in
     * @param numMemoryPages the number of pages of memory Database Operations should use when executing Queries
     * @param lockManager the lock manager
     * @throws DatabaseException
     */
    public Database(String fileDir, int numMemoryPages, LockManager lockManager)
    throws DatabaseException {
        this.numMemoryPages = numMemoryPages;
        this.fileDir = fileDir;
        numTransactions = 0;
        tableLookup = new ConcurrentHashMap<>();
        indexLookup = new ConcurrentHashMap<>();
        tableIndices = new ConcurrentHashMap<>();
        activeTransactions = new ConcurrentHashMap<>();

        File dir = new File(fileDir);
        this.lockManager = lockManager;

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles();

        try (Transaction initTransaction = new Transaction(-1)) {
            LockContext lockContext = lockManager.databaseContext();
            lockContext.acquire(initTransaction, LockType.X);
            for (File f : files) {
                String fName = f.getName();
                if (fName.endsWith(Table.FILENAME_EXTENSION)) {
                    int lastIndex = fName.lastIndexOf(Table.FILENAME_EXTENSION);
                    String tableName = fName.substring(0, lastIndex);
                    tableLookup.put(tableName, newTable(tableName, f.toPath().toString(),
                            lockContext.childContext("table-" + tableName), initTransaction));
                    if (!tableIndices.containsKey(tableName)) {
                        tableIndices.put(tableName, new ArrayList<>());
                    }
                } else if (fName.endsWith(BPlusTree.FILENAME_EXTENSION)) {
                    int lastIndex = fName.lastIndexOf(BPlusTree.FILENAME_EXTENSION);
                    String indexName = fName.substring(0, lastIndex);
                    String tableName = indexName.split(",", 2)[0];
                    indexLookup.put(indexName, new BPlusTree(f.toString(), getIndexContext(indexName),
                            initTransaction));
                    if (!tableIndices.containsKey(tableName)) {
                        tableIndices.put(tableName, new ArrayList<>());
                    }
                    tableIndices.get(tableName).add(indexName);
                }
            }
        }
    }

    /**
     * Close this database.
     */
    public synchronized void close() {
        try (Transaction closeTransaction = new Transaction(-2)) {
            lockManager.databaseContext().acquire(closeTransaction, LockType.X);

            for (Table t : this.tableLookup.values()) {
                t.close();
            }

            for (BPlusTree t : this.indexLookup.values()) {
                t.close();
            }

            this.tableLookup.clear();
            this.indexLookup.clear();
            this.tableIndices.clear();
        }
    }

    public Table getTable(String tableName) {
        return tableLookup.get(tableName);
    }

    private LockContext getTableContext(String table) {
        return lockManager.databaseContext().childContext("table-" + table);
    }

    private LockContext getIndexContext(String index) {
        return lockManager.databaseContext().childContext("index-" + index);
    }

    /**
     * Start a new transaction.
     *
     * @return the new Transaction
     */
    public synchronized Transaction beginTransaction() {
        Transaction t = new Transaction(this.numTransactions);
        this.activeTransactions.put(this.numTransactions, t);
        this.numTransactions++;
        return t;
    }

    /**
     * This transaction implementation assumes that exactly one transaction runs
     * on a thread at a time, and that, aside from the unblock() method, no methods
     * of the transaction are called from a different thread than the thread that the
     * transaction is associated with. This implementation blocks the thread when
     * block() is called.
     */
    public class Transaction extends BaseTransaction {
        long transNum;
        boolean active;
        boolean blocked;
        HashMap<String, Table> tempTables;
        HashMap<String, String> aliasMaps;
        long tempTableCounter;

        final ReentrantLock transactionLock = new ReentrantLock();
        final Condition unblocked = transactionLock.newCondition();

        protected Transaction(long tNum) {
            this.transNum = tNum;
            this.active = true;
            this.blocked = false;
            this.tempTables = new HashMap<String, Table>();
            this.aliasMaps = new HashMap<String, String>();
            this.tempTableCounter = 0;
        }

        public long getTransNum() {
            return this.transNum;
        }

        public boolean isActive() {
            return this.active;
        }

        public void end() {
            assert(this.active);

            deleteAllTempTables();
            this.active = false;
            Database.this.activeTransactions.remove(this.transNum);
        }

        /**
         * Create a new table in this database.
         *
         * @param s the table schema
         * @param tableName the name of the table
         * @throws DatabaseException
         */
        public void createTable(Schema s, String tableName) throws DatabaseException {
            LockContext tableContext = getTableContext(tableName);

            if (Database.this.tableLookup.containsKey(tableName)) {
                throw new DatabaseException("Table name already exists");
            }

            Path path = Paths.get(fileDir, tableName + Table.FILENAME_EXTENSION);
            Database.this.tableLookup.put(tableName, newTable(tableName, s, path.toString(), tableContext,
                                          this));
            Database.this.tableIndices.put(tableName, new ArrayList<>());
        }

        /**
         * Create a new table in this database with an index on each of the given column names.
         * @param s the table schema
         * @param tableName the name of the table
         * @param indexColumns the list of unique columnNames on the maintain an index on
         * @throws DatabaseException
         */
        public void createTableWithIndices(Schema s, String tableName,
                                           List<String> indexColumns) throws DatabaseException {
            LockContext tableContext = getTableContext(tableName);

            List<String> schemaColNames = s.getFieldNames();
            List<Type> schemaColType = s.getFieldTypes();

            HashSet<String> seenColNames = new HashSet<String>();
            List<Integer> schemaColIndex = new ArrayList<Integer>();
            for (int i = 0; i < indexColumns.size(); i++) {
                String col = indexColumns.get(i);
                if (!schemaColNames.contains(col)) {
                    throw new DatabaseException("Column desired for index does not exist");
                }
                if (seenColNames.contains(col)) {
                    throw new DatabaseException("Column desired for index has been duplicated");
                }
                seenColNames.add(col);
                schemaColIndex.add(schemaColNames.indexOf(col));
            }

            if (Database.this.tableLookup.containsKey(tableName)) {
                throw new DatabaseException("Table name already exists");
            }

            Path path = Paths.get(fileDir, tableName + Table.FILENAME_EXTENSION);
            Database.this.tableLookup.put(tableName, newTable(tableName, s, path.toString(), tableContext,
                                          this));
            Database.this.tableIndices.put(tableName, new ArrayList<>());
            for (int i : schemaColIndex) {
                String colName = schemaColNames.get(i);
                Type colType = schemaColType.get(i);
                String indexName = tableName + "," + colName;
                Path p = Paths.get(Database.this.fileDir, indexName + BPlusTree.FILENAME_EXTENSION);
                LockContext indexContext = getIndexContext(indexName);
                try {
                    Database.this.indexLookup.put(indexName, new BPlusTree(p.toString(), colType,
                                                  BPlusTree.maxOrder(Page.pageSize, colType), indexContext, this));
                    Database.this.tableIndices.get(tableName).add(indexName);
                } catch (BPlusTreeException e) {
                    throw new DatabaseException(e.getMessage());
                }
            }
        }

        /**
         * Delete a table in this database.
         *
         * @param tableName the name of the table
         * @return true if the database was successfully deleted
         */
        public boolean deleteTable(String tableName) {
            if (!Database.this.tableLookup.containsKey(tableName)) {
                return false;
            }

            Database.this.tableLookup.get(tableName).close();
            Database.this.tableLookup.remove(tableName);

            File f = new File(fileDir + tableName + Table.FILENAME_EXTENSION);
            f.delete();

            Iterator<String> indices = Database.this.tableIndices.get(tableName).iterator();
            while (indices.hasNext()) {
                String indexName = indices.next();
                indices.remove();
                Database.this.indexLookup.get(indexName).close();
                Database.this.indexLookup.remove(indexName);

                File indexFile = new File(fileDir + indexName + BPlusTree.FILENAME_EXTENSION);
                indexFile.delete();
            }
            Database.this.tableIndices.remove(tableName);

            return true;
        }

        /**
         * Delete all tables from this database.
         */
        public void deleteAllTables() {
            List<String> tableNames = new ArrayList<>(tableLookup.keySet());

            for (String s : tableNames) {
                deleteTable(s);
            }
        }

        public QueryPlan query(String tableName) throws DatabaseException {
            assert(this.active);
            return new QueryPlan(this, tableName);
        }

        public void queryAs(String tableName, String alias) throws DatabaseException {
            assert(this.active);

            if (Database.this.tableLookup.containsKey(alias)
                    || this.tempTables.containsKey(alias)
                    || this.aliasMaps.containsKey(alias)) {
                throw new DatabaseException("Table name already exists");
            }

            if (Database.this.tableLookup.containsKey(tableName)) {
                this.aliasMaps.put(alias, tableName);
            } else if (tempTables.containsKey(tableName)) {
                this.aliasMaps.put(alias, tableName);
            } else {
                throw new DatabaseException("Table name not found");
            }
        }

        public String createTempTable(Schema schema) throws DatabaseException {
            assert(this.active);
            String tempTableName = "tempTable" + tempTableCounter;
            tempTableCounter++;
            createTempTable(schema, tempTableName);
            return tempTableName;
        }

        public void createTempTable(Schema schema, String tempTableName) throws DatabaseException {
            assert(this.active);

            if (Database.this.tableLookup.containsKey(tempTableName)
                    || this.tempTables.containsKey(tempTableName))  {
                throw new DatabaseException("Table name already exists");
            }

            Path dir = Paths.get(Database.this.fileDir, "temp");
            File f = new File(dir.toAbsolutePath().toString());
            if (!f.exists()) {
                f.mkdirs();
            }

            Path path = Paths.get(Database.this.fileDir, "temp", tempTableName + Table.FILENAME_EXTENSION);
            LockContext lockContext = lockManager.orphanContext("temp-" + tempTableName);
            this.tempTables.put(tempTableName, newTable(tempTableName, schema, path.toString(), lockContext,
                                this));
        }

        public boolean indexExists(String tableName, String columnName) {
            try {
                resolveIndexFromName(tableName, columnName);
            } catch (DatabaseException e) {
                return false;
            }
            return true;
        }

        public Iterator<Record> sortedScan(String tableName, String columnName) throws DatabaseException {
            Table tab = getTable(tableName);
            try {
                Pair<String, BPlusTree> index = resolveIndexFromName(tableName, columnName);
                return new RecordIterator(this, tab, index.getSecond().scanAll(this));
            } catch (DatabaseException e1) {
                int offset = getTable(tableName).getSchema().getFieldNames().indexOf(columnName);
                try {
                    return new SortOperator(this, tableName,
                                            Comparator.comparing((Record r) -> r.getValues().get(offset))).iterator();
                } catch (QueryPlanException e2) {
                    throw new DatabaseException(e2);
                }
            }
        }

        public Iterator<Record> sortedScanFrom(String tableName, String columnName,
                                               DataBox startValue) throws DatabaseException {
            Table tab = getTable(tableName);
            Pair<String, BPlusTree> index = resolveIndexFromName(tableName, columnName);
            return new RecordIterator(this, tab, index.getSecond().scanGreaterEqual(this, startValue));
        }

        public Iterator<Record> lookupKey(String tableName, String columnName,
                                          DataBox key) throws DatabaseException {
            Table tab = getTable(tableName);
            Pair<String, BPlusTree> index = resolveIndexFromName(tableName, columnName);
            return new RecordIterator(this, tab, index.getSecond().scanEqual(this, key));
        }

        public boolean contains(String tableName, String columnName, DataBox key) throws DatabaseException {
            Pair<String, BPlusTree> index = resolveIndexFromName(tableName, columnName);
            return index.getSecond().get(this, key).isPresent();
        }

        public RecordId addRecord(String tableName, List<DataBox> values) throws DatabaseException {
            assert(this.active);

            Table tab = getTable(tableName);
            RecordId rid = tab.addRecord(this, values);
            Schema s = tab.getSchema();
            List<String> colNames = s.getFieldNames();

            for (int i = 0; i < colNames.size(); i++) {
                String col = colNames.get(i);
                if (indexExists(tableName, col)) {
                    try {
                        resolveIndexFromName(tableName, col).getSecond().put(this, values.get(i), rid);
                    } catch (BPlusTreeException e) {
                        throw new DatabaseException(e.getMessage());
                    }
                }
            }
            return rid;
        }

        public int getNumMemoryPages() throws DatabaseException {
            assert(this.active);
            return Database.this.numMemoryPages;
        }

        public RecordId deleteRecord(String tableName, RecordId rid)  throws DatabaseException {
            assert(this.active);

            Table tab = getTable(tableName);
            Schema s = tab.getSchema();

            Record rec = tab.deleteRecord(this, rid);
            List<DataBox> values = rec.getValues();
            List<String> colNames = s.getFieldNames();
            for (int i = 0; i < colNames.size(); i++) {
                String col = colNames.get(i);
                if (indexExists(tableName, col)) {
                    resolveIndexFromName(tableName, col).getSecond().remove(this, values.get(i));
                }
            }

            return rid;
        }

        public Record getRecord(String tableName, RecordId rid) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getRecord(this, rid);
        }

        public RecordIterator getRecordIterator(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).iterator(this);
        }

        public RecordId updateRecord(String tableName, List<DataBox> values,
                                     RecordId rid)  throws DatabaseException {
            return runUpdateRecord(tableName, values, rid);
        }

        public PageIterator getPageIterator(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getAllocator().iterator(this);
        }

        public BacktrackingIterator<Record> getBlockIterator(String tableName,
                Page[] block) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).blockIterator(this, block);
        }

        public BacktrackingIterator<Record> getBlockIterator(String tableName,
                BacktrackingIterator<Page> block) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).blockIterator(this, block);
        }

        public BacktrackingIterator<Record> getBlockIterator(String tableName, Iterator<Page> block,
                int maxPages) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).blockIterator(this, block, maxPages);
        }

        public RecordId runUpdateRecordWhere(String tableName, String targetColumnName, DataBox targetVaue,
                                             String predColumnName, DataBox predValue)  throws DatabaseException {
            Table tab = getTable(tableName);
            Iterator<RecordId> recordIds = tab.ridIterator(this);

            Schema s = tab.getSchema();
            int uindex = s.getFieldNames().indexOf(targetColumnName);
            int pindex = s.getFieldNames().indexOf(predColumnName);

            while(recordIds.hasNext()) {
                RecordId curRID = recordIds.next();
                Record cur = getRecord(tableName, curRID);
                List<DataBox> record_copy = new ArrayList<DataBox>(cur.getValues());

                if (record_copy.get(pindex).equals(predValue)) {
                    record_copy.set(uindex, targetVaue);
                    runUpdateRecord(tableName, record_copy, curRID);
                }
            }
            return null;
        }

        private RecordId runUpdateRecord(String tableName, List<DataBox> values,
                                         RecordId rid) throws DatabaseException {
            assert(this.active);
            Table tab = getTable(tableName);
            Schema s = tab.getSchema();

            Record rec = tab.updateRecord(this, values, rid);

            List<DataBox> oldValues = rec.getValues();
            List<String> colNames = s.getFieldNames();

            for (int i = 0; i < colNames.size(); i++) {
                String col = colNames.get(i);
                if (indexExists(tableName, col)) {
                    BPlusTree tree = resolveIndexFromName(tableName, col).getSecond();
                    tree.remove(this, oldValues.get(i));
                    try {
                        tree.put(this, values.get(i), rid);
                    } catch (BPlusTreeException e) {
                        throw new DatabaseException(e.getMessage());
                    }
                }
            }

            return rid;
        }

        public TableStats getStats(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getStats();
        }

        public int getNumDataPages(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getNumDataPages();
        }

        public int getNumEntriesPerPage(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getNumRecordsPerPage();
        }

        public byte[] readPageHeader(String tableName, Page p) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getBitMap(this, p);
        }

        public int getPageHeaderSize(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getBitmapSizeInBytes();
        }

        public int getEntrySize(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getSchema().getSizeInBytes();
        }

        public long getNumRecords(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getNumRecords();
        }

        public int getNumIndexPages(String tableName, String columnName) throws DatabaseException {
            assert(this.active);
            return this.resolveIndexFromName(tableName, columnName).getSecond().getNumPages();
        }

        public Schema getSchema(String tableName) throws DatabaseException {
            assert(this.active);
            return getTable(tableName).getSchema();
        }

        public Schema getFullyQualifiedSchema(String tableName) throws DatabaseException {
            assert(this.active);

            Schema schema = getTable(tableName).getSchema();
            List<String> newColumnNames = new ArrayList<String>();
            for (String oldName : schema.getFieldNames()) {
                newColumnNames.add(tableName + "." + oldName);
            }

            return new Schema(newColumnNames, schema.getFieldTypes());
        }

        private Pair<String, BPlusTree> resolveIndexFromName(String tableName,
                String columnName) throws DatabaseException {
            while (aliasMaps.containsKey(tableName)) {
                tableName = aliasMaps.get(tableName);
            }
            if (columnName.contains(".")) {
                String columnPrefix = columnName.split("\\.")[0];
                while (aliasMaps.containsKey(columnPrefix)) {
                    columnPrefix = aliasMaps.get(columnPrefix);
                }
                if (!tableName.equals(columnPrefix)) {
                    throw new DatabaseException("Column: " + columnName + " is not a column of " + tableName);
                }
                columnName = columnName.split("\\.")[1];
            }
            String indexName = tableName + "," + columnName;
            if (Database.this.indexLookup.containsKey(indexName)) {
                return new Pair<>(indexName, Database.this.indexLookup.get(indexName));
            }
            throw new DatabaseException("Index does not exist");
        }

        private Table getTable(String tableName) throws DatabaseException {
            if (this.tempTables.containsKey(tableName)) {
                return this.tempTables.get(tableName);
            }

            while (aliasMaps.containsKey(tableName)) {
                tableName = aliasMaps.get(tableName);
            }

            if (!Database.this.tableLookup.containsKey(tableName)) {
                throw new DatabaseException("Table: " + tableName + "does not exist");
            }

            return Database.this.tableLookup.get(tableName);
        }

        public void deleteTempTable(String tempTableName) {
            assert(this.active);

            if (!this.tempTables.containsKey(tempTableName)) {
                return;
            }

            this.tempTables.get(tempTableName).close();
            Database.this.tableLookup.remove(tempTableName);

            File f = new File(Database.this.fileDir + "temp/" + tempTableName + Table.FILENAME_EXTENSION);
            f.delete();
        }

        private void deleteAllTempTables() {
            Set<String> keys = tempTables.keySet();

            for (String tableName : keys) {
                deleteTempTable(tableName);
            }
        }

        public void block() {
            this.transactionLock.lock();
            try {
                this.blocked = true;
                while (this.blocked) {
                    this.unblocked.awaitUninterruptibly();
                }
            } finally {
                this.transactionLock.unlock();
            }
        }

        public void unblock() {
            this.transactionLock.lock();
            try {
                this.blocked = false;
                this.unblocked.signal();
            } finally {
                this.transactionLock.unlock();
            }
        }

        public boolean getBlocked() {
            return this.blocked;
        }
    }

    /* ******************************************************************************** */
    /* Every that follows is solely for the purpose of testing certain homeworks without
       requiring that previous homeworks be properly implemented. You should not change anything
       below. */
    /* ******************************************************************************** */
    protected Table newTable(String name, Schema schema, String filename, LockContext lockContext,
                             BaseTransaction transaction) {
        return new Table(name, schema, filename, lockContext, transaction);
    }

    protected Table newTable(String name, String filename, LockContext lockContext,
                             BaseTransaction transaction) throws DatabaseException {
        return new Table(name, filename, lockContext, transaction);
    }
}
