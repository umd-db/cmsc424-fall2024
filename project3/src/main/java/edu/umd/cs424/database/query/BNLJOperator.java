package edu.umd.cs424.database.query;

import java.util.*;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.table.Record;

public class BNLJOperator extends JoinOperator {
    private int numBuffers;

    public BNLJOperator(QueryOperator leftSource,
                        QueryOperator rightSource,
                        String leftColumnName,
                        String rightColumnName,
                        Database.Transaction transaction) throws QueryPlanException, DatabaseException {
        super(leftSource, rightSource, leftColumnName, rightColumnName, transaction, JoinType.BNLJ);

        this.numBuffers = transaction.getNumMemoryPages();

        // for HW4
        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
    }

    public Iterator<Record> iterator() throws QueryPlanException, DatabaseException {
        return new BNLJIterator();
    }

    public int estimateIOCost() {
        //This method implements the the IO cost estimation of the Block Nested Loop Join

        int usableBuffers = numBuffers -
                            2; //Common mistake have to first calculate the number of usable buffers

        int numLeftPages = getLeftSource().getStats().getNumPages();

        int numRightPages = getRightSource().getStats().getNumPages();

        return ((int) Math.ceil((double) numLeftPages / (double) usableBuffers)) * numRightPages +
               numLeftPages;

    }

    /**
     * BNLJ: Block Nested Loop Join
     *  See lecture slides.
     *
     * An implementation of Iterator that provides an iterator interface for this operator.
     *
     * Before proceeding, you should read and understand SNLJOperator.java
     *    You can find it in the same directory as this file.
     *
     * Word of advice: try to decompose the problem into distinguishable sub-problems.
     *    This means you'll probably want to add more methods than those given (Once again,
     *    SNLJOperator.java might prove to be a useful reference).
     */

    private class BNLJIterator extends JoinIterator {
        public BNLJIterator() throws QueryPlanException, DatabaseException {
            super();
            return;
        }

        /**
         * Checks if there are more record(s) to yield
         *
         * @return true if this iterator has another record to yield, otherwise false
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * Yields the next record of this iterator.
         *
         * @return the next Record
         * @throws NoSuchElementException if there are no more Records to yield
         */
        public Record next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
