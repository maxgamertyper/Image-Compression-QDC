package com.maxgamertyper1.datatypes;

public record CornerPoint(int index, int flagmask) {
    @Override
    public String toString() {
        return "index: "+index+"; flagmask: "+flagmask;
    }
}
