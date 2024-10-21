# Project 3: B+ Trees

This project is to be done by yourself.

## Introduction

To begin, pull the Project 3 folder from our repository as always.

This project was adapted from a B+ tree implementation project originally developed at the University of California Berkeley. The goal of this project is to give you a deeper understanding of how B+ trees work. The best way to accomplish this is by building one yourself. Do not fear, you will not be writing this from scratch!  Methods you will need to implement will have comments telling you what to do.  More on this later.

## Environment

### Using Your Editor/IDE of Choice

For this project, we highly recommend you install/use an IDE. Here are some popular options:
1. [IntelliJ](https://www.jetbrains.com/idea/download) (download the community edition). Open the project3 folder and choose "maven project" when prompted. You may also see a "JDK is not defined" warning when opening a file. Click "Setup JDK" and "Download JDK" to set the project up. IntelliJ is known to be slow during startup, so it may take a while for it to finish initializing.
2. [Eclipse](https://www.eclipse.org/downloads/). In Eclipse, import this project with: File > import > maven > existing maven project.
3. An editor of your choice such as VS Code. Note that you may need to manually [install OpenJDK](https://adoptium.net/temurin/releases/), [download maven](https://maven.apache.org/download.cgi), and [install maven](https://maven.apache.org/install.html) in order to run the project locally. Alternatively, you can use the `mvnw` file in the project directory to set up maven for you. TAs may only be able to provide limited assistance if you choose this route since the setup process varies by person and by editor.

### Using Docker

Alternatively, you can run the project within a Docker environment and complete the project using an IDE/editor of your choice.

The docker container can be started with the following command. Make sure that the current directory is the directory of the project:

```bash
docker run -v $PWD:/project3 -ti --rm -w /project3 --pull missing maven:3.9.9-eclipse-temurin-21-alpine /bin/bash
```

In the terminal of the Docker container, you can compile the project by running

```bash
# build code without testing
mvn compile
```

You should be able to see "BUILD SUCCESS" in the output, indicating that your environment is set up correctly.

## Testing and Debugging
> [!NOTE]
> If you are eager to start writing code, you can skip this section for now and refer back here when testing and debugging your implementation.

### Docker

If you are using docker, simply run `mvn test` in the docker container to perform all tests. 

If you would like to run a specific test, you can run, for example
```bash
mvn test -Dtest=TestBPlusNode#testFromBytes
```
This will run `testFromBytes` in the `TestBPlusNode` class.

As the start of the project, `mvn test` will give out a bunch of error messages. This is normal behavior. You should be able to find a line of output that says something along the lines of

> Tests run: 40, Failures: 0, Errors: 34, Skipped: 0

Most tests failed because you have not yet implemented the functions they are testing. However, a few tests will pass out of the box.

Because the project directory and the docker container are synced, you can make edits to the project using your editor of choice. Your changes will be synced with the container so that running `mvn test` will test the latest changes you have made. However, debugging in this setup could be difficult. You are welcome to try other options such as developing with Docker through VS Code, though the TAs may only be able to provided limited assistance since the setup varies by person and editor.

### IDE or Editor

If you are using an IDE such as IntelliJ or Eclipse, you can navigate to the tests which reside under `src/test/java`, right-click on a test class (or a directory containing tests), and then choose `Run As > JUnit Test` (Eclipse) or `Run tests in Java` (IntelliJ).

Most tests will fail initially, but you may see a few successful tests: this is expected behavior.

If a test failed, and you are not sure why, you can insert a breakpoint in locations of interest and run individual tests that failed using a debugger. If you are not sure what the previous sentence is saying, you may want to learn more about how to use a debugger on your IDE/editor on your own.

## The Project Files

Here is a brief overview of the directories present in the project.

### index
The 'index' directory contains all the files that you will need to edit, specifically, LeafNode.java, InnerNode.java, and BPlusTree.java.  

* **BPlusNode.java**: An abstract class for LeafNode and InnerNode. This contains very important information on how to implement methods in those two classes.  Make sure you follow what is written here!

* **LeafNode.java**: Leaf nodes class.  You will need to implement some methods in this.

* **InnerNode.java**: Inner nodes class.  You will need to implement some methods in this.

* **BPlusTree.java**: B+ Tree class.  This is the structure that will hold LeafNodes and InnerNodes.  You will need to implement some methods in this.

> [!NOTE]
> Although there are a more files in this project, you do not need to have a robust understanding of all of them. 
> Knowing about how the remaining components work may be helpful but is not required to complete this project: you will only be changing code the index directory.

### common
The 'common' directory contains miscellaneous but handy bits of code.

### databox
The 'databox' directory contains classes which represent the values stored in a database as well as their types. Specifically, the DataBox class represents values and the Type class represents types. Here's an example:

```java
DataBox x = new IntDataBox(42); // The integer value '42'.
Type t = Type.intType();        // The type 'int'.
Type xsType = x.type();         // Get x's type: Type.intType()
int y = x.getInt();             // Get x's value: 42
String s = x.getString();       // An exception is thrown.
```

### io
The `io` directory contains code that allows you to allocate, read, and write
pages to and from a file. All modifications to the pages of the file are
persisted to the file. The two main classes of this directory are
`PageAllocator` which can be used to allocate pages in a file, and `Page` which
represents pages in the file.  Below are some examples of how this works so you can get an idea of it:

Here is an example of how to persist data into a file using a `PageAllocator`:

```java
// Create a page allocator which stores data in the file "foo.data". Setting
// wipe to true clears out any data that may have previously been in the file.
bool wipe = true;
PageAllocator allocator = new PageAllocator("foo.data", wipe);

// Allocate a page in the file. All pages are assigned a unique page number
// which can be used to fetch the page.
int pageNum = allocator.allocPage(); // The page number of the allocated page.
Page page = allocator.fetchPage(pageNum); // The page we just allocated.
System.out.println(pageNum); // 0. Page numbers are assigned 0, 1, 2, ...

// Write data into the page. All data written to the page is persisted in the
// file automatically.
Buffer buf = page.getBuffer(transaction);
buf.putInt(42);
buf.putInt(9001);
```

Here is an example of how to read data that's been persisted to a file:

```java
// Create a page allocator which stores data in the file "foo.data". Setting
// wipe to false means that this page allocator can read any data that was
// previously stored in "foo.data".
bool wipe = false;
PageAllocator allocator = new PageAllocator("foo.data", wipe);

// Fetch the page we previously allocated.
Page page = allocator.fetchPage(0);

// Read the data we previously wrote.
Buffer buf = page.getBuffer(transaction);
int x = buf.getInt(); // 42
int y = buf.getInt(); // 9001
```

### table
The 'table' directory used to contain other table related things but you only need the 'RecordId' class which uniquely identifies a record on a page by its page number and entry number:

```java
// The jth record on the ith page.
RecordId rid = new RecordId(i, (short) j);
```

There are a few other files that are used to make the tests work that you are welcome to look at if you need to.

## Your Tasks
Familiarize yourself with the code in the 'index' directory since all the changes that you will make will happen here.

### First Function
Implement the `LeafNode::fromBytes` function that reads a `LeafNode` from a page. For information on how a leaf node is serialized, see `LeafNode::toBytes`. For an example on how to read a node from disk, see `InnerNode::fromBytes`.

Our implementation of B+ trees assumes that inner nodes and leaf nodes can be serialized on a single page. You do not have to support nodes that span multiple pages.

> [!NOTE]
> It is important to implement this function correctly first because many other methods and tests depend on it. 

After implementing this function, the test `TestBPlusNode::testFromBytes` should pass. For more information on running tests and debugging your code when a test fails, refer back to the [testing and debugging section](#testing-and-debugging).

### Node Functions
Implement the `get` and `getLeftmostLeaf` methods of `LeafNode` and `InnerNode`. For information on what these methods do, refer to the comments in `BPlusNode`.

> [!NOTE]
> You may not modify the signature of any methods or classes that we provide to you, but you're free to add helper methods in LeafNode.java, InnerNode.java, and BPlusTree.java. This applies to all parts of the project.

All tests prefixed by "testGet" in `TestInnerNode` and `TestLeafNode` should pass at this point. 

---

Implement the `put` and `bulkLoad` methods of `LeafNode` and `InnerNode`. For information on what these methods do, refer to the comments in `BPlusNode`.

> [!IMPORTANT]
> Our implementation of B+ trees does not support duplicate keys. You need to throw a `BPlusTreeException` whenever a duplicate key is inserted.
> 
> The order `d` of a B+ Tree is given upon the tree's creation within its metadata field which is passed onto its inner nodes and leaf nodes. Refer to the [things to note section](#things-to-note) for more information.
> 
> Don't forget to call `sync` when implementing `put`, `remove`, and `bulkLoad`.

Again, all tests prefixed by `testPut` or `testBulkLoad` in `TestInnerNode` and `TestLeafNode` should pass.

---

Implement `remove` in `LeafNode` and `InnerNode`.

> [!IMPORTANT]
> Our implementation of delete does not rebalance the tree. Thus, the rule that all non-root leaf nodes in a B+ tree of order `d` contain between `d` and `2d` entries will not hold for this project. Note that actual B+ trees **do rebalance** after deletion, but we will **not** be implementing rebalancing trees in this project for the sake of simplicity.  This means you also do not need to account for cases where inner nodes may be deleted.

All tests in `TestBPlusNode`, `TestInnerNode`, and `TestLeafNode` should pass at this point. Again, the next set of functionalities depend on methods listed above, so you are recommended to make sure everything is correctly implemented before proceeding.

### B+ Tree Functions
Implement the `get`, `put`, `remove`, and `bulkLoad` methods of `BPlusTree`. 

All tests except those prefixed by `testIterator` should pass at this point.

### Iterators

Before starting this section, make sure you know what Java iterators are. You have already used them when implementing bulk load and might have seen similar features in other languages, such as C++ iterators, Haskell's lazy evaluation, and python iterables/generators.

Your task is to implement the `BPlusTreeIterator` inner class inside the `BPlusTree` class. There is already a simple yet flawed implementation, which you can use as a reference.

Assuming you have passed all previous tests, the existing `BPlusTreeIterator` should be able to let you pass `testIterator_simple`. However, the `testIterator_noHasNext` test will fail. Try to understand the existing iterator implementation and why it is wrong. You do not have to write the reason down, but keep it in mind as you proceed with your implementation.

You will now replace the existing iterator with a much more powerful one. In addition to iterating through the whole tree, it should also support
1. A lower bound and upper bound (inclusive) on the key value. Only records whose key is within this range should be returned by the iterator.
2. A limit on the total number of records that should be returned. If the total number of records exceeds this limit, only return the ones with the lowest keys. This corresponds to the `LIMIT` clause in SQL.
3. A custom function which takes a `DataBox` object and returns a `Boolean`. Treat this function as a blackbox: if the function returns `true`, your iterator should return the corresponding record; otherwise, the iterator rejects this record and moves on to the next one. For real-life inspirations, think of queries such as "find all students whose UID ends with a 3", which is too complicated and too specific to have a rule designed specifically for it. If you can still remember CMSC 330 content, this is similar with the function you pass to `filter`.

With these in mind, now implement `hasNext`, `next`, and the constructor `BPlusTreeIterator` (there are 3 constructors with different function signatures) in `BPlusTreeIterator`. Two of the three `BPlusTreeIterator` constructors implement a subset of the features of the third one, which is also the most powerful. You are recommended to use [explicit constructor invocation](https://docs.oracle.com/javase/tutorial/java/javaOO/thiskey.html) so that you only need to implement one method.

All tests should pass upon successful completion of the iterator.

> [!NOTE]
> An inefficient implementation may pass all tests on your own machine (which is usually pretty fast) but get rejected by the autograder (which is pretty slow). If that happens, you are welcome to read the source code of the tests to determine the cause.

## Things to Note

This project uses a variable `d` to describe the "order" of a B+ Tree, and `2d` as the number of **values** in a node (this is true both for leaf nodes and internal nodes). In contrast, the textbook we are using for our class uses a variable `n`, which is defined as the number of **pointers** in a node. Every node in a B+ tree--both leaf nodes and internal nodes--have one more pointer than value, so the `n` from your textbook is always equal to `2d + 1`.

Therefore, this sentence in the textbook 
> Each leaf can hold up to n − 1 values. We allow leaf nodes to contain as few as ceiling[ (n−1)/2 ] values.

translates to 
> Each leaf can hold between d and 2d values, inclusively. 

in this project. For example, here is a node with `n = 5` (or `d = 2`).

<img src="nodes.png" alt="nodes" width="450"/>

Thus, there is no difference between the size constraints of nodes between this project and your textbook. You should be aware of how `d` is defined and how `n` is defined, and how `n = 2d + 1`, for quizzes and exams.

## Submitting
Just submit your `LeafNode.java`, `InnerNode.java`, and `BPlusTree.java` to Gradescope.
