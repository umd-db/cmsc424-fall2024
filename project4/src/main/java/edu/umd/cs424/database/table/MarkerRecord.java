package edu.umd.cs424.database.table;

import java.util.ArrayList;

import edu.umd.cs424.database.databox.DataBox;

/**
 * An empty record used to delineate groups in the GroupByOperator.
 * TODO(mwhittaker): Explain better.
 */
public class MarkerRecord extends Record {
    private static final MarkerRecord record = new MarkerRecord();

    private MarkerRecord() {
        super(new ArrayList<DataBox>());
    }

    public static MarkerRecord getMarker() {
        return MarkerRecord.record;
    }
}
