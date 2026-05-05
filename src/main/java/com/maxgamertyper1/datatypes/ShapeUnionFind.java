package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageDataHolder;

import java.util.Arrays;
import java.util.BitSet;

import static com.maxgamertyper1.imagereading.PixelHelper.IndexInImage;
import static com.maxgamertyper1.imagereading.PixelHelper.IndexsInSameRow;

public class ShapeUnionFind {
    private final int[] parentPixels;
    private int[] shapeAreasBasedOnStart;
    private int[] shapeHolesBasedOnStart;
    private int[] shapeContinuityBasedOnStart;
    private int[] shapePerimiterBasedOnStart;
    private int[] rootIndexes;

    public ShapeUnionFind(int size) {
        parentPixels = new int[size];

        // at the start every pixel is its own shape
        for (int i = 0; i < size; i++) {
            parentPixels[i] = i;
        }
    }

    public ShapeUnionFind(int[] parentPixels) {
        this.parentPixels = parentPixels;
    }

    public void Union(int pixelIndex1, int pixelIndex2) {
        int rootIndex1 = Find(pixelIndex1);
        int rootIndex2 = Find(pixelIndex2);

        if (rootIndex1!=rootIndex2) {
            // make it have the smalles root be the actual root!
            if (rootIndex1 < rootIndex2) {
                parentPixels[rootIndex2] = rootIndex1;
            } else {
                parentPixels[rootIndex1] = rootIndex2;
            }
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

    public void calculateShapeRoots() {
        IntArrayList uniqueBosses = new IntArrayList(16);

        for (int i = 0; i < parentPixels.length; i++) {
            if (parentPixels[i] == i) {
                uniqueBosses.Add(i);
            }
        }
        rootIndexes = uniqueBosses.toIntArray();
    }

    public int getTotalShapeCount() {
        return rootIndexes.length;
    }

    public int[] getRawData() {
        return parentPixels;
    }

    public void calculateShapeAreas() {
        shapeAreasBasedOnStart = new int[ImageDataHolder.flatsize];

        Arrays.fill(shapeAreasBasedOnStart,0);

        for (int i = 0; i < ImageDataHolder.flatsize; i++) {
            int root = Find(i); // find the root of a pixel

            shapeAreasBasedOnStart[root]++;
        }
    }

    public void calculateShapeContinuity() {
        shapeContinuityBasedOnStart = new int[ImageDataHolder.flatsize];

        Arrays.fill(shapeContinuityBasedOnStart,0);

        for (int i = 0; i < ImageDataHolder.flatsize; i++) {
            int root = Find(i);

            int topIndex = i-ImageDataHolder.width;
            int LeftIndex = i-1;

            if (IndexInImage(topIndex) && Find(topIndex)==root) {
                shapeContinuityBasedOnStart[root]++;
            }
            if (IndexInImage(LeftIndex) && IndexsInSameRow(LeftIndex,i) && Find(LeftIndex)==root) {
                shapeContinuityBasedOnStart[root]++;
            }
        }
    }

    public int getHoles(int startingIndex) {
        return shapeHolesBasedOnStart[startingIndex];
    }

    public int getContinuity(int startingIndex) {
        return shapeContinuityBasedOnStart[startingIndex];
    }

    public void calculatePerimiterAndHoles(int[] relativeFlags) {
        shapeHolesBasedOnStart = new int[ImageDataHolder.flatsize];
        shapePerimiterBasedOnStart = new int[ImageDataHolder.flatsize];

        BitSet hasMultipleSurroundies = new BitSet(ImageDataHolder.flatsize);
        int[] originalSurrounderBasedOnStart = new int[ImageDataHolder.flatsize];

        Arrays.fill(originalSurrounderBasedOnStart,-1);

        for (int currentIndex=0; currentIndex<relativeFlags.length; currentIndex++) {
            int flag = relativeFlags[currentIndex];
            int root = Find(currentIndex);

            shapePerimiterBasedOnStart[root]+=Integer.bitCount(flag);

            if (hasMultipleSurroundies.get(root)) continue;

            if ((flag&8)!=0) { // if theres a top boundary
                int aboveIndex = currentIndex-ImageDataHolder.width;

                HandleSide(root,aboveIndex,hasMultipleSurroundies,originalSurrounderBasedOnStart);
            }
            if ((flag&4)!=0) { // if theres a bottom boundary
                int belowIndex = currentIndex+ImageDataHolder.width;

                HandleSide(root,belowIndex,hasMultipleSurroundies,originalSurrounderBasedOnStart);
            }
            if ((flag&2)!=0) { // if theres a right boundary
                int rightIndex = currentIndex+1;

                HandleSide(root,rightIndex,hasMultipleSurroundies,originalSurrounderBasedOnStart);
            }
            if ((flag&1)!=0) { // if theres a left boundary
                int leftIndex = currentIndex-1;

                HandleSide(root,leftIndex,hasMultipleSurroundies,originalSurrounderBasedOnStart);
            }
        }
        if (rootIndexes==null) {
            this.calculateShapeRoots();
        }
        for (int root : rootIndexes) {
            if (!hasMultipleSurroundies.get(root)) {
                // never 0 as that touches an edge and will be marked false; so that doesnt need to be checked as its always false
                shapeHolesBasedOnStart[Find(root-1)]++;
            }
        }
    }

    private void HandleSide(int root, int newIndex, BitSet hasMultipleSurroundies, int[] originalSurrounderBasedOnStart) {
        if (IndexInImage(newIndex)) {
            if (originalSurrounderBasedOnStart[root]==-1) {
                originalSurrounderBasedOnStart[root] = Find(newIndex);
            } else if (originalSurrounderBasedOnStart[root]!=Find(newIndex)) {
                hasMultipleSurroundies.set(root);
            }
        } else {
            hasMultipleSurroundies.set(root); // if its on the bounds of the image, its not a hole bro; it either has to touch the bounds again (which then it'd be the whole image), or it touches anouther shape
        }
    }

    public int getShapeArea(int shapeStartingID) {
        return shapeAreasBasedOnStart[shapeStartingID];
    }
}