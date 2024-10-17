package edu.umd.cs424.database.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.io.PageAllocator;
import edu.umd.cs424.database.table.RecordId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(1)
public class TestBPlusNode {
    public static final String testFile = "TestBPlusNode";

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempFolder;

    private BPlusTreeMetadata getBPlusTreeMetadata(Type keySchema, int order)
    throws IOException {
        Path file = Files.createFile(tempFolder.resolve(testFile));
        var allocator = new PageAllocator(file.toAbsolutePath().toString(), false, null);
        return new BPlusTreeMetadata(allocator, keySchema, order);
    }

    @Test
    public void testFromBytes() throws IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);

        // Leaf node.
        var leafKeys = new ArrayList<DataBox>();
        var leafRids = new ArrayList<RecordId>();
        for (int i = 0; i < 2 * d; ++i) {
            leafKeys.add(new IntDataBox(i));
            leafRids.add(new RecordId(i, (short) i));
        }
        var leaf = new LeafNode(meta, leafKeys, leafRids, Optional.of(42), null);
        int leafPageNum = leaf.getPage().getPageNum();

        // Inner node.
        var innerKeys = new ArrayList<DataBox>();
        var innerChildren = new ArrayList<Integer>();
        for (int i = 0; i < 2 * d; ++i) {
            innerKeys.add(new IntDataBox(i));
            // We use the same page for all children here just for convenience, this should
            // not happen in an actual B+ tree.
            innerChildren.add(leafPageNum);
        }
        innerChildren.add(leafPageNum);
        var inner = new InnerNode(meta, innerKeys, innerChildren, null);

        int innerPageNum = inner.getPage().getPageNum();
        assertEquals(leaf, BPlusNode.fromBytes(null, meta, leafPageNum));
        assertEquals(inner, BPlusNode.fromBytes(null, meta, innerPageNum));
    }
}
