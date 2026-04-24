package com.maxgamertyper1.datatypes;

public class ShapeUnionFind {
    private final int[] parentPixels;

    public ShapeUnionFind(int n) {
        parentPixels = new int[n];

        // at the start every pixel is its own shape
        for (int i = 0; i < n; i++) {
            parentPixels[i] = i;
        }
    }

    public void Union(int pixelIndex1, int pixelIndex2) {
        int rootIndex1 = Find(pixelIndex1);
        int rootIndex2 = Find(pixelIndex2);

        if (rootIndex1!=rootIndex2) {
            parentPixels[rootIndex1] = rootIndex2;
        }
    }

    public int Find(int pixelIndex) {
        while (parentPixels[pixelIndex]!=pixelIndex) {
            parentPixels[pixelIndex] = parentPixels[parentPixels[pixelIndex]];

            pixelIndex = parentPixels[pixelIndex];
        }
        return pixelIndex;
    }

    public void Finalize() { // flatten the array to unique shape IDs, which the IDs are also the first index in the shape
        for (int i=0; i<parentPixels.length; i++) {
            parentPixels[i] = Find(i);
        }
    }

    public int getTotalShapeCount() {
        IntArrayList uniqueBosses = new IntArrayList(16);
        for (int i = 0; i < parentPixels.length; i++) {
            if (parentPixels[i] == i) {
                uniqueBosses.Add(i);
            }
        }
        return uniqueBosses.size();
    }

    public int[] getRawData() {
        return parentPixels;
    }
}
