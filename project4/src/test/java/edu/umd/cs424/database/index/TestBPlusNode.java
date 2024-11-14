package edu.umd.cs424.database.index;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.umd.cs424.database.TimeoutScaling;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import edu.umd.cs424.database.categories.*;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.databox.IntDataBox;
import edu.umd.cs424.database.databox.Type;
import edu.umd.cs424.database.io.PageAllocator;
import edu.umd.cs424.database.table.RecordId;

@Category(HW2Tests.class)
public class TestBPlusNode {
    public static final String testFile = "TestBPlusNode";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // 1 seconds max per method tested.
    @Rule
    public TestRule globalTimeout = new DisableOnDebug(Timeout.millis((long) (1000 * TimeoutScaling.factor)));

    private BPlusTreeMetadata getBPlusTreeMetadata(Type keySchema, int order)
    throws IOException {
        File file = tempFolder.newFile(testFile);
        PageAllocator allocator = new PageAllocator(file.getAbsolutePath(), false, null);
        return new BPlusTreeMetadata(allocator, keySchema, order);
    }

    @Test
    @Category(PublicTests.class)
    public void testFromBytes() throws IOException {
        int d = 5;
        BPlusTreeMetadata meta = getBPlusTreeMetadata(Type.intType(), d);

        // Leaf node.
        List<DataBox> leafKeys = new ArrayList<>();
        List<RecordId> leafRids = new ArrayList<>();
        for (int i = 0; i < 2 * d; ++i) {
            leafKeys.add(new IntDataBox(i));
            leafRids.add(new RecordId(i, (short) i));
        }
        LeafNode leaf = new LeafNode(meta, leafKeys, leafRids, Optional.of(42), null);

        // Inner node.
        List<DataBox> innerKeys = new ArrayList<>();
        List<Integer> innerChildren = new ArrayList<>();
        for (int i = 0; i < 2 * d; ++i) {
            innerKeys.add(new IntDataBox(i));
            innerChildren.add(i);
        }
        innerChildren.add(2 * d);
        InnerNode inner = new InnerNode(meta, innerKeys, innerChildren, null);

        int leafPageNum = leaf.getPage().getPageNum();
        int innerPageNum = inner.getPage().getPageNum();
        assertEquals(leaf, BPlusNode.fromBytes(null, meta, leafPageNum));
        assertEquals(inner, BPlusNode.fromBytes(null, meta, innerPageNum));
    }
}
