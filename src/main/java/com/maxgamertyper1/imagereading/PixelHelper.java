package com.maxgamertyper1.imagereading;

public class PixelHelper {
    public static int IndexCalculator(int width, int x, int y) {
        return y * width + x;
    }

    public static int[] ReverseIndexCalculator(int width, int index) {
        return new int[]{index % width, index / width};
    }

    public static boolean IndexIsOnLeftColomn(int width, int index) {
        return index % width == 0; // however, here the 0-index helps us, but idk how to wxplain it (i.e. width=5; 0,1,2,3,4 (first row), 5,6,7,8,9; so its modulo 5 for it)
    }

    public static boolean IndexIsOnTopRow(int width, int index) {
        return index <= (width - 1); // since the array is 0-indexed we need to -1 to check if its on the top
    }

    public static boolean IndexInImage(int index, int flatsize) {
        return index >= 0 && index < flatsize;
    }

    public static boolean IndexsInSameRow(int index1, int index2, int width) {
        return index1 / width == index2 / width;
    }
}
