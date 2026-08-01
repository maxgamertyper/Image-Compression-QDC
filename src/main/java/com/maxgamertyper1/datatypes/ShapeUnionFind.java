package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageDataHolder;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;

import static com.maxgamertyper1.imagereading.PixelHelper.*;

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
        shapeIndexAddressTable = new int[ImageDataHolder.flatsize+1];
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

    public void calculatePerimiterAndHoles(int[] relativeFlags) { // O(NlogN)
        shapeHolesBasedOnStart = new int[getTotalShapeCount()];

        shapeParentFromHole = new int[getTotalShapeCount()];
        Arrays.fill(shapeParentFromHole, -1);

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

        IntArrayDeque shapeQueue = new IntArrayDeque(getTotalShapeCount()); // make size is the amount of shapes
        BitSet reusableBitset = new BitSet(getTotalShapeCount());

        for (int i=0; i<perimiterHolders.length; i++) {
            IntArrayList IAL = perimiterHolders[i];
            IAL.removeDuplicatesUnsorted();

            if (IAL.Size()==1) {
                shapeQueue.enqueue(i);
            }
        }

        while (shapeQueue.getSize()!=0) {
            int thisShape = shapeQueue.dequeue();
            IntArrayList currentNeighbors = perimiterHolders[thisShape];

            int parentShape = currentNeighbors.atIndex(0);
            int parentShapeAddress = shapeIndexAddressTable[parentShape];

            IntArrayList parentNeighbors = perimiterHolders[parentShapeAddress];
            parentNeighbors.SwapRemove(thisShape);
            if (parentNeighbors.Size()==1) {
                shapeQueue.enqueue(parentShape);
            }

            shapeHolesBasedOnStart[parentShapeAddress]++;
            shapeParentFromHole[thisShape] = parentShapeAddress;
        }
    }

    /*public void CalculateHolesVersion3(int[] relativeFlags) {
        int w = ImageDataHolder.width;

        int[] globalEnclosureMap = new int[getTotalShapeCount()];
        int[] localEnclosureMap = new int[getTotalShapeCount()];
        Arrays.fill(globalEnclosureMap,-1); // mark as unproccessed


        int[] jumpMap = new int[w]; // does not need set as it will only check if an index is less than the jump
        int[] firstSeenUngrouped = new int[getTotalShapeCount()];

        for (int yValue = 0; yValue<ImageDataHolder.height; yValue++) {
            Arrays.fill(localEnclosureMap,-1); // mark as unproccessed
            Arrays.fill(firstSeenUngrouped,-1); // mark as unseen

            for (int xValue=0; xValue<w; xValue++) {
                int shapeID = shapeIndexAddressTable[parentPixels[IndexCalculator(w,xValue,yValue)]];

                if (firstSeenUngrouped[shapeID]==-1 || firstSeenUngrouped[shapeID]==xValue-1) {
                    firstSeenUngrouped[shapeID]=xValue;
                } else {
                    // process shapes in between
                    int startWall = firstSeenUngrouped[shapeID];
                    int endWall = xValue;
                    int reXValue = startWall + 1;

                    while (reXValue < endWall) {
                        // check jump map to prevent reprocessing
                        if (jumpMap[reXValue] > reXValue) {
                            reXValue = jumpMap[reXValue]; // jump to pocket end
                            continue;
                        }

                        // get the inner shape
                        int innerRawID = parentPixels[IndexCalculator(w, reXValue, yValue)];
                        int innerShapeID = shapeIndexAddressTable[innerRawID];

                        localEnclosureMap[innerShapeID] = shapeID;

                        firstSeenUngrouped[innerShapeID] = -1; // remove its data

                        reXValue++;
                    }

                    jumpMap[startWall] = endWall;
                    firstSeenUngrouped[shapeID] = endWall;
                }
            }
        }
    }*/

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

    public int getParentShapeID(int holeShapeIndex) {
        return shapeParentFromHole[shapeIndexAddressTable[holeShapeIndex]];
    }
}