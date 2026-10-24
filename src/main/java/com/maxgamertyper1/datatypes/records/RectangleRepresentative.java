package com.maxgamertyper1.datatypes.records;

public record RectangleRepresentative(
        int height,
        int width,
        int startingIndex,
        int value,
        int zigzagValue
) {

    public RectangleRepresentative(int height, int width, int startingIndex, int value) {
        this(
                height,
                width,
                startingIndex,
                value,
                zigzagEncode(value)
        );
    }

    private static int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-2, ...
        return (number << 1) ^ (number >> 31);
    }

    public int area() {
        return width * height;
    }

    public int storageCost() {
        return dataBitLength(height) + dataBitLength(width) + dataBitLength(value);
    }

    private int dataBitLength(int data) {
        int baseReturn = (32 - Integer.numberOfLeadingZeros(data));
        return baseReturn == 0 ? 1 : baseReturn;
    }

    public boolean betterThan(RectangleRepresentative other) {
        return (long) area() * other.storageCost() > (long) other.area() * storageCost();
    }
}