package edu.umd.cs424.database.concurrency;

import edu.umd.cs424.database.BaseTransaction;
import edu.umd.cs424.database.LoggingLockManager;
import edu.umd.cs424.database.TimeoutScaling;
import edu.umd.cs424.database.categories.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@Category({HW5Tests.class, HW5Part1Tests.class})
public class TestLockManager {
    private LoggingLockManager lockman;
    private BaseTransaction[] transactions;
    private ResourceName dbResource;
    private ResourceName[] tables;

    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (1000 * TimeoutScaling.factor)));

    public static boolean holds(LockManager lockman, BaseTransaction transaction, ResourceName name,
                                LockType type) {
        List<Lock> locks = lockman.getLocks(transaction);
        if (locks == null) {
            return false;
        }
        for (Lock lock : locks) {
            if (lock.name == name && lock.lockType == type) {
                return true;
            }
        }
        return false;
    }

    @Before
    public void setUp() {
        lockman = new LoggingLockManager();
        transactions = new BaseTransaction[8];
        dbResource = new ResourceName("database");
        tables = new ResourceName[transactions.length];
        for (int i = 0; i < transactions.length; ++i) {
            transactions[i] = new DummyTransaction(lockman, i);
            tables[i] = new ResourceName(Collections.singletonList("database"), "table" + i);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleAcquireRelease() {
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.S, Collections.emptyList());
        lockman.acquireAndRelease(transactions[0], tables[1], LockType.S,
                                  Collections.singletonList(tables[0]));
        assertEquals(LockType.NL, lockman.getLockType(transactions[0], tables[0]));
        assertEquals(Collections.emptyList(), lockman.getLocks(tables[0]));
        assertEquals(LockType.S, lockman.getLockType(transactions[0], tables[1]));
        assertEquals(Collections.singletonList(new Lock(tables[1], LockType.S, 0L)),
                     lockman.getLocks(tables[1]));
    }

    @Test
    @Category(PublicTests.class)
    public void testAcquireReleaseQueue() {
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.X, Collections.emptyList());
        lockman.acquireAndRelease(transactions[1], tables[1], LockType.X, Collections.emptyList());
        lockman.acquireAndRelease(transactions[0], tables[1], LockType.X,
                                  Collections.singletonList(tables[0]));
        assertEquals(LockType.X, lockman.getLockType(transactions[0], tables[0]));
        assertEquals(Collections.singletonList(new Lock(tables[0], LockType.X, 0L)),
                     lockman.getLocks(tables[0]));
        assertEquals(LockType.NL, lockman.getLockType(transactions[0], tables[1]));
        assertEquals(Collections.singletonList(new Lock(tables[1], LockType.X, 1L)),
                     lockman.getLocks(tables[1]));
        assertTrue(transactions[0].getBlocked());
    }

    @Test
    @Category(PublicTests.class)
    public void testAcquireReleaseDuplicateLock() {
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.X, Collections.emptyList());
        try {
            lockman.acquireAndRelease(transactions[0], tables[0], LockType.X, Collections.emptyList());
            fail();
        } catch (DuplicateLockRequestException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testAcquireReleaseNotHeld() {
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.X, Collections.emptyList());
        try {
            lockman.acquireAndRelease(transactions[0], tables[2], LockType.X, Arrays.asList(tables[0],
                                      tables[1]));
            fail();
        } catch (NoLockHeldException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testAcquireReleaseUpgrade() {
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.S, Collections.emptyList());
        lockman.acquireAndRelease(transactions[0], tables[0], LockType.X,
                                  Collections.singletonList(tables[0]));
        assertEquals(LockType.X, lockman.getLockType(transactions[0], tables[0]));
        assertEquals(Collections.singletonList(new Lock(tables[0], LockType.X, 0L)),
                     lockman.getLocks(tables[0]));
        assertFalse(transactions[0].getBlocked());
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleAcquireLock() {
        lockman.acquire(transactions[0], tables[0], LockType.S);
        lockman.acquire(transactions[1], tables[1], LockType.X);
        assertEquals(LockType.S, lockman.getLockType(transactions[0], tables[0]));
        assertEquals(Collections.singletonList(new Lock(tables[0], LockType.S, 0L)),
                     lockman.getLocks(tables[0]));
        assertEquals(LockType.X, lockman.getLockType(transactions[1], tables[1]));
        assertEquals(Collections.singletonList(new Lock(tables[1], LockType.X, 1L)),
                     lockman.getLocks(tables[1]));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleAcquireLockFail() {
        BaseTransaction t1 = transactions[0];

        ResourceName r1 = dbResource;

        lockman.acquire(t1, r1, LockType.X);
        try {
            lockman.acquire(t1, r1, LockType.X);
            fail();
        } catch (DuplicateLockRequestException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleReleaseLock() {
        lockman.acquire(transactions[0], dbResource, LockType.X);
        lockman.release(transactions[0], dbResource);
        assertEquals(LockType.NL, lockman.getLockType(transactions[0], dbResource));
        assertEquals(Collections.emptyList(), lockman.getLocks(dbResource));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleConflict() {
        lockman.acquire(transactions[0], dbResource, LockType.X);
        lockman.acquire(transactions[1], dbResource, LockType.X);
        assertEquals(LockType.X, lockman.getLockType(transactions[0], dbResource));
        assertEquals(LockType.NL, lockman.getLockType(transactions[1], dbResource));
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 0L)),
                     lockman.getLocks(dbResource));
        assertFalse(transactions[0].getBlocked());
        assertTrue(transactions[1].getBlocked());

        lockman.release(transactions[0], dbResource);
        assertEquals(LockType.NL, lockman.getLockType(transactions[0], dbResource));
        assertEquals(LockType.X, lockman.getLockType(transactions[1], dbResource));
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 1L)),
                     lockman.getLocks(dbResource));
        assertFalse(transactions[0].getBlocked());
        assertFalse(transactions[1].getBlocked());
    }

    @Test
    @Category(PublicTests.class)
    public void testSXS() {
        List<Boolean> blocked_status = new ArrayList<>();

        lockman.acquire(transactions[0], dbResource, LockType.S);
        lockman.acquire(transactions[1], dbResource, LockType.X);
        lockman.acquire(transactions[2], dbResource, LockType.S);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.S, 0L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 3; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, true, true), blocked_status);

        lockman.release(transactions[0], dbResource);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 1L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 3; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, false, true), blocked_status);

        lockman.release(transactions[1], dbResource);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.S, 2L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 3; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, false, false), blocked_status);
    }

    @Test
    @Category(PublicTests.class)
    public void testXSXS() {
        List<Boolean> blocked_status = new ArrayList<>();

        lockman.acquire(transactions[0], dbResource, LockType.X);
        lockman.acquire(transactions[1], dbResource, LockType.S);
        lockman.acquire(transactions[2], dbResource, LockType.X);
        lockman.acquire(transactions[3], dbResource, LockType.S);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 0L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 4; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, true, true, true), blocked_status);

        lockman.release(transactions[0], dbResource);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.S, 1L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 4; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, false, true, true), blocked_status);

        lockman.release(transactions[1], dbResource);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 2L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 4; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, false, false, true), blocked_status);

        lockman.release(transactions[2], dbResource);
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.S, 3L)),
                     lockman.getLocks(dbResource));
        blocked_status.clear();
        for (int i = 0; i < 4; ++i) { blocked_status.add(i, transactions[i].getBlocked()); }
        assertEquals(Arrays.asList(false, false, false, false), blocked_status);
    }

    @Test
    @Category(PublicTests.class)
    public void testSimplePromoteLock() {
        lockman.acquire(transactions[0], dbResource, LockType.S);
        lockman.promote(transactions[0], dbResource, LockType.X);
        assertEquals(LockType.X, lockman.getLockType(transactions[0], dbResource));
        assertEquals(Collections.singletonList(new Lock(dbResource, LockType.X, 0L)),
                     lockman.getLocks(dbResource));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimplePromoteLockNotHeld() {
        try {
            lockman.promote(transactions[0], dbResource, LockType.X);
            fail();
        } catch (NoLockHeldException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimplePromoteLockAlreadyHeld() {
        lockman.acquire(transactions[0], dbResource, LockType.X);
        try {
            lockman.promote(transactions[0], dbResource, LockType.X);
            fail();
        } catch (DuplicateLockRequestException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testFIFOQueueLocks() {
        lockman.acquire(transactions[0], dbResource, LockType.X);
        lockman.acquire(transactions[1], dbResource, LockType.X);
        lockman.acquire(transactions[2], dbResource, LockType.X);

        assertTrue(holds(lockman, transactions[0], dbResource, LockType.X));
        assertFalse(holds(lockman, transactions[1], dbResource, LockType.X));
        assertFalse(holds(lockman, transactions[2], dbResource, LockType.X));

        lockman.release(transactions[0], dbResource);

        assertFalse(holds(lockman, transactions[0], dbResource, LockType.X));
        assertTrue(holds(lockman, transactions[1], dbResource, LockType.X));
        assertFalse(holds(lockman, transactions[2], dbResource, LockType.X));

        lockman.release(transactions[1], dbResource);

        assertFalse(holds(lockman, transactions[0], dbResource, LockType.X));
        assertFalse(holds(lockman, transactions[1], dbResource, LockType.X));
        assertTrue(holds(lockman, transactions[2], dbResource, LockType.X));
    }

    @Test
    @Category(PublicTests.class)
    public void testStatusUpdates() {
        BaseTransaction t1 = transactions[0];
        BaseTransaction t2 = transactions[1];

        ResourceName r1 = dbResource;

        lockman.acquire(t1, r1, LockType.X);
        lockman.acquire(t2, r1, LockType.X);

        assertTrue(holds(lockman, t1, r1, LockType.X));
        assertFalse(holds(lockman, t2, r1, LockType.X));
        assertFalse(t1.getBlocked());
        assertTrue(t2.getBlocked());

        lockman.release(t1, r1);

        assertFalse(holds(lockman, t1, r1, LockType.X));
        assertTrue(holds(lockman, t2, r1, LockType.X));
        assertFalse(t1.getBlocked());
        assertFalse(t2.getBlocked());
    }

    @Test
    @Category(PublicTests.class)
    public void testTableEventualUpgrade() {
        BaseTransaction t1 = transactions[0];
        BaseTransaction t2 = transactions[1];

        ResourceName r1 = dbResource;

        lockman.acquire(t1, r1, LockType.S);
        lockman.acquire(t2, r1, LockType.S);

        assertTrue(holds(lockman, t1, r1, LockType.S));
        assertTrue(holds(lockman, t2, r1, LockType.S));

        lockman.promote(t1, r1, LockType.X);

        assertTrue(holds(lockman, t1, r1, LockType.S));
        assertFalse(holds(lockman, t1, r1, LockType.X));
        assertTrue(holds(lockman, t2, r1, LockType.S));

        lockman.release(t2, r1);

        assertTrue(holds(lockman, t1, r1, LockType.X));
        assertFalse(holds(lockman, t2, r1, LockType.S));

        lockman.release(t1, r1);

        assertFalse(holds(lockman, t1, r1, LockType.X));
        assertFalse(holds(lockman, t2, r1, LockType.S));
    }

    @Test
    @Category(PublicTests.class)
    public void testIntentBlockedAcquire() {
        BaseTransaction t1 = transactions[0];
        BaseTransaction t2 = transactions[1];

        ResourceName r0 = dbResource;

        lockman.acquire(t1, r0, LockType.S);
        lockman.acquire(t2, r0, LockType.IX);

        assertTrue(holds(lockman, t1, r0, LockType.S));
        assertFalse(holds(lockman, t2, r0, LockType.IX));

        lockman.release(t1, r0);

        assertFalse(holds(lockman, t1, r0, LockType.S));
        assertTrue(holds(lockman, t2, r0, LockType.IX));
    }

    @Test
    @Category(PublicTests.class)
    public void testReleaseUnheldLock() {
        BaseTransaction t1 = transactions[0];
        try {
            lockman.release(t1, dbResource);
            fail();
        } catch (NoLockHeldException e) {
            // do nothing
        }
    }

}

