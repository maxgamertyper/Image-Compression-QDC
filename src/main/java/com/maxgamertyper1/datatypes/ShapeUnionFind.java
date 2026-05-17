package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageDataHolder;

import java.util.Arrays;
import java.util.BitSet;

import static com.maxgamertyper1.imagereading.PixelHelper.IndexInImage;
import static com.maxgamertyper1.imagereading.PixelHelper.IndexsInSameRow;

 // this stuff wastes a ton of space so i might try to figure out a primitive hash map or smth

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
        if (rootIndexes==null) {
            this.calculateShapeRoots();
        }
        shapeHolesBasedOnStart = new int[ImageDataHolder.flatsize];

        IntArrayList[] perimiterHolders = new IntArrayList[rootIndexes.length];
        int[] addressTable = new int[ImageDataHolder.flatsize];
        Arrays.fill(addressTable,-1);

        for (int i=0; i<rootIndexes.length; i++) {
            int location = rootIndexes[i];
            addressTable[location] = i;
            perimiterHolders[i] = new IntArrayList(16);
        }

        for (int currentIndex=0; currentIndex<relativeFlags.length; currentIndex++) {
            int flag = relativeFlags[currentIndex];
            int root = Find(currentIndex);

            if ((flag&8)!=0) { // if theres a top boundary
                int aboveIndex = currentIndex-ImageDataHolder.width;
                HandleSideTheSequel(root, aboveIndex, perimiterHolders, addressTable);
            }
            if ((flag&4)!=0) { // if theres a bottom boundary
                int belowIndex = currentIndex+ImageDataHolder.width;
                HandleSideTheSequel(root, belowIndex, perimiterHolders, addressTable);
            }
            if ((flag&2)!=0) { // if theres a right boundary
                int rightIndex = currentIndex+1;
                HandleSideTheSequel(root, rightIndex, perimiterHolders, addressTable);
            }
            if ((flag&1)!=0) { // if theres a left boundary
                int leftIndex = currentIndex-1;
                HandleSideTheSequel(root, leftIndex, perimiterHolders, addressTable);
            }
        }

        // NOTE: may be better to make a primitve set for this
        for (IntArrayList IAL : perimiterHolders) {
            IAL.removeDuplicatesUnsorted();
        }

        // NOTE: might be better to make this a queue that only processes the new updated shapes
        int holesFound = 1;
        while (holesFound!=0) {
            holesFound=0;

            for (int i=0; i<perimiterHolders.length; i++) {
                IntArrayList currentPerimiterTouchers = perimiterHolders[i];

                if (currentPerimiterTouchers.Size()==1) {
                    int parentShape = currentPerimiterTouchers.atIndex(0);
                    perimiterHolders[addressTable[parentShape]].Remove(rootIndexes[i]);
                    holesFound++;
                    shapeHolesBasedOnStart[currentPerimiterTouchers.atIndex(0)]++;
                    currentPerimiterTouchers.Clear();
                }
            }
        }
    }

    private void HandleSideTheSequel(int root, int newIndex, IntArrayList[] perimiterHolders, int[] addressTable) {
        int address = addressTable[root];
        if (IndexInImage(newIndex)) {
            perimiterHolders[address].Add(parentPixels[newIndex]);
        } else {
            perimiterHolders[address].Add(-1); // represents the image edge
        }
    }

    public int getShapeArea(int shapeStartingID) {
        return shapeAreasBasedOnStart[shapeStartingID];
    }
}