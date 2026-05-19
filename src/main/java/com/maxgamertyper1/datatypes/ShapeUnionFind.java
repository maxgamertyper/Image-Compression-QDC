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
    private int[] rootIndexes;
    private int[] shapeIndexAddressTable;
    private int[][] shapeIndexGroupings;

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
        calculateShapeRoots();
    }

    public void calculateShapeRoots() {
        IntArrayList uniqueBosses = new IntArrayList(16);
        shapeIndexAddressTable = new int[ImageDataHolder.flatsize];
        Arrays.fill(shapeIndexAddressTable,-1);

        for (int i = 0; i < parentPixels.length; i++) {
            if (parentPixels[i] == i) {
                shapeIndexAddressTable[i] = uniqueBosses.Size();
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
        shapeAreasBasedOnStart = new int[getTotalShapeCount()];

        for (int i = 0; i < ImageDataHolder.flatsize; i++) {
            int root = parentPixels[i]; // find the root of a pixel

            shapeAreasBasedOnStart[shapeIndexAddressTable[root]]++;
        }
    }

    public void calculateIndexGroupings() {
        shapeIndexGroupings = new int[getTotalShapeCount()][];

        for (int i=0; i<shapeIndexGroupings.length; i++) {
            shapeIndexGroupings[i] = new int[getShapeArea(rootIndexes[i])];
        }
        int[] shapePointers = new int[getTotalShapeCount()];

        for (int index = 0; index<ImageDataHolder.flatsize; index++) {
            int address = shapeIndexAddressTable[parentPixels[index]];
            shapeIndexGroupings[address][shapePointers[address]++] = index;
        }
    }

    public int[] getIndexGrouping(int shapeID) {
        return shapeIndexGroupings[shapeIndexAddressTable[shapeID]];
    }

    public void calculateShapeContinuity() {
        shapeContinuityBasedOnStart = new int[getTotalShapeCount()];

        for (int i = 0; i < ImageDataHolder.flatsize; i++) {
            int root = parentPixels[i];

            int topIndex = i-ImageDataHolder.width;
            int LeftIndex = i-1;

            if (IndexInImage(topIndex) && parentPixels[topIndex]==root) {
                shapeContinuityBasedOnStart[shapeIndexAddressTable[root]]++;
            }
            if (IndexInImage(LeftIndex) && IndexsInSameRow(LeftIndex,i) && parentPixels[LeftIndex]==root) {
                shapeContinuityBasedOnStart[shapeIndexAddressTable[root]]++;
            }
        }
    }

    public int getHoles(int startingIndex) {
        return shapeHolesBasedOnStart[shapeIndexAddressTable[startingIndex]];
    }

    public int getContinuity(int startingIndex) {
        return shapeContinuityBasedOnStart[shapeIndexAddressTable[startingIndex]]; // same as above
    }

    public void calculatePerimiterAndHoles(int[] relativeFlags) {
        shapeHolesBasedOnStart = new int[getTotalShapeCount()];

        IntArrayList[] perimiterHolders = new IntArrayList[getTotalShapeCount()];

        for (int i=0; i<rootIndexes.length; i++) {
            int location = rootIndexes[i];
            shapeIndexAddressTable[location] = i;
            perimiterHolders[i] = new IntArrayList(16);
        }

        // NOTE: this might be better if its moved into finalize with a Width size array that tracks the left and above pixels aligned with the x-value
        for (int currentIndex=0; currentIndex<relativeFlags.length; currentIndex++) {
            int flag = relativeFlags[currentIndex];

            if (flag == 0) continue; // skip interior so i dont need all the other checks

            int address = shapeIndexAddressTable[parentPixels[currentIndex]];
            IntArrayList currentHolder = perimiterHolders[address];

            if ((flag&8)!=0) { // if theres a top boundary
                HandleSideTheSequel(currentIndex-ImageDataHolder.width, currentHolder);
            }
            if ((flag&4)!=0) { // if theres a bottom boundary
                HandleSideTheSequel(currentIndex+ImageDataHolder.width, currentHolder);
            }
            if ((flag&2)!=0) { // if theres a right boundary
                HandleSideTheSequel(currentIndex+1, currentHolder);
            }
            if ((flag&1)!=0) { // if theres a left boundary
                HandleSideTheSequel(currentIndex-1, currentHolder);
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
                    perimiterHolders[shapeIndexAddressTable[parentShape]].Remove(rootIndexes[i]);
                    holesFound++;
                    shapeHolesBasedOnStart[shapeIndexAddressTable[currentPerimiterTouchers.atIndex(0)]]++;
                    currentPerimiterTouchers.Clear();
                }
            }
        }
    }

    private void HandleSideTheSequel(int newIndex, IntArrayList currentHolder) {
        if (IndexInImage(newIndex)) {
            currentHolder.Add(parentPixels[newIndex]);
        } else {
            currentHolder.Add(-1); // represents the image edge
        }
    }

    public int getShapeArea(int shapeStartingID) {
        return shapeAreasBasedOnStart[shapeIndexAddressTable[shapeStartingID]];
    }
}