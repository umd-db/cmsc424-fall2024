package edu.umd.cs424.database.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs424.database.concurrency.DummyLockContext;
import edu.umd.cs424.database.common.Pair;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.table.RecordId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(1)
public class TestBPlusTree {
    public static final String filename = "TestBPlusTree";
    private Path file;

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempFolder;

    // Helpers /////////////////////////////////////////////////////////////////
    @BeforeEach
    public void initFile() throws IOException {
        this.file = Files.createFile(tempFolder.resolve(filename));
    }

    private BPlusTree getBPlusTree(Type keySchema, int order) {
        try {
            return new BPlusTree(file.toAbsolutePath().toString(), keySchema, order, new DummyLockContext(), null);
        } catch (BPlusTreeException e) {
            // Why do checked exceptions exist? Why are they even used here?
            throw new RuntimeException(e);
        }
    }

    private static class TreeEntry {
        IntDataBox key;
        RecordId value;

        public TreeEntry(IntDataBox key, RecordId value) {
            this.key = key;
            this.value = value;
        }
    }

    private static void addToBPlusTree(BPlusTree tree, int size, Random random, ArrayList<TreeEntry> entries) {
        var existing = new HashSet<>(entries.stream().map(entry -> entry.key.getInt()).toList());
        for (int i = 0; i < size; i++) {
            int rand;
            // use rejection sampling to avoid duplicates
            do {
                rand = random.nextInt();
            } while (existing.contains(rand));
            existing.add(rand);
            var entry = new TreeEntry(new IntDataBox(rand), new RecordId(i, (short) 1));
            entries.add(entry);
            try {
                tree.put(null, entry.key, entry.value);
            } catch (BPlusTreeException e) {
                throw new RuntimeException(e);
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.key.getInt()));
    }

    private BPlusTree makeRandomBPlusTree(int size,
                                          ArrayList<TreeEntry> entries) {
        return makeRandomBPlusTree(size, 42, entries);
    }

    private BPlusTree makeRandomBPlusTree(int size, int seed,
                                          ArrayList<TreeEntry> entries) {
        return makeRandomBPlusTree(size, new Random(seed), entries);
    }

    private BPlusTree makeRandomBPlusTree(int size, Random random,
                                          ArrayList<TreeEntry> entries) {
        var tree = getBPlusTree(Type.intType(), 5);
        addToBPlusTree(tree, size, random, entries);
        return tree;
    }

    private static void shuffleBPlusTree(BPlusTree tree, Random random, ArrayList<TreeEntry> entries) {
        int originalSize = entries.size();
        // between 5% and 25% probability of removing each element
        int removeProbability = 5 + random.nextInt(20);
        var result = new ArrayList<TreeEntry>();
        for (var entry : entries) {
            int r = random.nextInt(100);
            if (r < removeProbability) {
                tree.remove(null, entry.key);
            } else {
                result.add(entry);
            }
        }
        entries.clear();
        entries.addAll(result);
        addToBPlusTree(tree, originalSize - entries.size(), random, entries);
    }

    private static <T> ArrayList<T> iteratorToList(Iterator<T> iter, int limit) {
        var xs = new ArrayList<T>();
        while (iter.hasNext() && xs.size() < limit) {
            xs.add(iter.next());
        }
        return xs;
    }

    private static <T> ArrayList<T> iteratorToList(Iterator<T> iter) {
        return iteratorToList(iter, Integer.MAX_VALUE);
    }

    // Tests ///////////////////////////////////////////////////////////////////

    @Test
    public void testBulkLoad_simple() throws BPlusTreeException {
        BPlusTree tree = getBPlusTree(Type.intType(), 2);
        float fillFactor = 0.75f;
        assertEquals("()", tree.toSexp(null));

        var data = new ArrayList<Pair<DataBox, RecordId>>();
        for (int i = 1; i <= 11; ++i) {
            data.add(new Pair<>(new IntDataBox(i), new RecordId(i, (short) i)));
        }

        tree.bulkLoad(null, data.iterator(), fillFactor);
        //      (    4        7         10        _   )
        //       /       |         |         \
        // (1 2 3 _) (4 5 6 _) (7 8 9 _) (10 11 _ _)
        String leaf0 = "((1 (1 1)) (2 (2 2)) (3 (3 3)))";
        String leaf1 = "((4 (4 4)) (5 (5 5)) (6 (6 6)))";
        String leaf2 = "((7 (7 7)) (8 (8 8)) (9 (9 9)))";
        String leaf3 = "((10 (10 10)) (11 (11 11)))";
        String sexp = String.format("(%s 4 %s 7 %s 10 %s)", leaf0, leaf1, leaf2, leaf3);
        assertEquals(sexp, tree.toSexp(null));
    }

    @Test
    public void testWhiteBoxTest() throws BPlusTreeException, IOException {
        BPlusTree tree = getBPlusTree(Type.intType(), 1);
        assertEquals("()", tree.toSexp(null));

        // (4)
        tree.put(null, new IntDataBox(4), new RecordId(4, (short) 4));
        assertEquals("((4 (4 4)))", tree.toSexp(null));

        // (4 9)
        tree.put(null, new IntDataBox(9), new RecordId(9, (short) 9));
        assertEquals("((4 (4 4)) (9 (9 9)))", tree.toSexp(null));

        //   (6)
        //  /   \
        // (4) (6 9)
        tree.put(null, new IntDataBox(6), new RecordId(6, (short) 6));
        String l = "((4 (4 4)))";
        String r = "((6 (6 6)) (9 (9 9)))";
        assertEquals(String.format("(%s 6 %s)", l, r), tree.toSexp(null));

        //     (6)
        //    /   \
        // (2 4) (6 9)
        tree.put(null, new IntDataBox(2), new RecordId(2, (short) 2));
        l = "((2 (2 2)) (4 (4 4)))";
        r = "((6 (6 6)) (9 (9 9)))";
        assertEquals(String.format("(%s 6 %s)", l, r), tree.toSexp(null));

        //      (6 7)
        //     /  |  \
        // (2 4) (6) (7 9)
        tree.put(null, new IntDataBox(7), new RecordId(7, (short) 7));
        l = "((2 (2 2)) (4 (4 4)))";
        String m = "((6 (6 6)))";
        r = "((7 (7 7)) (9 (9 9)))";
        assertEquals(String.format("(%s 6 %s 7 %s)", l, m, r), tree.toSexp(null));

        //         (7)
        //        /   \
        //     (6)     (8)
        //    /   \   /   \
        // (2 4) (6) (7) (8 9)
        tree.put(null, new IntDataBox(8), new RecordId(8, (short) 8));
        String ll = "((2 (2 2)) (4 (4 4)))";
        String lr = "((6 (6 6)))";
        String rl = "((7 (7 7)))";
        String rr = "((8 (8 8)) (9 (9 9)))";
        l = String.format("(%s 6 %s)", ll, lr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 7 %s)", l, r), tree.toSexp(null));

        //            (7)
        //           /   \
        //     (3 6)       (8)
        //   /   |   \    /   \
        // (2) (3 4) (6) (7) (8 9)
        tree.put(null, new IntDataBox(3), new RecordId(3, (short) 3));
        ll = "((2 (2 2)))";
        String lm = "((3 (3 3)) (4 (4 4)))";
        lr = "((6 (6 6)))";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)) (9 (9 9)))";
        l = String.format("(%s 3 %s 6 %s)", ll, lm, lr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 7 %s)", l, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //   (3)      (6)       (8)
        //  /   \    /   \    /   \
        // (2) (3) (4 5) (6) (7) (8 9)
        tree.put(null, new IntDataBox(5), new RecordId(5, (short) 5));
        ll = "((2 (2 2)))";
        lr = "((3 (3 3)))";
        String ml = "((4 (4 4)) (5 (5 5)))";
        String mr = "((6 (6 6)))";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)) (9 (9 9)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (1 2) (3) (4 5) (6) (7) (8 9)
        tree.put(null, new IntDataBox(1), new RecordId(1, (short) 1));
        ll = "((1 (1 1)) (2 (2 2)))";
        lr = "((3 (3 3)))";
        ml = "((4 (4 4)) (5 (5 5)))";
        mr = "((6 (6 6)))";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)) (9 (9 9)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (  2) (3) (4 5) (6) (7) (8 9)
        tree.remove(null, new IntDataBox(1));
        ll = "((2 (2 2)))";
        lr = "((3 (3 3)))";
        ml = "((4 (4 4)) (5 (5 5)))";
        mr = "((6 (6 6)))";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)) (9 (9 9)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (  2) (3) (4 5) (6) (7) (8  )
        tree.remove(null, new IntDataBox(9));
        ll = "((2 (2 2)))";
        lr = "((3 (3 3)))";
        ml = "((4 (4 4)) (5 (5 5)))";
        mr = "((6 (6 6)))";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (  2) (3) (4 5) ( ) (7) (8  )
        tree.remove(null, new IntDataBox(6));
        ll = "((2 (2 2)))";
        lr = "((3 (3 3)))";
        ml = "((4 (4 4)) (5 (5 5)))";
        mr = "()";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (  2) (3) (  5) ( ) (7) (8  )
        tree.remove(null, new IntDataBox(4));
        ll = "((2 (2 2)))";
        lr = "((3 (3 3)))";
        ml = "((5 (5 5)))";
        mr = "()";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (   ) (3) (  5) ( ) (7) (8  )
        tree.remove(null, new IntDataBox(2));
        ll = "()";
        lr = "((3 (3 3)))";
        ml = "((5 (5 5)))";
        mr = "()";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (   ) (3) (   ) ( ) (7) (8  )
        tree.remove(null, new IntDataBox(5));
        ll = "()";
        lr = "((3 (3 3)))";
        ml = "()";
        mr = "()";
        rl = "((7 (7 7)))";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (   ) (3) (   ) ( ) ( ) (8  )
        tree.remove(null, new IntDataBox(7));
        ll = "()";
        lr = "((3 (3 3)))";
        ml = "()";
        mr = "()";
        rl = "()";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (   ) ( ) (   ) ( ) ( ) (8  )
        tree.remove(null, new IntDataBox(3));
        ll = "()";
        lr = "()";
        ml = "()";
        mr = "()";
        rl = "()";
        rr = "((8 (8 8)))";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));

        //            (4 7)
        //           /  |  \
        //    (3)      (6)       (8)
        //   /   \    /   \    /   \
        // (   ) ( ) (   ) ( ) ( ) (   )
        tree.remove(null, new IntDataBox(8));
        ll = "()";
        lr = "()";
        ml = "()";
        mr = "()";
        rl = "()";
        rr = "()";
        l = String.format("(%s 3 %s)", ll, lr);
        m = String.format("(%s 6 %s)", ml, mr);
        r = String.format("(%s 8 %s)", rl, rr);
        assertEquals(String.format("(%s 4 %s 7 %s)", l, m, r), tree.toSexp(null));
    }

    @Timeout(10)
    @Test
    public void testPut_random() throws BPlusTreeException, IOException {
        var keys = new ArrayList<DataBox>();
        var rids = new ArrayList<RecordId>();
        var sortedRids = new ArrayList<RecordId>();
        for (int i = 0; i < 1000; ++i) {
            keys.add(new IntDataBox(i));
            rids.add(new RecordId(i, (short) i));
            sortedRids.add(new RecordId(i, (short) i));
        }

        // Try trees with different orders.
        for (int d = 2; d < 5; ++d) {
            // Try trees with different insertion orders.
            for (int n = 0; n < 2; ++n) {
                Collections.shuffle(keys, new Random(42));
                Collections.shuffle(rids, new Random(42));

                // Insert all the keys.
                BPlusTree tree = getBPlusTree(Type.intType(), d);
                for (int i = 0; i < keys.size(); ++i) {
                    tree.put(null, keys.get(i), rids.get(i));
                }

                // Test get.
                for (int i = 0; i < keys.size(); ++i) {
                    assertEquals(Optional.of(rids.get(i)), tree.get(null, keys.get(i)));
                }

                // Test scanAll.
                assertEquals(sortedRids, iteratorToList(tree.scanAll(null)));

                // Load the tree from disk.
                var fromDisk = new BPlusTree(file.toAbsolutePath().toString(), new DummyLockContext(), null);
                assertEquals(sortedRids, iteratorToList(fromDisk.scanAll(null)));

                // Test remove.
                Collections.shuffle(keys, new Random(42));
                Collections.shuffle(rids, new Random(42));
                for (DataBox key : keys) {
                    fromDisk.remove(null, key);
                    assertEquals(Optional.empty(), fromDisk.get(null, key));
                }
            }
        }
    }

    @Test
    public void testMaxOrder() {
        // Note that this white box test depend critically on the implementation
        // of toBytes and includes a lot of magic numbers that won't make sense
        // unless you read toBytes.
        assertEquals(4, Type.intType().getSizeInBytes());
        assertEquals(6, RecordId.getSizeInBytes());
        int pageSizeInBytes = 100;
        Type keySchema = Type.intType();
        assertEquals(4, LeafNode.maxOrder(pageSizeInBytes, keySchema));
        assertEquals(5, InnerNode.maxOrder(pageSizeInBytes, keySchema));
        assertEquals(4, BPlusTree.maxOrder(pageSizeInBytes, keySchema));
    }

    /**
     * This test constructs a random BPlus Tree of size <code>size</code> and then ensures that each record that
     * was inserted into the tree can be retrieved with the iterator.
     *
     * @param size The number of elements in the tree.
     */
    @ParameterizedTest
    @ValueSource(ints = {5, 19, 33})
    public void testIterator_simple(int size) {
        var entries = new ArrayList<TreeEntry>();
        try (var tree = makeRandomBPlusTree(size, entries)) {
            // all records returned by the iterator should match those stored in entries
            var iter = tree.scanAll(null);
            for (var entry : entries) {
                // first, the iterator should indicate that there are more records to come
                assertTrue(iter.hasNext());
                // second, the next record returned by the iterator should match the one we inserted before,
                // which is stored in entry.value
                assertEquals(entry.value, iter.next());
            }
            // now that we have iterated through all the records, the iterator should indicate that no more record
            // is available
            assertFalse(iter.hasNext());
            // the iterator should also throw an exception when we try to retrieve the next record
            assertThrows(NoSuchElementException.class, iter::next);
        }
    }

    /**
     * This method tests the BPlus Tree iterator in a similar fashion as the one above but misses one thing.
     *
     * @param size The number of elements in the BPlus Tree.
     */
    @ParameterizedTest
    @ValueSource(ints = {11, 33})
    public void testIterator_noHasNext(int size) {
        var entries = new ArrayList<TreeEntry>();
        // should work even if hasNext is never called
        try (var tree = makeRandomBPlusTree(size, entries)) {
            var iter = tree.scanAll(null);
            for (int i = 0; i < size; i++) {
                assertEquals(entries.get(i).value, iter.next());
            }
            assertThrows(NoSuchElementException.class, iter::next);
        }
    }

    @Test
    public void testIterator_rangeEdgeCase() {
        var entries = new ArrayList<TreeEntry>();
        try (var tree = makeRandomBPlusTree(0, entries)) {
            // tree is empty -> doesn't yield anything
            var iter = tree.scanEnhanced(null, new IntDataBox(Integer.MIN_VALUE), new IntDataBox(Integer.MAX_VALUE));
            assertThrows(NoSuchElementException.class, iter::next);
            assertFalse(iter.hasNext());
        }
        try (var tree = makeRandomBPlusTree(15, entries)) {
            // no value in range -> doesn't yield anything
            var iter = tree.scanEnhanced(null, new IntDataBox(Integer.MAX_VALUE), new IntDataBox(Integer.MIN_VALUE));
            assertThrows(NoSuchElementException.class, iter::next);
            assertFalse(iter.hasNext());
            var entry = entries.get(7);
            // looking for a single value -> iterator should only return one value
            iter = tree.scanEnhanced(null, entry.key, entry.key);
            assertTrue(iter.hasNext());
            assertEquals(entry.value, iter.next());
            assertThrows(NoSuchElementException.class, iter::next);
            assertFalse(iter.hasNext());
        }
    }

    /**
     * Repeatedly calling hasNext should not affect performance
     */
    @ParameterizedTest
    @ValueSource(ints = {5483})
    @Timeout(8)
    public void testIterator_performance_1(int size) {
        var entries = new ArrayList<TreeEntry>();
        int repetitions = 50;
        Random r = new Random(42);
        // repeatedly calling hasNext should not affect performance
        try (var tree = makeRandomBPlusTree(size, r, entries)) {
            repeatTest(tree, r, entries, 5, 1, () -> {
                var first = entries.get(0);
                var last = entries.get(entries.size() - 1);
                var iter = tree.scanEnhanced(null, first.key, last.key, entries.size(),
                        (key) -> key.getInt() == first.key.getInt() || key.getInt() == last.key.getInt());
                for (int i = 0; i < repetitions; i++) {
                    assertTrue(iter.hasNext());
                }
                assertEquals(first.value, iter.next());
                for (int i = 0; i < repetitions; i++) {
                    assertTrue(iter.hasNext());
                }
                assertEquals(last.value, iter.next());
                for (int i = 0; i < repetitions; i++) {
                    assertFalse(iter.hasNext());
                }
                assertThrows(NoSuchElementException.class, iter::next);
            });
        }
    }


    /**
     * This test checks that the iterator is fast enough when only the beginning of the tree is needed.
     */
    @Test
    @Timeout(8)
    public void testIterator_performance_2() {
        int size = 5483;
        var entries = new ArrayList<TreeEntry>();
        var random = new Random();
        // optimization for ending early
        try (var tree = makeRandomBPlusTree(size, random.nextInt(), entries)) {
            repeatTest(tree, random, entries, 5, 10000, () -> {
                int rangeStart = random.nextInt(size / 1000);
                int rangeEnd = rangeStart + random.nextInt(size / 1000);
                testEnhancedIterator(tree, entries, rangeStart, rangeEnd, Integer.MAX_VALUE, Integer.MAX_VALUE);
            });
        }
    }

    /**
     * This test checks that the iterator is fast enough when only the end of the tree is needed
     */
    @Test
    @Timeout(8)
    public void testIterator_performance_3() {
        int size = 5483;
        var entries = new ArrayList<TreeEntry>();
        var random = new Random();
        // optimization for scanning near the end of the tree
        try (var tree = makeRandomBPlusTree(size, random.nextInt(), entries)) {
            repeatTest(tree, random, entries, 5, 10000, () -> {
                int rangeStart = size - size / 50 + random.nextInt(size / 1000);
                int rangeEnd = rangeStart + random.nextInt(size / 1000);
                testEnhancedIterator(tree, entries, rangeStart, rangeEnd, Integer.MAX_VALUE, Integer.MAX_VALUE);
            });
        }
    }

    /**
     * This test checks that the iterator is fast enough when only parts of the result is needed
     */
    @Test
    @Timeout(12)
    public void testIterator_performance_4() {
        int size = 5483;
        var entries = new ArrayList<TreeEntry>();
        var random = new Random();
        // Check that the solution does not compute the entire result at the start of the iterator
        // If you are also thinking of doing that, congratulations! I almost missed this case.
        try (var tree = makeRandomBPlusTree(size, random.nextInt(), entries)) {
            repeatTest(tree, random, entries, 10, 10000, () -> {
                int rangeStart = random.nextInt(size / 2);
                int rangeEnd = size - random.nextInt(size / 5);
                var iter = tree.scanEnhanced(null,
                        entries.get(rangeStart).key, entries.get(rangeEnd - 1).key,
                        Integer.MAX_VALUE, x -> true);
                int iterLimit = 1 + random.nextInt(size / 500);
                var actual = iteratorToList(iter, iterLimit);
                assertEquals(actual.size(), iterLimit);
                var expected = new ArrayList<RecordId>();
                for (int i = rangeStart; i < rangeEnd && i < rangeStart + iterLimit; i++) {
                    expected.add(entries.get(i).value);
                }
                assertEquals(expected, actual);
            });
        }
    }

    /**
     * A randomized test that applies all 3 iteration criteria and shifts them around so that
     * all modes of iteration are (hopefully) tested.
     * It also randomly deletes some tree elements and then add new ones between each iterator test.
     */
    @ParameterizedTest
    @ValueSource(ints = {5, 23, 101, 397, 1103})
    @Timeout(8)
    public void testIterator_randomized(int size) {
        var entries = new ArrayList<TreeEntry>();
        // supplying a seed to this Random object will make the whole test deterministic, which is helpful
        // when debugging
        var random = new Random();
        try (var tree = makeRandomBPlusTree(size, random.nextInt(), entries)) {
            // repeat t times
            int t = 1000;
            // this obscure arrow operator will keep decrementing t until it reaches 0 (jk)
            while (t-- > 0) {
                // determine the size of the ranges and where it starts
                var rangeSize = random.nextInt(size);
                int rangeStart = random.nextInt(size - rangeSize);
                int rangeEnd = rangeStart + rangeSize;
                // determine how many
                var iterLimit = random.nextInt(size);
                var filterLimit = random.nextInt(size);
                testEnhancedIterator(tree, entries, rangeStart, rangeEnd, iterLimit, filterLimit);
                shuffleBPlusTree(tree, random, entries);
            }
        }
    }

    /**
     * A helper utility to repeat the same test
     *
     * @param tree         The BPlusTree to test
     * @param random       A random object
     * @param entries      Entries that are in the tree
     * @param outerRep     Number of times to repeat the outer loop (a shuffle is performed between each iteration)
     * @param innerRep     Number of times to repeat the inner loop (a test function is invoked each time)
     * @param testFunction The function to execute for each test repetition
     */
    private static void repeatTest(BPlusTree tree, Random random, ArrayList<TreeEntry> entries, int outerRep, int innerRep,
                                   Runnable testFunction) {
        for (int i = 0; i < outerRep; i++) {
            for (int j = 0; j < innerRep; j++) {
                testFunction.run();
            }
            shuffleBPlusTree(tree, random, entries);
        }
    }

    /**
     * A generic helper to check the enhanced iterator in the BPlusTree. It finds the records returned by the BPlusTree
     * iterator and compares the result against the records stored in the entries list
     *
     * @param tree        The BPlusTree to be tested
     * @param entries     The keys and values of the tree stored in an ArrayList
     * @param rangeStart  Only consider keys that are greater than or equal to entries[rangeStart].key
     * @param rangeEnd    Only consider keys that are less than or equal to entries[rangeEnd].key
     * @param iterLimit   Limit on the number of elements returned by the iterator
     * @param filterLimit Only consider keys whose remainder mod entries.size() is less than or equal to filterLimit
     */
    private static void testEnhancedIterator(BPlusTree tree, ArrayList<TreeEntry> entries, int rangeStart, int rangeEnd, int iterLimit, int filterLimit) {
        var start = entries.get(rangeStart);
        rangeEnd = rangeEnd >= entries.size() ? entries.size() - 1: rangeEnd;
        var end = entries.get(rangeEnd);
        Function<DataBox, Boolean> filterFunction = (DataBox key) -> key.getInt() % entries.size() <= filterLimit;
        int endKey = end.key.getInt();
        var actual = iteratorToList(tree.scanEnhanced(null,
                // bounds are inclusive, so adjust the key accordingly
                // beware of underflow
                start.key, new IntDataBox(endKey == Integer.MIN_VALUE ? Integer.MIN_VALUE : endKey - 1),
                iterLimit,
                filterFunction));
        var expected = new ArrayList<>();
        int i = rangeStart;
        while (i < rangeEnd && expected.size() < iterLimit) {
            if (filterFunction.apply(entries.get(i).key)) {
                expected.add(entries.get(i).value);
            }
            i++;
        }
        assertEquals(expected, actual);
    }
}
