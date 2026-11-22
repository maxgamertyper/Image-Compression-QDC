package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.CornerPoint;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import com.maxgamertyper1.imagereading.ImageMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import static com.maxgamertyper1.imagereading.PixelHelper.*;

public class SpatialCompressor {
    private final ChunkedImageRepresentative imageToCompress;
    private final ImageMeta metaData;
    private final CompressionConfig config;

    public SpatialCompressor(ChunkedImageRepresentative chunkedImage, CompressionConfig config) {
        imageToCompress = chunkedImage;
        metaData = chunkedImage.getMetaData();
        this.config = config;
    }

    private boolean PixelChunkUseRectangles(int convexCorners, int continuity, int holes) {
        if (config.cornersEnabled()) {
            return (continuity / ((double) convexCorners * convexCorners)) * holes <= 1; // prediction for rectangle efficiency basically
        } else {
            return true;
        }
    }

    public CompressedImageRepresentative QuadralizedDataCompressed() {
        ChunkedColorChannel[] chunkedColorChannels = imageToCompress.getChunkedChannels();
        CompressedColorChannel[] compressedColorChannels = new CompressedColorChannel[chunkedColorChannels.length];

        for (int i = 0; i < chunkedColorChannels.length; i++) {
            BitSet blockedPixels = new BitSet(metaData.flatsize()); // better here for threading later
            blockedPixels.set(0, metaData.flatsize());

            ChunkedColorChannel CCC = chunkedColorChannels[i];

            byte[] flagHolder = CCC.getRefactorFlags();
            ShapeUnionFind shapeOrganizer = CCC.getShapeOrganizer();
            EdgeMapHolder cardinalMaps = CCC.getCardinalMaps();
            int[] currentIsolated = CCC.getChannelIsolatedIndexes();


            ArrayList<RectangleRepresentative> finalRectangleHolder = new ArrayList<>();
            IntArrayList finalIsolatedIndexes = new IntArrayList(currentIsolated.length);
            IntArrayList IsolatedIndexesValues = new IntArrayList(currentIsolated.length);
            ArrayList<CornerDataRepresentative> finalCornerHolder = new ArrayList<>();

            for (int isolatedIndex : currentIsolated) {
                finalIsolatedIndexes.add(isolatedIndex);
            }
            for (int indexValue : CCC.getChannelIsolatedCorrespondingColor()) {
                IsolatedIndexesValues.add(indexValue);
            }


            for (PixelChunk pixelChunk : CCC.getChannelChunks()) {
                int startingIndex = pixelChunk.startingIndex;
                boolean useRectangles = PixelChunkUseRectangles(pixelChunk.convexCorners, shapeOrganizer.getContinuity(startingIndex), shapeOrganizer.getHoles(startingIndex));
                int[] indexBucket = shapeOrganizer.getIndexGrouping(pixelChunk.startingIndex);

                if (indexBucket.length < config.minRectangleSize()) {
                    for (int index : indexBucket) {
                        finalIsolatedIndexes.add(index);
                        IsolatedIndexesValues.add(pixelChunk.getColorValue());
                    }
                    continue;
                }

                if (useRectangles) {
                    // NOTE: should probably not do this to make speed better tbh
                    ArrayList<RectangleRepresentative> brokenChunk = RectangleCreator(pixelChunk, flagHolder, shapeOrganizer, cardinalMaps, blockedPixels);

                    finalRectangleHolder.addAll(brokenChunk);
                } else {
                    CornerDataRepresentative holder = CalculateCornerData(pixelChunk, CCC.getComputedFlags());
                    finalCornerHolder.add(holder);
                }

                int[] shapeData = shapeOrganizer.getIndexGrouping(pixelChunk.startingIndex);
                blockedPixels.set(shapeData[0], shapeData[shapeData.length - 1]);
            }

            // sequential sorting
            finalRectangleHolder.sort(Comparator.comparingInt(RectangleRepresentative::startingIndex));
            finalCornerHolder.sort(Comparator.comparingInt(CornerDataRepresentative::startingIndex));

            //parallel sort the isolated indexes and their values
            int[] isolatedIndexes = finalIsolatedIndexes.toIntArray();
            int[] indexValues = IsolatedIndexesValues.toIntArray();

            Integer[] arrayIndices = new Integer[isolatedIndexes.length];
            for (int j = 0; j < arrayIndices.length; j++) arrayIndices[j] = j;

            Arrays.sort(arrayIndices, Comparator.comparingInt(idx -> isolatedIndexes[idx]));

            int[] sortedKeys = new int[isolatedIndexes.length];
            int[] sortedValues = new int[indexValues.length];

            for (int j = 0; j < arrayIndices.length; j++) {
                int arrayIndex = arrayIndices[j];
                sortedKeys[j] = isolatedIndexes[arrayIndex];
                sortedValues[j] = indexValues[arrayIndex];
            }
            // done

            //store!
            RectangleRepresentative[] sortedRectangles = finalRectangleHolder.toArray(new RectangleRepresentative[0]);
            CornerDataRepresentative[] sortedCorners = finalCornerHolder.toArray(new CornerDataRepresentative[0]);

            CompressedColorChannel compressedColorChannel = new CompressedColorChannel(CCC.getChannelType(), sortedRectangles, sortedCorners, imageToCompress.getInitialPaethValues()[i]);
            compressedColorChannel.setIsolatedIndexes(sortedKeys, sortedValues);
            compressedColorChannels[i] = compressedColorChannel;
            compressedColorChannel.setShapeOrganizer(shapeOrganizer);

        }

        return new CompressedImageRepresentative(metaData, compressedColorChannels);
    }

    public CornerDataRepresentative CalculateCornerData(PixelChunk pixelChunk, int[] flagHolder) {
        int dataSize = pixelChunk.cornerPoints.size();
        int[] flagMask = new int[dataSize];
        int[] indexDelta = new int[dataSize - 1];

        flagMask[0] = pixelChunk.cornerPoints.getFirst().flagmask(); // always 9?

        int prevIndex = pixelChunk.cornerPoints.getFirst().index();
        for (int i = 1; i < dataSize; i++) {
            CornerPoint CP = pixelChunk.cornerPoints.get(i);
            int currentIndex = CP.index();

            indexDelta[i - 1] = currentIndex - prevIndex;
            flagMask[i] = CP.flagmask();

            prevIndex = currentIndex;
        }

        return new CornerDataRepresentative(indexDelta, pixelChunk.startingIndex, flagMask, pixelChunk.getColorValue(), metaData.width());
    }

    public ArrayList<RectangleRepresentative> RectangleCreator(PixelChunk pixelChunk, byte[] flagHolder, ShapeUnionFind shapeOrganizer, EdgeMapHolder EMH, BitSet blockedPixels) { // greedy algorith
        int area = pixelChunk.getTotalArea();

        ArrayList<CornerPoint> processingList = new ArrayList<>(pixelChunk.cornerPoints);
        ArrayList<RectangleRepresentative> returnHolder = new ArrayList<RectangleRepresentative>();

        int[] raw = shapeOrganizer.getRawData();

        for (int index : shapeOrganizer.getIndexGrouping(pixelChunk.startingIndex)) {
            blockedPixels.clear(index);
        }

        while (area > 0) {
            // find the optimal rectangle
            RectangleRepresentative currentGlobalBest = new RectangleRepresentative(0, 0, 1, 0);

            for (CornerPoint processingCorner : processingList) {
                int cornerIndex = processingCorner.index();

                int flag = processingCorner.flagmask();

                RectangleRepresentative[] longRectangles = CalculateLongRectangles(EMH, cornerIndex, flag, pixelChunk, blockedPixels, metaData);

                if (longRectangles.length == 1) {
                    currentGlobalBest = longRectangles[0];
                    break;
                }

                RectangleRepresentative localKingRect = longRectangles[1]; // the initial wide rectangle (wide gets priority)

                RectangleRepresentative longRectangle = longRectangles[0];

                if (longRectangle.betterThan(localKingRect)) {
                    localKingRect = longRectangle;
                }

                RectangleRepresentative squareCalculation = CalculateSquare(EMH, cornerIndex, flag, pixelChunk, blockedPixels);

                // is square better?
                if (squareCalculation.betterThan(localKingRect)) {
                    localKingRect = squareCalculation;
                }

                // is this local king better than the other ones?
                if (localKingRect.betterThan(currentGlobalBest)) {
                    currentGlobalBest = localKingRect;
                }
            }

            if (currentGlobalBest.area() == 0) {
                throw new IllegalStateException("No rectangle found, " + area + " pixels uncovered");
            }

            // process the rectangle
            area -= currentGlobalBest.area();
            processBestRectangle(currentGlobalBest, processingList, blockedPixels);
            returnHolder.add(currentGlobalBest);
        }

        return returnHolder;
    }

    public void processBestRectangle(RectangleRepresentative currentGlobalBest, ArrayList<CornerPoint> processingList, BitSet blockedPixels) {
        int w = metaData.width();

        // 1) update bitset
        for (int y = 0; y < currentGlobalBest.height(); y++) {
            int rowStart = currentGlobalBest.startingIndex() + (y * w);
            blockedPixels.set(rowStart, rowStart + currentGlobalBest.width());
        }
        // 2) remove swallowed corners
        processingList.removeIf(cp -> blockedPixels.get(cp.index()));

        int[] startingCoordinates = ReverseIndexCalculator(w, currentGlobalBest.startingIndex());

        // 3) add new corners
        int right = startingCoordinates[0] + currentGlobalBest.width();
        int left = startingCoordinates[0] - 1;
        int top = startingCoordinates[1] - 1;
        int bottom = startingCoordinates[1] + currentGlobalBest.height();

        // width and height are inclusive so
        //top and bottom boundary check
        for (int x = left; x <= right; x++) {
            int topTestingIndex = IndexCalculator(w, x, top);
            int bottomTestingIndex = IndexCalculator(w, x, bottom);

            if (IndexInImage(topTestingIndex, metaData.flatsize()) && !blockedPixels.get(topTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(blockedPixels, topTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
            if (IndexInImage(bottomTestingIndex, metaData.flatsize()) && !blockedPixels.get(bottomTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(blockedPixels, bottomTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
        }

        // left and right boundary check
        for (int y = top; y <= bottom; y++) {
            int leftTestingIndex = IndexCalculator(w, left, y);
            int rightTestingIndex = IndexCalculator(w, right, y);

            if (left >= 0) {
                if (IndexInImage(leftTestingIndex, metaData.flatsize()) && !blockedPixels.get(leftTestingIndex)) {
                    CornerPoint newCP = translateToCornerPoint(blockedPixels, leftTestingIndex);
                    if (newCP.trueCorner()) {
                        processingList.add(newCP);
                    }
                }
            }
            if (right < metaData.width()) {
                if (IndexInImage(rightTestingIndex, metaData.flatsize()) && !blockedPixels.get(rightTestingIndex)) {
                    CornerPoint newCP = translateToCornerPoint(blockedPixels, rightTestingIndex);
                    if (newCP.trueCorner()) {
                        processingList.add(newCP);
                    }
                }
            }
        }

    }

    private CornerPoint translateToCornerPoint(BitSet blockedPixels, int flatIndex) {
        int w = metaData.width();
        int flagmask = 0;

        int left = flatIndex - 1;
        int right = flatIndex + 1;
        int above = flatIndex - w;
        int below = flatIndex + w;

        if (flatIndex % w == 0 || blockedPixels.get(left)) flagmask |= EdgeFlags.LEFT;
        if ((flatIndex + 1) % w == 0 || blockedPixels.get(right)) flagmask |= EdgeFlags.RIGHT;
        if (below >= metaData.flatsize() || blockedPixels.get(below)) flagmask |= EdgeFlags.DOWN;
        if (above < 0 || blockedPixels.get(above)) flagmask |= EdgeFlags.UP;

        return new CornerPoint(flatIndex, flagmask);
    }

    public RectangleRepresentative[] CalculateLongRectangles(EdgeMapHolder EMH, int cornerIndex, int flag, PixelChunk PC, BitSet blockedPixels, ImageMeta metaData) {
        int directionData = ~flag; // not-ing the flag gives us expansion directions
        int[] edgeReach = CalculateLimits(directionData, cornerIndex, EMH);
        int[] freeReach = ProcessedDataInclusion(blockedPixels, edgeReach, directionData, cornerIndex, metaData.flatsize());
        int longRectangleWidth = 1;
        int wideRectangleHeight = 1;

        RectangleRepresentative[] returnHolder = new RectangleRepresentative[2];

        int probeIndex = cornerIndex;
        int change = (((directionData & 1) != 0) ? -1 : 1); // -1 if im checking left, 1 if im checking right

        for (int xChange = 0; xChange < freeReach[0]; xChange++) { // the tall rectangle
            probeIndex += change;

            if (!IndexsInSameRow(probeIndex, cornerIndex, metaData.width()) || !IndexInImage(probeIndex, metaData.flatsize()))
                break; // DOES IT MATTER IF IT BLEEDS INTO THE NEXT ROW?

            int[] testingExpansionLimits = CalculateLimits(directionData, probeIndex, EMH);
            int[] realTestingExpansionLimits = ProcessedDataInclusion(blockedPixels, testingExpansionLimits, directionData, probeIndex, metaData.flatsize());


            if (realTestingExpansionLimits[1] < freeReach[1]) { // see next one for change
                break;
            }
            longRectangleWidth++;
        }
        RectangleRepresentative longRect = new RectangleRepresentative(
                freeReach[1] + 1,
                longRectangleWidth,
                rectangleStartIndex(cornerIndex, longRectangleWidth, freeReach[1] + 1, directionData),
                PC.getColorValue()
        );

        if (longRect.area() == PC.getTotalArea()) {
            return new RectangleRepresentative[]{longRect}; // return the perfect fit
        }
        returnHolder[1] = longRect; // otherwise keep it there


        probeIndex = cornerIndex;
        change = metaData.width() * (((directionData & 8) != 0) ? -1 : 1); // -height if im checking up, and height if im checking down
        for (int yChange = 0; yChange < freeReach[1]; yChange++) { // the wide rectnagle, expands verically
            probeIndex += change;

            if (!IndexInImage(probeIndex, metaData.flatsize())) break;

            int[] testingExpansionLimits = CalculateLimits(directionData, probeIndex, EMH);
            int[] realTestingExpansionLimits = ProcessedDataInclusion(blockedPixels, testingExpansionLimits, directionData, probeIndex, metaData.flatsize());

            if (realTestingExpansionLimits[0] < freeReach[0]) { // changed so if a new spot's info has longer it's still added
                break;
            }
            wideRectangleHeight++;
        }

        RectangleRepresentative wideRect = new RectangleRepresentative(
                wideRectangleHeight,
                freeReach[0] + 1,
                rectangleStartIndex(cornerIndex, freeReach[0] + 1, wideRectangleHeight, directionData),
                PC.getColorValue()
        );

        if (wideRect.area() == PC.getTotalArea()) {
            return new RectangleRepresentative[]{wideRect}; // return the perfect fit
        }
        returnHolder[0] = wideRect; // otherwise keep it there

        return returnHolder;
    }

    public RectangleRepresentative CalculateSquare(EdgeMapHolder EMH, int flatAnchorIndex, int flag, PixelChunk PC, BitSet blockedPixels) {
        int directionData = ~flag;

        if (Integer.bitCount(directionData) < 2)
            return new RectangleRepresentative(1, 1, flatAnchorIndex, PC.getColorValue());

        int[] expansionLimits = CalculateLimits(directionData, flatAnchorIndex, EMH);
        int[] trueLimits = ProcessedDataInclusion(blockedPixels, expansionLimits, directionData, flatAnchorIndex, blockedPixels.size());

        int relativeIndex = flatAnchorIndex;
        int dx = (((directionData & 1) != 0) ? -1 : 1);
        int dy = metaData.width() * (((directionData & 8) != 0) ? -1 : 1);

        int size = 1;
        for (; size <= Math.min(trueLimits[0], trueLimits[1]); size++) {
            relativeIndex += dx + dy;

            int[] relativeExpansionLimits = CalculateLimits(flag, relativeIndex, EMH); // we actually want to calculate in the direction of the flag to check if theres any intersection
            int[] relativeTrueLimits = ProcessedDataInclusion(blockedPixels, relativeExpansionLimits, directionData, relativeIndex, blockedPixels.size());

            if (relativeTrueLimits[0] < size || relativeTrueLimits[1] < size) {
                break;
            }
        }

        return new RectangleRepresentative(size, size, rectangleStartIndex(flatAnchorIndex, size, size, directionData), PC.getColorValue());
    }

    private int rectangleStartIndex(int anchorIndex, int rectWidth, int rectHeight, int directions) {
        int start = anchorIndex;
        if (EdgeFlags.has(directions, EdgeFlags.LEFT)) {
            start -= rectWidth - 1; // anchor is the right edge
        }
        if (EdgeFlags.has(directions, EdgeFlags.UP)) {
            start -= (rectHeight - 1) * metaData.width(); // anchor is the bottom edge
        }
        return start;
    }

    private int[] ProcessedDataInclusion(BitSet blockedPixels, int[] calculateLimitReturn, int calculationDirections, int cornerIndex, int flatsize) {
        int limitX = calculateLimitReturn[0];
        int limitY = calculateLimitReturn[1];

        int trueLimitX = 1; // otherwise the first test is just the index
        int trueLimitY = 1;

        int dx = (((calculationDirections & 1) != 0) ? -1 : 1);
        for (; trueLimitX <= limitX; trueLimitX++) {
            int testingLocation = cornerIndex + (trueLimitX * dx);

            if (!IndexInImage(testingLocation, flatsize) || blockedPixels.get(testingLocation)) break;
        }

        int dy = metaData.width() * (((calculationDirections & 8) != 0) ? -1 : 1);
        for (; trueLimitY <= limitY; trueLimitY++) {
            int testingLocation = cornerIndex + (trueLimitY * dy);

            if (!IndexInImage(testingLocation, flatsize) || blockedPixels.get(testingLocation)) break;
        }

        return new int[]{trueLimitX - 1, trueLimitY - 1};
    }

    public int[] CalculateLimits(int calculationDirections, int cornerIndex, EdgeMapHolder edgeMaps) {
        int width = metaData.width();
        int anchorX = cornerIndex % width;
        int anchorY = cornerIndex / width;

        int limitX = 0;
        if (EdgeFlags.has(calculationDirections, EdgeFlags.LEFT)) { // left edge open
            limitX = anchorX - edgeMaps.prevRightEdge(anchorY, anchorX) - 1;
        } else if (EdgeFlags.has(calculationDirections, EdgeFlags.RIGHT)) { // right edge open
            limitX = edgeMaps.nextRightEdge(anchorY, anchorX) - anchorX;
        }

        int limitY = 0;
        if (EdgeFlags.has(calculationDirections, EdgeFlags.DOWN)) { // down open
            limitY = edgeMaps.nextBottomEdge(anchorX, anchorY) - anchorY;
        } else if (EdgeFlags.has(calculationDirections, EdgeFlags.UP)) { // up open
            limitY = anchorY - edgeMaps.prevBottomEdge(anchorX, anchorY) - 1;
        }

        return new int[]{limitX, limitY};
    }
}
