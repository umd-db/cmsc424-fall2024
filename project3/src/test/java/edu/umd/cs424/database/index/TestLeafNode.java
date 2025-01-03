package edu.umd.cs424.database.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs424.database.common.Pair;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.io.PageAllocator;
import edu.umd.cs424.database.table.RecordId;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(1)
public class TestLeafNode {
    public static final String testFile = "TestLeafNode";

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempFolder;

    private static DataBox d0 = new IntDataBox(0);
    private static DataBox d1 = new IntDataBox(1);
    private static DataBox d2 = new IntDataBox(2);
    private static DataBox d3 = new IntDataBox(3);
    private static DataBox d4 = new IntDataBox(4);

    private static RecordId r0 = new RecordId(0, (short) 0);
    private static RecordId r1 = new RecordId(1, (short) 1);
    private static RecordId r2 = new RecordId(2, (short) 2);
    private static RecordId r3 = new RecordId(3, (short) 3);
    private static RecordId r4 = new RecordId(4, (short) 4);

    // Helpers /////////////////////////////////////////////////////////////////
    private BPlusTreeMetadata getBPlusTreeMetadata(Type keySchema, int order)
    throws IOException {
        Path file = Files.createFile(tempFolder.resolve(testFile));
        String path = file.toAbsolutePath().toString();
        var allocator = new PageAllocator(path, false, null);
        return new BPlusTreeMetadata(allocator, keySchema, order);
    }

    private LeafNode getEmptyLeaf(BPlusTreeMetadata meta,
                                  Optional<Integer> rightSibling) {
        var keys = new ArrayList<DataBox>();
        var rids = new ArrayList<RecordId>();
        return new LeafNode(meta, keys, rids, rightSibling, null);
    }

    // Tests ///////////////////////////////////////////////////////////////////
    @Test
    public void testGetL() throws IOException {
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), 5);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());
        for (int i = 0; i < 10; ++i) {
            assertEquals(leaf, leaf.get(null, new IntDataBox(i)));
        }
    }

    @Test
    public void testGetLeftmostLeaf() throws IOException {
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), 5);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());
        assertEquals(leaf, leaf.getLeftmostLeaf(null));
    }

    @Test
    public void testBulkLoad_small() throws BPlusTreeException, IOException {
        // Bulk loads with 60% of a leaf's worth, then checks that the
        // leaf didn't split.
        int d = 5;
        float fillFactor = 0.8f;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        var data = new ArrayList<Pair<DataBox, RecordId>>();
        for (int i = 0; i < (int) Math.ceil(1.5 * d * fillFactor); ++i) {
            DataBox key = new IntDataBox(i);
            var rid = new RecordId(i, (short) i);
            data.add(i, new Pair<>(key, rid));
        }

        assertFalse(leaf.bulkLoad(null, data.iterator(), fillFactor).isPresent());

        Iterator<RecordId> iter = leaf.scanAll();
        Iterator<Pair<DataBox, RecordId>> expected = data.iterator();
        while (iter.hasNext() && expected.hasNext()) {
            assertEquals(expected.next().getSecond(), iter.next());
        }
        assertFalse(iter.hasNext());
        assertFalse(expected.hasNext());
    }

    @Test
    public void testPut_noOverflow() throws BPlusTreeException, IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        for (int i = 0; i < 2 * d; ++i) {
            DataBox key = new IntDataBox(i);
            var rid = new RecordId(i, (short) i);
            assertEquals(Optional.empty(), leaf.put(null, key, rid));

            for (int j = 0; j <= i; ++j) {
                key = new IntDataBox(j);
                rid = new RecordId(j, (short) j);
                assertEquals(Optional.of(rid), leaf.getKey(key));
            }
        }
    }

    @Test
    public void testPut_noOverflowFromDisk()
    throws BPlusTreeException, IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        // Populate the leaf.
        for (int i = 0; i < 2 * d; ++i) {
            leaf.put(null, new IntDataBox(i), new RecordId(i, (short) i));
        }

        // Then read the leaf from disk.
        int pageNum = leaf.getPage().getPageNum();
        LeafNode fromDisk = LeafNode.fromBytes(null, meta, pageNum);

        // Check to see that we can read from disk.
        for (int i = 0; i < 2 * d; ++i) {
            var key = new IntDataBox(i);
            var rid = new RecordId(i, (short) i);
            assertEquals(Optional.of(rid), fromDisk.getKey(key));
        }
    }

    @Test
    public void testPut_duplicate() throws BPlusTreeException, IOException {
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), 4);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        // The initial insert is fine.
        leaf.put(null, new IntDataBox(0), new RecordId(0, (short) 0));

        // The duplicate insert should raise an exception.
        Assertions.assertThrows(BPlusTreeException.class,
                () -> leaf.put(null, new IntDataBox(0), new RecordId(0, (short) 0)));
    }

    @Test
    public void testRemove_simple() throws BPlusTreeException, IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        // Insert entries.
        for (int i = 0; i < 2 * d; ++i) {
            var key = new IntDataBox(i);
            var rid = new RecordId(i, (short) i);
            leaf.put(null, key, rid);
            assertEquals(Optional.of(rid), leaf.getKey(key));
        }

        // Remove entries.
        for (int i = 0; i < 2 * d; ++i) {
            var key = new IntDataBox(i);
            leaf.remove(null, key);
            assertEquals(Optional.empty(), leaf.getKey(key));
        }
    }

    @Test
    public void testScanAll() throws BPlusTreeException, IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        // Insert tuples in reverse order to make sure that scanAll is returning
        // things in sorted order.
        for (int i = 2 * d - 1; i >= 0; --i) {
            leaf.put(null, new IntDataBox(i), new RecordId(i, (short) i));
        }

        Iterator<RecordId> iter = leaf.scanAll();
        for (int i = 0; i < 2 * d; ++i) {
            assertTrue(iter.hasNext());
            assertEquals(new RecordId(i, (short) i), iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMaxOrder() {
        // Note that this white box test depend critically on the implementation
        // of toBytes and includes a lot of magic numbers that won't make sense
        // unless you read toBytes.
        assertEquals(4, Type.intType().getSizeInBytes());
        assertEquals(6, RecordId.getSizeInBytes());
        for (int d = 0; d < 10; ++d) {
            int dd = d + 1;
            for (int i = 9 + (2 * d) * (4 + 6); i < 9 + (2 * dd) * (4 + 6); ++i) {
                assertEquals(d, LeafNode.maxOrder(i, Type.intType()));
            }
        }
    }

    @Test
    public void testToSexp() throws BPlusTreeException, IOException {
        int d = 2;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);
        LeafNode leaf = getEmptyLeaf(meta, Optional.empty());

        assertEquals("()", leaf.toSexp(null));
        leaf.put(null, new IntDataBox(4), new RecordId(4, (short) 4));
        assertEquals("((4 (4 4)))", leaf.toSexp(null));
        leaf.put(null, new IntDataBox(1), new RecordId(1, (short) 1));
        assertEquals("((1 (1 1)) (4 (4 4)))", leaf.toSexp(null));
        leaf.put(null, new IntDataBox(2), new RecordId(2, (short) 2));
        assertEquals("((1 (1 1)) (2 (2 2)) (4 (4 4)))", leaf.toSexp(null));
        leaf.put(null, new IntDataBox(3), new RecordId(3, (short) 3));
        assertEquals("((1 (1 1)) (2 (2 2)) (3 (3 3)) (4 (4 4)))", leaf.toSexp(null));
    }

    @Test
    public void testToAndFromBytes() throws BPlusTreeException, IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);

        var keys = new ArrayList<DataBox>();
        var rids = new ArrayList<RecordId>();
        var leaf = new LeafNode(meta, keys, rids, Optional.of(42), null);
        int pageNum = leaf.getPage().getPageNum();

        assertEquals(leaf, LeafNode.fromBytes(null, meta, pageNum));

        for (int i = 0; i < 2 * d; ++i) {
            leaf.put(null, new IntDataBox(i), new RecordId(i, (short) i));
            assertEquals(leaf, LeafNode.fromBytes(null, meta, pageNum));
        }
    }
}
