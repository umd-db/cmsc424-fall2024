package edu.umd.cs424.database.query;

import edu.umd.cs424.database.Database;
import edu.umd.cs424.database.DatabaseException;
import edu.umd.cs424.database.common.Pair;
import edu.umd.cs424.database.io.Page;
import edu.umd.cs424.database.table.Record;
import edu.umd.cs424.database.table.Schema;
import edu.umd.cs424.database.table.Table;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class HashJoinOperator extends JoinOperator {
    final private BiFunction<Integer, Integer, Integer> hashBiFunction;
    // List of pairs of Runs. In each pair, the first Run contains some elements from the left table that
    // fall into a certain bucket. The second Run contains some elements from the right table that fall into
    // the same bucket.
    final private ArrayList<Pair<Run, Run>> buckets;
    final private Schema schemaLeft, schemaRight;
    // The maximum number of elements that can be present in a bucket.
    private int bucketSizeLimit;
    // The number of Runs to create whenever there is an overflow and the current Run needs to be subdivided.
    private int numSubRuns;
    // Random number generator for the hash function.
    private final Random random;

    public HashJoinOperator(QueryOperator leftSource,
                            QueryOperator rightSource,
                            String leftColumnName,
                            String rightColumnName,
                            Database.Transaction transaction,
                            BiFunction<Integer, Integer, Integer> hash) throws QueryPlanException, DatabaseException {
        super(leftSource, rightSource, leftColumnName, rightColumnName, transaction, JoinType.HASHJOIN);

        this.stats = this.estimateStats();
        this.cost = this.estimateIOCost();
        this.hashBiFunction = hash;
        buckets = new ArrayList<>();
        numSubRuns = transaction.getNumMemoryPages() - 1;
        schemaLeft = leftSource.computeSchema();
        schemaRight = rightSource.computeSchema();
        int recordsPerPage = Table.computeNumRecordsPerPage(Page.pageSize, schemaLeft);
        bucketSizeLimit = (int)((transaction.getNumMemoryPages() - 1) * (recordsPerPage) * 0.8);

        random = new Random(42);
    }

    /**
     * For testing purposes only. Do not call or modify.
     */
    public void setBucketSizeLimit(int bucketSizeLimit) {
        this.bucketSizeLimit = bucketSizeLimit;
    }

    /**
     * For testing purposes only. Do not call or modify.
     */
    public void setNumSubRuns(int numSubRuns) {
        this.numSubRuns = numSubRuns;
    }

    public Iterator<Record> iterator() throws QueryPlanException, DatabaseException {
        return new HashJoinIterator();
    }

    public int estimateIOCost() throws QueryPlanException {
        //does nothing
        return 0;
    }

    /**
     * Use this method to create a Run instead of <code>new Run(...)</code>
     * @param isLeft Whether this is on the left table.
     * @return An empty new Run.
     */
    private Run createRun(boolean isLeft) throws DatabaseException {
        return new Run(this.getTransaction(), (isLeft ? schemaLeft : schemaRight));
    }

    /**
     * Given an iterator of records, distribute them into <code>numSubRuns</code> Runs. The Run is determined
     * by calling a hash function and then performing (modulo numSubRuns) on the returned value.
     *
     * @param recordIterator An iterator through all the records that need to be distributed.
     * @param hashFunction A hash function that returns an integer hash when supplied with an integer. Note that it
     *                     may return a negative value, so you should take this into account.
     * @param isLeft Whether this is done on the left or right table. You can ignore this parameter.
     * @return An array list of Runs that are similar in size if the hash function is sufficiently "random".
     */
    private ArrayList<Run> createRuns(Iterator<Record> recordIterator,
                                      Function<Integer, Integer> hashFunction,
                                      boolean isLeft) throws DatabaseException {
        // Create numSubRuns Runs
        var result = new ArrayList<Run>();
        for (var i = 0; i < numSubRuns; i++) {
            result.add(createRun(isLeft));
        }
        // This value tells you which column stores the value you are joining on.
        int columnIndex = isLeft ? this.getLeftColumnIndex() : this.getRightColumnIndex();
        // Your task: distribute each record into a Run. The Run is determined by the hash function and the
        //  value at columnIndex.
        throw new UnsupportedOperationException("implement this");
    }

    /**
     * This is a recursive function that distributes records into buckets according to a hash function. It recursively
     * reduces the size of a Run by breaking it into multiple smaller Runs. Once a Run on the left table is small
     * enough, it is added to this.buckets along with the corresponding Run on the right.
     *
     * @param recordList1 An iterator for a Run of records on the left table.
     * @param recordList2 An iterator for a Run of records on the right table.
     */
    private void prepareHashJoin(Iterator<Record> recordList1, Iterator<Record> recordList2) throws DatabaseException {
        // Create a hash function.
        int seed = random.nextInt();
        Function<Integer, Integer> hashFunction = x -> this.hashBiFunction.apply(seed, x);
        // Break the two lists into multiple lists/runs
        var runsLeft = createRuns(recordList1, hashFunction, true);
        var runsRight = createRuns(recordList2, hashFunction, false);
        /*
         * For Run i, there are two cases
         * (1) The size of the Run on the left is greater than bucketSizeLimit. In this case, we have an overflow
         *     and need to call this function recursively to break this Run in the left into smaller Runs. The
         *     corresponding Run on the right must be broken down as well.
         * (2) The size of the Run on the left is less than or equal to bucketSizeLimit. We can add this pair of
         *     Runs to this.buckets, which is the base case in the recursion.
         */
        throw new UnsupportedOperationException("implement this");
    }

    /**
     * Set up <code>this.buckets</code> so that the iterator can go through each pair of runs.
     */
    private void prepareHashJoin() throws QueryPlanException, DatabaseException {
        prepareHashJoin(this.getLeftSource().iterator(), this.getRightSource().iterator());
    }

    /**
     * An implementation of Iterator that provides an iterator interface for this operator.
     */
    private class HashJoinIterator extends JoinIterator {
        /**
        * Some member variables are provided for guidance, but there are many possible solutions.
        * You should implement the solution that's best for you, using any member variables you need.
        * You're free to use these member variables, but you're not obligated to.
        */
        int bucketIndex = -1;
        Iterator<Record> currentBucket = Collections.emptyIterator();
        Record cachedRecord = null;
        int leftColumnIndex, rightColumnIndex;

        /**
         * Join two Runs and store the result as an iterator in <code>currentBucket</code>. In the interest of
         * not overcomplicating the project, we allow you to cut some corners here and generate all matches at once.
         * <p>
         * You are welcome to implement it your way if you so wish.
         *
         * @param left A Run from the left table
         * @param right A Run from the right table that is in the same hash bucket as <code>left</code>.
         */
        void joinRuns(Run left, Run right) {
            ArrayList<Record> result = new ArrayList<>();
            // This is intended to map the join attribute to a list of records that all have the same join attribute.
            Map<Integer, ArrayList<Record>> map = new HashMap<>();
            /*
             * In the reference implementation, records on the left are stored in a HashMap for fast look up.
             * The join attribute of each record on the right is then queried on the map for matches. Each pair
             * of matches is added to the result array list. In the interest of not overcomplicating the project,
             * we allow you to cut some corners here and generate all matches at once and then store them as
             * an iterator.
             *
             * As usual, you are welcome to implement this function your way.
             */
            throw new UnsupportedOperationException("implement this");

            // Uncomment the next line once you are done populating the result array list.
            // currentBucket = result.iterator();
        }

        public HashJoinIterator() throws QueryPlanException, DatabaseException {
            prepareHashJoin();
            leftColumnIndex = HashJoinOperator.this.getLeftColumnIndex();
            rightColumnIndex = HashJoinOperator.this.getRightColumnIndex();
        }

        /**
         * Move to the next bucket (usually called when the current bucket no longer has matches left).
         *
         * @return True if we moved successfully to the next bucket. False if there are no more buckets left.
         */
        private boolean nextBucket() {
            bucketIndex++;
            if (bucketIndex >= buckets.size()) {
                return false;
            }
            var pair = buckets.get(bucketIndex);
            joinRuns(pair.getFirst(), pair.getSecond());
            return true;
        }

        private Record nextRecord() {
            if (currentBucket.hasNext()) {
                return currentBucket.next();
            }
            if (nextBucket()) {
                return nextRecord();
            }
            return null;
        }

        /**
         * Checks if there are more record(s) to yield
         *
         * @return true if this iterator has another record to yield, otherwise false
         */
        public boolean hasNext() {
            if (cachedRecord == null) {
                this.cachedRecord = nextRecord();
            }
            return this.cachedRecord != null;
        }

        /**
         * Yields the next record of this iterator.
         *
         * @return the next Record
         * @throws NoSuchElementException if there are no more Records to yield
         */
        public Record next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Record record = cachedRecord;
            cachedRecord = nextRecord();
            return record;
        }

    }
}
