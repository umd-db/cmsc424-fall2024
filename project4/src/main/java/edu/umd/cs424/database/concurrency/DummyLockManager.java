package edu.umd.cs424.database.concurrency;

import java.util.Arrays;
import java.util.List;

import edu.umd.cs424.database.BaseTransaction;
import edu.umd.cs424.database.common.Pair;

/**
 * Dummy lock manager that does no locking or error checking.
 *
 * Used for non-locking-related tests to disable locking.
 */
public class DummyLockManager extends LockManager {
    public DummyLockManager() { }

    @Override
    public LockContext databaseContext() {
        return new DummyLockContext(null);
    }

    @Override
    public LockContext orphanContext(Object name) {
        return new DummyLockContext(null);
    }

    @Override
    public void acquire(BaseTransaction transaction, ResourceName name,
                        LockType lockType) throws DuplicateLockRequestException { }

    @Override
    public void release(BaseTransaction transaction, ResourceName name)
    throws NoLockHeldException { }

    @Override
    public void promote(BaseTransaction transaction, ResourceName name,
                        LockType newLockType)
    throws DuplicateLockRequestException, NoLockHeldException, InvalidLockException { }

    @Override
    public LockType getLockType(BaseTransaction transaction, ResourceName name) {
        return LockType.NL;
    }

    @Override
    public List<Lock> getLocks(ResourceName name) {
        return Arrays.asList();
    }

    @Override
    public List<Lock> getLocks(BaseTransaction transaction) {
        return Arrays.asList();
    }
}

