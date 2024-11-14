package edu.umd.cs424.database.query;

public class QueryPlanException extends Exception {
    private String message;

    public QueryPlanException(String message) {
        this.message = message;
    }

    public QueryPlanException(Exception e) {
        this.message = e.getClass().toString() + ": " + e.getMessage();
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}

