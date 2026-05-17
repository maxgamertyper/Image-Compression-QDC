package com.maxgamertyper1.datatypes.records;

public record RectangleRepresentative(int height, int length, int startingIndex, int value) {
    public int area() {
        return length * height;
    }

    public int storageCost() {
        return dataBitLength(height)+dataBitLength(length)+dataBitLength(value);
    }

    private int dataBitLength(int data) {
        int baseReturn = (32 - Integer.numberOfLeadingZeros(data));
        return baseReturn == 0 ? 1 : baseReturn;
    }
}