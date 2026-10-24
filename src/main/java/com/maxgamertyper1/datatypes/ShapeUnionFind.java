package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageMeta;

import java.util.Arrays;

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
    private int[] shapeParentFromHole;
    private int[][] shapeChildrenMap;

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

    public void solveShapes(byte[] flags, ImageMeta metaData) {
        flatten();
        calculateShapeRoots(metaData);
        calculateShapeAreas(metaData);
        calculateShapeContinuity(metaData);
        calculatePerimiterAndHoles(flags, metaData.width());
        calculateIndexGroupings(metaData);
    }

    public void union(int pixelIndex1, int pixelIndex2) {
        int rootIndex1 = find(pixelIndex1);
        int rootIndex2 = find(pixelIndex2);

        if (rootIndex1 != rootIndex2) {
            // make it have the smalles root be the actual root!
            if (rootIndex1 < rootIndex2) {
                parentPixels[rootIndex2] = rootIndex1;
            } else {
                parentPixels[rootIndex1] = rootIndex2;
            }
        }
    }

    public int find(int pixelIndex) {
        while (parentPixels[pixelIndex] != pixelIndex) {
            parentPixels[pixelIndex] = parentPixels[parentPixels[pixelIndex]];

            pixelIndex = parentPixels[pixelIndex];
        }
        return pixelIndex;
    }

    public void flatten() { // flatten the array to unique shape IDs, which the IDs are also the first index in the shape
        for (int i = 0; i < parentPixels.length; i++) {
            parentPixels[i] = find(i);
        }
    }

    public void calculateShapeRoots(ImageMeta metaData) {
        IntArrayList uniqueBosses = new IntArrayList(16);
        shapeIndexAddressTable = new int[metaData.flatsize() + 1];
        Arrays.fill(shapeIndexAddressTable, -1);

        for (int i = 0; i < parentPixels.length; i++) {
            if (parentPixels[i] == i) {
                shapeIndexAddressTable[i] = uniqueBosses.size();
                uniqueBosses.add(i);
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

    public void calculateShapeAreas(ImageMeta metaData) {
        shapeAreasBasedOnStart = new int[getTotalShapeCount()];

        for (int i = 0; i < metaData.flatsize(); i++) {
            int root = parentPixels[i]; // find the root of a pixel

            shapeAreasBasedOnStart[shapeIndexAddressTable[root]]++;
        }
    }

    public void calculateIndexGroupings(ImageMeta metaData) {
        shapeIndexGroupings = new int[getTotalShapeCount()][];

        for (int i = 0; i < shapeIndexGroupings.length; i++) {
            shapeIndexGroupings[i] = new int[getShapeArea(rootIndexes[i])];
        }
        int[] shapePointers = new int[getTotalShapeCount()];

        for (int index = 0; index < metaData.flatsize(); index++) {
            int address = shapeIndexAddressTable[parentPixels[index]];
            shapeIndexGroupings[address][shapePointers[address]++] = index;
        }
    }

    public int[] getIndexGrouping(int shapeID) {
        return shapeIndexGroupings[shapeIndexAddressTable[shapeID]];
    }

    public void calculateShapeContinuity(ImageMeta metaData) {
        shapeContinuityBasedOnStart = new int[getTotalShapeCount()];

        for (int i = 0; i < metaData.flatsize(); i++) {
            int root = parentPixels[i];

            int topIndex = i - metaData.width();
            int LeftIndex = i - 1;

            if (IndexInImage(topIndex, metaData.flatsize()) && parentPixels[topIndex] == root) {
                shapeContinuityBasedOnStart[shapeIndexAddressTable[root]]++;
            }
            if (IndexInImage(LeftIndex, metaData.flatsize()) && IndexsInSameRow(LeftIndex, i, metaData.width()) && parentPixels[LeftIndex] == root) {
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

    public void calculatePerimiterAndHoles(byte[] relativeFlags, int width) { // O(NlogN)
        shapeHolesBasedOnStart = new int[getTotalShapeCount()];

        shapeParentFromHole = new int[getTotalShapeCount()];
        int flatsize = parentPixels.length;
        Arrays.fill(shapeParentFromHole, -1);

        IntArrayList[] perimiterHolders = new IntArrayList[getTotalShapeCount()];

        for (int i = 0; i < rootIndexes.length; i++) {
            int location = rootIndexes[i];
            shapeIndexAddressTable[location] = i;
            perimiterHolders[i] = new IntArrayList(16);
        }

        // NOTE: this might be better if its moved into finalize with a Width size array that tracks the left and above pixels aligned with the x-value
        for (int currentIndex = 0; currentIndex < relativeFlags.length; currentIndex++) {
            int flag = relativeFlags[currentIndex];

            if (flag == 0) continue; // skip interior so i dont need all the other checks

            int address = shapeIndexAddressTable[parentPixels[currentIndex]];
            IntArrayList currentHolder = perimiterHolders[address];

            if ((flag & 8) != 0) { // if theres a top boundary
                HandleSideTheSequel(currentIndex - width, currentHolder, flatsize);
            }
            if ((flag & 4) != 0) { // if theres a bottom boundary
                HandleSideTheSequel(currentIndex + width, currentHolder, flatsize);
            }
            if ((flag & 2) != 0) { // if theres a right boundary
                HandleSideTheSequel(currentIndex + 1, currentHolder, flatsize);
            }
            if ((flag & 1) != 0) { // if theres a left boundary
                HandleSideTheSequel(currentIndex - 1, currentHolder, flatsize);
            }
        }

        // NOTE: may be better to make a primitve set for this

        IntArrayDeque shapeQueue = new IntArrayDeque(getTotalShapeCount()); // make size is the amount of shapes

        for (int i = 0; i < perimiterHolders.length; i++) {
            IntArrayList IAL = perimiterHolders[i];
            IAL.removeDuplicatesUnsorted();

            if (IAL.size() == 1) {
                shapeQueue.enqueue(i);
            }
        }

        while (shapeQueue.getSize() != 0) {
            int thisShape = shapeQueue.dequeue();
            IntArrayList currentNeighbors = perimiterHolders[thisShape];

            int parentShape = currentNeighbors.atIndex(0);
            int parentShapeAddress = shapeIndexAddressTable[parentShape];

            IntArrayList parentNeighbors = perimiterHolders[parentShapeAddress];
            parentNeighbors.swapRemove(thisShape);
            if (parentNeighbors.size() == 1) {
                shapeQueue.enqueue(parentShape);
            }

            shapeHolesBasedOnStart[parentShapeAddress]++;
            shapeParentFromHole[thisShape] = parentShapeAddress;
        }
    }

    private void HandleSideTheSequel(int newIndex, IntArrayList currentHolder, int flatsize) {
        if (IndexInImage(newIndex, flatsize)) {
            currentHolder.add(parentPixels[newIndex]);
        } else {
            currentHolder.add(-1); // represents the image edge
        }
    }

    public int getShapeArea(int shapeStartingID) {
        int rootPixel = parentPixels[shapeStartingID];
        int address = shapeIndexAddressTable[rootPixel];
        return shapeAreasBasedOnStart[address];
    }

    public int getParentShapeID(int holeShapeIndex) {
        int rootPixel = parentPixels[holeShapeIndex];
        int address = shapeIndexAddressTable[rootPixel];
        return shapeParentFromHole[address];
    }

    public void calculateChildrenMap() {
        shapeChildrenMap = new int[getTotalShapeCount()][];
        for (int parentID = 0; parentID < shapeHolesBasedOnStart.length; parentID++) {
            shapeChildrenMap[parentID] = new int[shapeHolesBasedOnStart[parentID]];
        }

        int[] childCounters = new int[getTotalShapeCount()];

        for (int childID = 0; childID < shapeParentFromHole.length; childID++) {
            int parentID = shapeParentFromHole[childID];
            if (parentID != -1) {
                int index = childCounters[parentID]++;
                shapeChildrenMap[parentID][index] = childID;
            }
        }
    }

    public int[] getChildShapes(int shapeID) {
        return shapeChildrenMap[shapeID];
    }
}