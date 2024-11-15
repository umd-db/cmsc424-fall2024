package edu.umd.cs424.database.concurrency;

import edu.umd.cs424.database.BaseTransaction;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock acquisition
 * for the user (you, in Part 2 of HW5). Generally speaking, you should use LockUtil
 * for lock acquisition instead of calling LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that TRANSACTION can perform actions requiring LOCKTYPE on LOCKCONTEXT.
     *
     * This method should promote/escalate as needed, but should only grant the least
     * permissive set of locks needed.
     *
     * lockType must be one of LockType.S, LockType.X, and behavior is unspecified
     * if an intent lock is passed in to this method (you can do whatever you want in this case).
     *
     * If TRANSACTION is null, this method should do nothing.
     */
    public static void ensureSufficientLockHeld(BaseTransaction transaction, LockContext lockContext,
            LockType lockType) {
        return;
    }

}
