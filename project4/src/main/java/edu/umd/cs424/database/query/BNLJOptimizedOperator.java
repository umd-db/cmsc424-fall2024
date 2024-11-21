package edu.umd.cs424.database.query;

import java.util.*;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.common.BacktrackingIterator;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.io.PageAllocator.PageIterator;
import edu.umd.cs424.database.table.Record;

public class BNLJOptimizedOperator extends JoinOperator {
    private int numBuffers;

    public BNLJOptimizedOperator(QueryOperator leftSource,
                        QueryOperator rightSource,
                        String leftColumnName,
                        String rightColumnName,
                        Database.Transaction transaction) throws QueryPlanException, DatabaseException {
        super(leftSource, rightSource, leftColumnName, rightColumnName, transaction, JoinType.BNLJOPTIMIZED);

        this.numBuffers = transaction.getNumMemoryPages();

        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
    }

    public Iterator<Record> iterator() throws QueryPlanException, DatabaseException {
        return new BNLJOptimizedIterator();
    }

    public int estimateIOCost() {
        //This method implements the the IO cost estimation of the Optimized Block Nested Loop Join

        int usableBuffers = numBuffers -
                            2; //Common mistake have to first calculate the number of usable buffers

        int numLeftPages = getLeftSource().getStats().getNumPages();

        int numRightPages = getRightSource().getStats().getNumPages();

        return ((int) Math.ceil((double) numLeftPages / (double) usableBuffers)) * numRightPages +
               numLeftPages;

    }

    /**
     * Optimized Block Nested Loop Join
     * See the second bullet point at the end of Section 15.5.2 in the textbook.
     *
     * A record iterator that executes the logic for an optimized block nested loop join.
     * Look over the implementation in SNLJOperator if you want to get a feel
     * for the fetchNextRecord() logic.
     */
     private class BNLJOptimizedIterator extends JoinIterator {
        // Iterator over all the pages of the left source
        private PageIterator leftSourceIterator;
        // Iterator over all the pages of the right source
        private PageIterator rightSourceIterator;
        // Iterator over records in the current left page
        private BacktrackingIterator<Record> leftRecordPerPageIterator;
        // Iterator over records in the current right page
        private BacktrackingIterator<Record> rightRecordPerPageIterator;
        // The current record from the left relation
        private Record leftRecord;
        // The next record to return
        private Record nextRecord;

        private BNLJOptimizedIterator() throws QueryPlanException, DatabaseException {
            super();
            this.leftSourceIterator = BNLJOptimizedOperator.this.getPageIterator(this.getLeftTableName());
            this.leftSourceIterator.next();
            this.leftRecordPerPageIterator = null;
            this.leftRecord = null;
            this.fetchNextLeftPage();

            this.rightSourceIterator = BNLJOptimizedOperator.this.getPageIterator(this.getRightTableName());
            this.rightSourceIterator.next();
            this.rightRecordPerPageIterator = null;
            if (this.rightSourceIterator.hasNext()) {
                this.rightSourceIterator.next();
                this.rightSourceIterator.mark();
                this.rightSourceIterator.reset();
            }
            this.fetchNextRightPage();

            this.nextRecord = null;
        }

        /**
         * Fetch the next block of records from the left table.
         * leftRecordPerPageIterator should be set to a backtracking iterator over up to
         * numBuffers-2 pages of records from the left table, and leftRecord should be
         * set to the first record in this block.
         *
         * If there are no more pages in the left table, this method should
         * do nothing.
         *
         * You may find BNLJOptimizedOperator.this.getBlockIterator() useful here.
         */
        private void fetchNextLeftPage() throws DatabaseException {
            throw new UnsupportedOperationException("Implement this.");
        }

        /**
         * Fetch the next page of records from the right table.
         * rightRecordPerPageIterator should be set to a backtracking iterator over up to
         * one page of records from the right table, and is marked such that the next
         * time reset() is called, a subsequent next() call will return the first record
         * in this page.
         *
         * If there are no more pages in the right table, this method should
         * do nothing.
         *
         * You may find BNLJOptimizedOperator.this.getBlockIteratoruseful here.
         */
        private void fetchNextRightPage() throws DatabaseException {
            throw new UnsupportedOperationException("Implement this.");
        }

        /**
         * Returns the next record that should be yielded from this join,
         * or null if there are no more records to join.
         */
        private Record fetchNextRecord() throws DatabaseException {
            throw new UnsupportedOperationException("Implement this.");
        }

        /**
         * @return true if this iterator has another record to yield, otherwise
         * false
         */
        @Override
        public boolean hasNext() {
            try {
                if (this.nextRecord == null) this.nextRecord = fetchNextRecord();
                return this.nextRecord != null;
            } catch (DatabaseException e) {
                return false;
            }
        }

        /**
         * @return the next record from this iterator
         * @throws NoSuchElementException if there are no more records to yield
         */
        @Override
        public Record next() {
            if (!this.hasNext()) throw new NoSuchElementException();
            Record nextRecord = this.nextRecord;
            this.nextRecord = null;
            return nextRecord;
        }
    }
}
