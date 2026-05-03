package com.maxgamertyper1.datatypes.records;

public record CornerPoint(int index, int flagmask) {
    @Override
    public String toString() {
        return "index: "+index+"; flagmask: "+flagmask;
    }
    public boolean trueCorner() {
        return (flagmask!=8 && flagmask!=12 && flagmask>4);
    }
}
