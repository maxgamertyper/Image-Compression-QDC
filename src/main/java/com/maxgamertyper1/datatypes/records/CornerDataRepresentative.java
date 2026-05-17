package com.maxgamertyper1.datatypes.records;

import java.util.Arrays;

public record CornerDataRepresentative(int[] convexCornerIndexDeltas, int startingIndex, int[] cornerFlags, int dataValue) {
    @Override
    public String toString() {
        return "Index Deltas: "+ Arrays.toString(convexCornerIndexDeltas) +"; StartingIndex: "+startingIndex+" Value: "+dataValue;
    }
}
