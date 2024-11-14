package edu.umd.cs424.database.table;

import edu.umd.cs424.database.BaseTransaction;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.common.ArrayBacktrackingIterator;
import edu.umd.cs424.database.common.BacktrackingIterator;
import edu.umd.cs424.database.concurrency.LockContext;
import edu.umd.cs424.database.io.Page;

import java.util.Iterator;

public class TableStub extends Table {
    public TableStub(String name, Schema schema, String filename, LockContext lockContext,
                     BaseTransaction transaction) {
        super(name, schema, filename, lockContext, transaction);
    }

    public TableStub(String name, String filename, LockContext lockContext,
                     BaseTransaction transaction) throws DatabaseException {
        super(name, filename, lockContext, transaction);
    }

    @Override
    protected BacktrackingIterator<RecordId> newTableIterator(BaseTransaction transaction) {
        /**
         * Simple implementation that assumes packed records (no deletes), which
         * is sufficient for testing non-HW2 code.
         */
        Iterator<Page> pageIterator = getAllocator().iterator(transaction);
        RecordId[] res = new RecordId[(int) getNumRecords()];

        pageIterator.next();
        int i = 0;
        while (pageIterator.hasNext()) {
            Page page = pageIterator.next();
            for (short entryNum = 0; entryNum < getNumRecordsPerPage() &&
                    i < getNumRecords(); ++entryNum, ++i) {
                res[i] = new RecordId(page.getPageNum(), entryNum);
            }
        }

        return new ArrayBacktrackingIterator<>(res);
    }
}
