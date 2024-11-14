package edu.umd.cs424.database;

import edu.umd.cs424.database.concurrency.LockContext;
import edu.umd.cs424.database.concurrency.LockManager;
import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.table.Table;
import edu.umd.cs424.database.table.TableStub;

public class DatabaseWithTableStub extends Database {
    public DatabaseWithTableStub(String fileDir) throws DatabaseException {
        super(fileDir);
    }

    public DatabaseWithTableStub(String fileDir, int numMemoryPages) throws DatabaseException {
        super(fileDir, numMemoryPages);
    }

    public DatabaseWithTableStub(String fileDir, int numMemoryPages,
                                 LockManager lockManager) throws DatabaseException {
        super(fileDir, numMemoryPages, lockManager);
    }

    @Override
    protected Table newTable(String tableName, Schema schema, String fileName, LockContext lockContext,
                             BaseTransaction transaction) {
        return new TableStub(tableName, schema, fileName, lockContext, transaction);
    }

    @Override
    protected Table newTable(String tableName, String fileName, LockContext lockContext,
                             BaseTransaction transaction)
    throws DatabaseException  {
        return new TableStub(tableName, fileName, lockContext, transaction);
    }
}
