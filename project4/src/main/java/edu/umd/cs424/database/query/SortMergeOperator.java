package edu.umd.cs424.database.query;

import java.util.*;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.RecordIterator;

public class SortMergeOperator extends JoinOperator {
    public SortMergeOperator(QueryOperator leftSource,
                             QueryOperator rightSource,
                             String leftColumnName,
                             String rightColumnName,
                             Database.Transaction transaction) throws QueryPlanException, DatabaseException {
        super(leftSource, rightSource, leftColumnName, rightColumnName, transaction, JoinType.SORTMERGE);

        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
    }

    public Iterator<Record> iterator() throws QueryPlanException, DatabaseException {
        return new SortMergeIterator();
    }

    public int estimateIOCost() throws QueryPlanException {
        //does nothing
        return 0;
    }

    /**
     * An implementation of Iterator that provides an iterator interface for this operator.
     *
     * Before proceeding, you should read and understand SNLJOperator.java
     *    You can find it in the same directory as this file.
     *
     * Word of advice: try to decompose the problem into distinguishable sub-problems.
     *    This means you'll probably want to add more methods than those given (Once again,
     *    SNLJOperator.java might be a useful reference).
     *
     */
    private class SortMergeIterator extends JoinIterator {
        /**
        * Some member variables are provided for guidance, but there are many possible solutions.
        * You should implement the solution that's best for you, using any member variables you need.
        * You're free to use these member variables, but you're not obligated to.
        */

        private RecordIterator leftSourceIterator;
        private RecordIterator rightSourceIterator;
        private Record leftRecord;
        private Record nextRecord;
        private Record rightRecord;
        private LR_RecordComparator lr_comparator = new LR_RecordComparator();

        public SortMergeIterator() throws QueryPlanException, DatabaseException {
            super();
            SortOperator l = new SortOperator(
                SortMergeOperator.this.getTransaction(), this.getLeftTableName(), new LeftRecordComparator());
            SortOperator r = new SortOperator(
                SortMergeOperator.this.getTransaction(), this.getRightTableName(), new RightRecordComparator());

            this.leftSourceIterator = SortMergeOperator.this.getRecordIterator(l.sort());
            if (this.leftSourceIterator.hasNext()) {
                this.leftRecord = this.leftSourceIterator.next();
            }

            this.rightSourceIterator = SortMergeOperator.this.getRecordIterator(r.sort());
            if (this.rightSourceIterator.hasNext()) {
                this.rightRecord = this.rightSourceIterator.next();
                this.rightSourceIterator.mark(); 
            }
        }

        private Record fetchNextRecord() {
            if (leftRecord == null) {
                return null;
            }
            
            while (true) {
                if (this.rightRecord != null) {
                    // Advance the right record until it is greater than or equal to the left record.
                    // Mark the right source such that we can later reset to the last right record that
                    // is less than the left record.
                    while (this.rightSourceIterator.hasNext() && lr_comparator.compare(this.leftRecord, this.rightRecord) > 0) {
                        this.rightSourceIterator.mark();
                        this.rightRecord = this.rightSourceIterator.next();
                    } 
                }
                
                if (this.rightRecord != null && lr_comparator.compare(this.leftRecord, this.rightRecord) == 0) {
                    // Join the left and right record and return the result. The right record must be
                    // advanced before returning. If there is no more right record, set it to null.
                    throw new UnsupportedOperationException("Implement this.");
                } else if (this.leftSourceIterator.hasNext()) {
                    // Advance the left record by one and reset the right record.
                    throw new UnsupportedOperationException("Implement this.");
                } else {
                    return null;
                }
            }
        }

        /**
         * Checks if there are more record(s) to yield
         *
         * @return true if this iterator has another record to yield, otherwise false
         */
        public boolean hasNext() {
            if (this.nextRecord == null) this.nextRecord = fetchNextRecord();
            return this.nextRecord != null;
        }

        /**
         * Yields the next record of this iterator.
         *
         * @return the next Record
         * @throws NoSuchElementException if there are no more Records to yield
         */
        public Record next() {
            if (!this.hasNext()) throw new NoSuchElementException();
            Record nextRecord = this.nextRecord;
            this.nextRecord = null;
            return nextRecord;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private class LeftRecordComparator implements Comparator<Record> {
            public int compare(Record o1, Record o2) {
                return o1.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).compareTo(
                           o2.getValues().get(SortMergeOperator.this.getLeftColumnIndex()));
            }
        }

        private class RightRecordComparator implements Comparator<Record> {
            public int compare(Record o1, Record o2) {
                return o1.getValues().get(SortMergeOperator.this.getRightColumnIndex()).compareTo(
                           o2.getValues().get(SortMergeOperator.this.getRightColumnIndex()));
            }
        }

        /**
        * Left-Right Record comparator
        * o1 : leftRecord
        * o2: rightRecord
        */
        private class LR_RecordComparator implements Comparator<Record> {
            public int compare(Record o1, Record o2) {
                return o1.getValues().get(SortMergeOperator.this.getLeftColumnIndex()).compareTo(
                           o2.getValues().get(SortMergeOperator.this.getRightColumnIndex()));
            }
        }
    }
}
