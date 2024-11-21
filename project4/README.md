# Project 4: Implementing Join Algorithms

## Environment

We will be using the same environment as the previous project.

Again, for this project, we highly recommend you install/use an IDE like [Eclipse](https://www.eclipse.org/downloads/). In Eclipse, import this project with: File > import > maven > existing maven project.

Alternatively, you can run the project within a Docker environment, which can be started with the following command. Make sure that the current directory is the directory of the project:

```bash
docker run -v $PWD:/project4 -ti --rm -w /project4 maven:3.9.0-eclipse-temurin-17-alpine /bin/bash
```

In the terminal of the Docker container, you can compile and run the tests with the following commands:

```bash
    # build code without testing
    mvn compile
    # build code and run unit tests
    mvn test
```

## The Project Files

In the `src/main/java/edu/umd/cs424/database` directory, you will find all of the
code we have provided to you.

### Getting Familiar with the Release Code

Navigate to the `src/main/java/edu/umd/cs424/database` directory. You
will find six directories: `common`, `databox`, `io`, `table`, `index`, and `query`, and two files, `Database` and `DatabaseException`.
You do not have to deeply understand all of the code, but it's worth becoming a little
familiar with it. **In this assignment, though, you may only modify files in
the `query` and `table` directories**.

### common

The `common` directory now contains an interface called a `BacktrackingIterator`. Iterators that implement this will be able to mark a point during iteration, and reset back to that mark. For example, here we have a back tracking iterator that just returns 1, 2, and 3, but can backtrack:

```java
BackTrackingIterator<Integer> iter = new BackTrackingIteratorImplementation();
iter.next(); //returns 1
iter.next(); //returns 2
iter.mark();
iter.next(); //returns 3
iter.hasNext(); //returns false
iter.reset();
iter.hasNext(); // returns true
iter.next(); //returns 2
```

`ArrayBacktrackingIterator` implements this interface. It takes in an array and returns a backtracking iterator over the values in that array.

### Table

The `table` directory contains an implementation of
relational tables that store values of type `DataBox`. The `RecordId` class uniquely identifies a record on a page by its **page number** and **entry number** on that page. A `Record` is represented as a list of DataBoxes. A `Schema` is represented as list of column names and a list of column types. A `RecordIterator` takes in an iterator over `RecordId`s for a given table and returns an iterator over the corresponding records. A `Table` is made up of pages, with the first page always being the header page for the file. See the comments in `Table` for how the data of a table is serialized to a file.

### Database

The `Database` class represents a database. It is the interface through which we can create and update tables, and run queries on tables. When a user is operating on the database, they start a `transaction`, which allows for atomic access to tables in the database. You should be familiar with the code in here as it will be helpful when writing your own tests.

### Query

The `query` directory contains what are called query operators. These are operators that are applied to one or more tables, or other operators. They carry out their operation on their input operator(s) and return iterators over records that are the result of applying that specific operator. We call them **operators** here to distinguish them from the Java iterators you will be implementing.

`SortOperator` does the external merge sort algorithm covered in lecture. It contains a subclass called a `Run`. A `Run` is just an object that we can add records to, and read records from. Its underlying structure is a Table.

`JoinOperator` is the base class that join operators you will implement extend. It contains any methods you might need to deal with tables through the current running transaction. This means you should not deal directly with `Table` objects in the `Query` directory, but only through methods given through the current transaction.


## Your Tasks

You need to implement all unsupported operators which are marked by:

```java
throw new UnsupportedOperationException("Implement this.");
```

We use additional tests to evaluate your solution (in addition to the ones we are providing with this codebase).

### 1. Table Iterators

In the `table` directory, fill in the class `Table#RIDPageIterator`. The tests in `TestTable` should pass once this is complete.

**Hint:** To fill in this iterator, you can start from reading the description of storage format and bitmap at the beginning of the Table class (line 21) in Table.java.

### 2. Nested Loops Joins

There are three types of join algorithms in the codebase (See section 15.5.2 of the textbook):

- SNLJ: Simple Nested Loop Join
- BNLJ: Block Nested Loop Join
- BNLJOptimized: Optimized Block Nested Loop Join

Move to the `query` directory. You may first want to take a look at `SNLJOperator`. Complete `BNLJOperator` and `BNLJOptimizedOperator`. The BNLJ and Optimized BNLJ tests in `TestJoinOperator` should pass once this is complete.

We sometimes use the words `block` and `page` interchangeably to describe a single unit of transfer from disc. 
The notion of a `block` when discussing join algorithms is different however. A `page` is a single unit of transfer from disc, and a  `block` is one or more `pages`.
Sometimes BNLJ is also called PNLJ. Similarly, BNLJOptimized is called BNLJ.

**Hint:** BNLJ and BNLJOptimized extend from `JoinOperator`. You should be familiar with this class, it contains some useful methods which can help you get the different iterators such as `getPageIterator`, `getRecordIterator` and `getBlockIterator`. 
**NOTE:** BNLJOptimized needs access to numBuffer, you may use that number from `BNLJOptimizedOperator`.

### 3: External Sort

Complete implementing `SortOperator.java`. The tests in `TestSortOperator` should pass once this is complete.

**Besides when the comments tell you that you can do something in memory, everything else should be streamed. You should not hold more pages in memory at once than the given algorithm says you are allowed to.**

**Hint:** To get `numBuffer` pages of records at a time, you need to get `PageIterator` by `transaction.getPageIterator`, then pass `pageIterator` and `numBuffers` to `transaction.getBlockIterator`.

### 4: Sort Merge Join

Complete implementing `SortMergeOperator.java`. The SortMerge tests in `TestJoinOperator` should pass once this is complete.

### 5: Hash Join

Complete implementing `HashJoinOperator.java`. The tests in `TestHashJoinOperator` should pass once this is complete.

## Testing
> [!NOTE]  
> Remember to periodically use `git pull` to fetch the latest changes. We may add or adjust tests if we notice problems with them (e.g. the time limit for a test needs to be increased).

In the `src/test/java/edu/umd/cs424/database` directory, you will find all the unit tests we have provided to you.

Remember the test cases we give you are not comprehensive, so you should write your own tests to further test your code and catch edge cases.

The tests we provide to you for this project are under `table/TestTable.java` for part 1, `query/TestJoinOperator` for parts 2 and 4, `query/TestSortOperator` for part 3, and `query/TestHashJoinOperator` for part 5.

## Submitting

Just submit the following files to Gradescope.

```bash
├── Table.java
├── BNLJOperator.java
├── BNLJOptimizedOperator.java
├── SortMergeOperator.java
├── SortOperator.java
└── HashJoinOperator.java
```
