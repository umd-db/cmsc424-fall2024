package edu.umd.cs424.database.concurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.umd.cs424.database.TimeoutScaling;
import edu.umd.cs424.database.categories.*;
import edu.umd.cs424.database.BaseTransaction;
import edu.umd.cs424.database.LoggingLockManager;
import edu.umd.cs424.database.common.Pair;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.junit.Assert.*;

@Category({HW5Tests.class, HW5Part1Tests.class})
public class TestLockContext {
    private LoggingLockManager lockManager;

    private LockContext dbLockContext;
    private LockContext tableLockContext;
    private LockContext pageLockContext;

    private BaseTransaction[] transactions;

    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (1000 * TimeoutScaling.factor)));

    @Before
    public void setUp() {
        lockManager = new LoggingLockManager();

        dbLockContext = lockManager.databaseContext();
        tableLockContext = dbLockContext.childContext("table");
        pageLockContext = tableLockContext.childContext("page");

        transactions = new BaseTransaction[8];
        for (int i = 0; i < transactions.length; i++) {
            transactions[i] = new DummyTransaction(lockManager, i);
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleAcquireFail() {
        dbLockContext.acquire(transactions[0], LockType.IS);
        try {
            tableLockContext.acquire(transactions[0], LockType.X);
            fail();
        } catch (InvalidLockException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleAcquirePass() {
        dbLockContext.acquire(transactions[0], LockType.IS);
        tableLockContext.acquire(transactions[0], LockType.S);
        Assert.assertEquals(Arrays.asList(new Lock(dbLockContext.getResourceName(), LockType.IS, 0L),
                                          new Lock(tableLockContext.getResourceName(), LockType.S, 0L)),
                            lockManager.getLocks(transactions[0]));
    }

    @Test
    @Category(PublicTests.class)
    public void testTreeAcquirePass() {
        dbLockContext.acquire(transactions[0], LockType.IX);
        tableLockContext.acquire(transactions[0], LockType.IS);
        pageLockContext.acquire(transactions[0], LockType.S);

        Assert.assertEquals(Arrays.asList(new Lock(dbLockContext.getResourceName(), LockType.IX, 0L),
                                          new Lock(tableLockContext.getResourceName(), LockType.IS, 0L),
                                          new Lock(pageLockContext.getResourceName(), LockType.S, 0L)),
                            lockManager.getLocks(transactions[0]));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleReleasePass() {
        dbLockContext.acquire(transactions[0], LockType.IS);
        tableLockContext.acquire(transactions[0], LockType.S);
        tableLockContext.release(transactions[0]);

        Assert.assertEquals(Collections.singletonList(new Lock(dbLockContext.getResourceName(), LockType.IS,
                            0L)),
                            lockManager.getLocks(transactions[0]));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimpleReleaseFail() {
        dbLockContext.acquire(transactions[0], LockType.IS);
        tableLockContext.acquire(transactions[0], LockType.S);
        try {
            dbLockContext.release(transactions[0]);
            fail();
        } catch (InvalidLockException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSharedPage() {
        BaseTransaction t1 = transactions[1];
        BaseTransaction t2 = transactions[2];

        LockContext r0 = tableLockContext;
        LockContext r1 = pageLockContext;

        dbLockContext.acquire(t1, LockType.IS);
        dbLockContext.acquire(t2, LockType.IS);
        r0.acquire(t1, LockType.IS);
        r0.acquire(t2, LockType.IS);
        r1.acquire(t1, LockType.S);
        r1.acquire(t2, LockType.S);

        assertTrue(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.IS));
        assertTrue(TestLockManager.holds(lockManager, t2, r0.getResourceName(), LockType.IS));
        assertTrue(TestLockManager.holds(lockManager, t1, r1.getResourceName(), LockType.S));
        assertTrue(TestLockManager.holds(lockManager, t2, r1.getResourceName(), LockType.S));
    }

    @Test
    @Category(PublicTests.class)
    public void testSandIS() {
        BaseTransaction t1 = transactions[1];
        BaseTransaction t2 = transactions[2];

        LockContext r0 = dbLockContext;
        LockContext r1 = tableLockContext;

        r0.acquire(t1, LockType.S);
        r0.acquire(t2, LockType.IS);
        r1.acquire(t2, LockType.S);
        r0.release(t1);

        assertTrue(TestLockManager.holds(lockManager, t2, r0.getResourceName(), LockType.IS));
        assertTrue(TestLockManager.holds(lockManager, t2, r1.getResourceName(), LockType.S));
        assertFalse(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.S));
    }

    @Test
    @Category(PublicTests.class)
    public void testSharedIntentConflict() {
        BaseTransaction t1 = transactions[1];
        BaseTransaction t2 = transactions[2];

        LockContext r0 = dbLockContext;
        LockContext r1 = tableLockContext;

        r0.acquire(t1, LockType.IS);
        r0.acquire(t2, LockType.IX);
        r1.acquire(t1, LockType.S);
        r1.acquire(t2, LockType.X);

        assertTrue(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.IS));
        assertTrue(TestLockManager.holds(lockManager, t2, r0.getResourceName(), LockType.IX));
        assertTrue(TestLockManager.holds(lockManager, t1, r1.getResourceName(), LockType.S));
        assertFalse(TestLockManager.holds(lockManager, t2, r1.getResourceName(), LockType.X));
    }

    @Test
    @Category(PublicTests.class)
    public void testSharedIntentConflictRelease() {
        BaseTransaction t1 = transactions[1];
        BaseTransaction t2 = transactions[2];

        LockContext r0 = dbLockContext;
        LockContext r1 = tableLockContext;

        r0.acquire(t1, LockType.IS);
        r0.acquire(t2, LockType.IX);
        r1.acquire(t1, LockType.S);
        r1.acquire(t2, LockType.X);
        r1.release(t1);

        assertTrue(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.IS));
        assertTrue(TestLockManager.holds(lockManager, t2, r0.getResourceName(), LockType.IX));
        assertFalse(TestLockManager.holds(lockManager, t1, r1.getResourceName(), LockType.S));
        assertTrue(TestLockManager.holds(lockManager, t2, r1.getResourceName(), LockType.X));
    }

    @Test
    @Category(PublicTests.class)
    public void testSimplePromote() {
        BaseTransaction t1 = transactions[1];
        dbLockContext.acquire(t1, LockType.S);
        dbLockContext.promote(t1, LockType.X);
        assertTrue(TestLockManager.holds(lockManager, t1, dbLockContext.getResourceName(), LockType.X));
    }

    @Test
    @Category(PublicTests.class)
    public void testEscalateFail() {
        BaseTransaction t1 = transactions[1];

        LockContext r0 = dbLockContext;

        r0.acquire(t1, LockType.IS);
        try {
            r0.escalate(t1);
            fail();
        } catch (NoLockHeldException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testEscalateS() {
        BaseTransaction t1 = transactions[1];

        LockContext r0 = dbLockContext;
        LockContext r1 = tableLockContext;

        r0.acquire(t1, LockType.IS);
        r1.acquire(t1, LockType.S);
        r0.escalate(t1);

        assertTrue(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.S));
        assertFalse(TestLockManager.holds(lockManager, t1, r1.getResourceName(), LockType.S));
    }

    @Test
    @Category(PublicTests.class)
    public void testEscalateMultipleS() {
        BaseTransaction t1 = transactions[1];

        LockContext r0 = dbLockContext;
        LockContext r1 = tableLockContext;
        LockContext r2 = dbLockContext.childContext("table2");
        LockContext r3 = dbLockContext.childContext("table3");

        r0.capacity(4);

        r0.acquire(t1, LockType.IS);
        r1.acquire(t1, LockType.S);
        r2.acquire(t1, LockType.IS);
        r3.acquire(t1, LockType.S);

        assertEquals(3.0 / 4, r0.saturation(t1), 1e-6);
        r0.escalate(t1);
        assertEquals(0.0, r0.saturation(t1), 1e-6);

        assertTrue(TestLockManager.holds(lockManager, t1, r0.getResourceName(), LockType.S));
        assertFalse(TestLockManager.holds(lockManager, t1, r1.getResourceName(), LockType.S));
        assertFalse(TestLockManager.holds(lockManager, t1, r2.getResourceName(), LockType.IS));
        assertFalse(TestLockManager.holds(lockManager, t1, r3.getResourceName(), LockType.S));
    }

    @Test
    @Category(PublicTests.class)
    public void testGetLockType() {
        BaseTransaction t1 = transactions[1];
        BaseTransaction t2 = transactions[2];
        BaseTransaction t3 = transactions[3];
        BaseTransaction t4 = transactions[4];

        dbLockContext.acquire(t1, LockType.S);
        dbLockContext.acquire(t2, LockType.IS);
        dbLockContext.acquire(t3, LockType.IS);
        dbLockContext.acquire(t4, LockType.IS);

        tableLockContext.acquire(t2, LockType.S);
        tableLockContext.acquire(t3, LockType.IS);

        pageLockContext.acquire(t3, LockType.S);

        assertEquals(LockType.S, pageLockContext.getEffectiveLockType(t1));
        assertEquals(LockType.S, pageLockContext.getEffectiveLockType(t2));
        assertEquals(LockType.S, pageLockContext.getEffectiveLockType(t3));
        assertEquals(LockType.NL, pageLockContext.getEffectiveLockType(t4));
        assertEquals(LockType.NL, pageLockContext.getExplicitLockType(t1));
        assertEquals(LockType.NL, pageLockContext.getExplicitLockType(t2));
        assertEquals(LockType.S, pageLockContext.getExplicitLockType(t3));
        assertEquals(LockType.NL, pageLockContext.getExplicitLockType(t4));
    }

    @Test
    @Category(PublicTests.class)
    public void testReadonly() {
        dbLockContext.disableChildLocks();
        LockContext tableContext = dbLockContext.childContext("table2");
        BaseTransaction t1 = transactions[1];
        dbLockContext.acquire(t1, LockType.IX);
        try {
            tableContext.acquire(t1, LockType.IX);
            fail();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
        try {
            tableContext.release(t1);
            fail();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
        try {
            tableContext.promote(t1, LockType.IX);
            fail();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
        try {
            tableContext.escalate(t1);
            fail();
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
    }

    @Test
    @Category(PublicTests.class)
    public void testSaturation() {
        LockContext tableContext = dbLockContext.childContext("table2");
        BaseTransaction t1 = transactions[1];
        dbLockContext.capacity(10);
        dbLockContext.acquire(t1, LockType.IX);
        tableContext.acquire(t1, LockType.IS);
        assertEquals(0.1, dbLockContext.saturation(t1), 1E-6);
        tableContext.promote(t1, LockType.IX);
        assertEquals(0.1, dbLockContext.saturation(t1), 1E-6);
        tableContext.release(t1);
        assertEquals(0.0, dbLockContext.saturation(t1), 1E-6);
        tableContext.acquire(t1, LockType.IS);
        dbLockContext.escalate(t1);
        assertEquals(0.0, dbLockContext.saturation(t1), 1E-6);
    }

}
