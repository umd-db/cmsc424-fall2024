package edu.umd.cs424.database.io;

import static org.junit.Assert.*;

import edu.umd.cs424.database.categories.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.File;

/**
* Tests LRU Cache; should be optional test for students
* @author  Sammy Sidhu
* @version 1.0
*/

@Category({HW99Tests.class, SystemTests.class})
public class TestLRUCache {
    private final String fName = "TestLRUCache.temp";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLRUCache() throws IOException {
        File tempFile = tempFolder.newFile(fName);
        FileChannel fc = new RandomAccessFile(tempFile, "rw").getChannel();
        Page p = new Page(fc, 0, 0);
        LRUCache<Long, Page> l = new LRUCache<>(10);
        for (long i = 0; i < 10; i++) {
            l.put(i, p);
            assertEquals(i + 1, l.size());
        }
        for (long i = 10; i < 20; i++) {
            l.put(i, p);
            assertEquals(10, l.size());
            assertTrue(l.containsKey(i));
            assertFalse(l.containsKey(i - 10));
        }
    }
}
