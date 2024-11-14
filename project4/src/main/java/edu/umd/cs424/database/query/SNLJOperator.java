package edu.umd.cs424.database.query;

import java.util.*;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.databox.DataBox;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.RecordIterator;

/**
 * Performs an equijoin between two relations on leftColumnName and
 * rightColumnName respectively using the Simple Nested Loop Join algorithm.
 */
public class SNLJOperator extends JoinOperator {
    public SNLJOperator(QueryOperator leftSource,
                        QueryOperator rightSource,
                        String leftColumnName,
                        String rightColumnName,
                        Database.Transaction transaction) throws QueryPlanException, DatabaseException {
        super(leftSource,
              rightSource,
              leftColumnName,
              rightColumnName,
              transaction,
              JoinType.SNLJ);

        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
    }

    public Iterator<Record> iterator() throws QueryPlanException, DatabaseException {
        return new SNLJIterator();
    }

    public int estimateIOCost() throws QueryPlanException {
        int numLeftRecords = getLeftSource().getStats().getNumRecords();

        int numRightPages = getRightSource().getStats().getNumPages();
        int numLeftPages = getLeftSource().getStats().getNumPages();

        return numLeftRecords * numRightPages + numLeftPages;
    }

    /**
     * A record iterator that executes the logic for a simple nested loop join.
     * Note that the left table is the "outer" loop and the right table is the
     * "inner" loop.
     */
    private class SNLJIterator extends JoinIterator {
        // Iterator over all the records of the left relation
        private RecordIterator leftSourceIterator;
        // Iterator over all the records of the right relation
        private RecordIterator rightSourceIterator;
        // The current record from the left relation
        private Record leftRecord;
        // The next record to return
        private Record nextRecord;

        public SNLJIterator() throws QueryPlanException, DatabaseException {
            super();
            this.leftSourceIterator = SNLJOperator.this.getRecordIterator(this.getLeftTableName());
            if (this.leftSourceIterator.hasNext()) {
                this.leftRecord = this.leftSourceIterator.next();
            }

            this.rightSourceIterator = SNLJOperator.this.getRecordIterator(this.getRightTableName());
            if (this.rightSourceIterator.hasNext()) {
                this.rightSourceIterator.next();
                this.rightSourceIterator.mark();
                this.rightSourceIterator.reset();
            }
        }

        /**
         * Returns the next record that should be yielded from this join,
         * or null if there are no more records to join.
         */
        private Record fetchNextRecord() {
            if (leftRecord == null) {
                // The left source was empty, nothing to fetch
                return null;
            }
            while (true) {
                if (this.rightSourceIterator.hasNext()) {
                    // there's a next right record, join it if there's a match
                    Record rightRecord = rightSourceIterator.next();

                    DataBox leftJoinValue = leftRecord.getValues().get(SNLJOperator.this.getLeftColumnIndex());
                    DataBox rightJoinValue = rightRecord.getValues().get(SNLJOperator.this.getRightColumnIndex());
                    if (leftJoinValue.equals(rightJoinValue)) {
                        List<DataBox> leftValues = new ArrayList<>(this.leftRecord.getValues());
                        List<DataBox> rightValues = new ArrayList<>(rightRecord.getValues());
                        leftValues.addAll(rightValues);
                        return new Record(leftValues);
                    }
                } else if (leftSourceIterator.hasNext()){
                    // there's no more right records but there's still left
                    // records. Advance left and reset right
                    this.leftRecord = leftSourceIterator.next();
                    this.rightSourceIterator.reset();
                } else {
                    // if you're here then there are no more records to fetch
                    return null;
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (this.nextRecord == null) this.nextRecord = fetchNextRecord();
            return this.nextRecord != null;
        }

        @Override
        public Record next() {
            if (!this.hasNext()) throw new NoSuchElementException();
            Record nextRecord = this.nextRecord;
            this.nextRecord = null;
            return nextRecord;
        }
    }
}
