package edu.umd.cs424.database.io;

/**
 * Exception thrown for errors while paging.
 */
public class PageException extends RuntimeException {
    public PageException() {
        super();
    }

    public PageException(String message) {
        super(message);
    }
}

