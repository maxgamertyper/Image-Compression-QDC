package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;

import java.util.Arrays;

public class CompressedColorChannel {
    private final RectangleRepresentative[] builtRectangles;
    private final CornerDataRepresentative[] builtCorners;
    private int[] isolatedIndexes;
    private int[] correspondingIsolatedValues;
    private final int initialPixel;
    private final ChannelColorTypes channelColor;
    private ShapeUnionFind shapeOrganizer;

    //Corner override mapping
    private int[] cornerRectanglesMap;
    private int[] cornerRectOffsets;
    private int[] cornerRectanglesBucketed;

    private int[] cornerIndexesMap;
    private int[] cornerIndexOffsets;
    private int[] cornerIndexesBucketed;

    private int[] cornerShapeMap;

    public CompressedColorChannel(ChannelColorTypes channelColor, RectangleRepresentative[] builtRectangles, CornerDataRepresentative[] builtCorners, int initialPixel) {
        this.channelColor = channelColor;
        this.builtCorners = builtCorners;
        this.builtRectangles = builtRectangles;
        this.initialPixel = initialPixel;
    }

    public int[] getCornerIndexesMap() {
        return cornerIndexesMap;
    }

    public int[] getCornerIndexOffsets() {
        return cornerIndexOffsets;
    }

    public int[] getCornerRectanglesMap() {
        return cornerRectanglesMap;
    }

    public int[] getCornerRectOffsets() {
        return cornerRectOffsets;
    }

    public ShapeUnionFind getShapeOrganizer() {
        return shapeOrganizer;
    }

    public int[] getCornerShapeMap() {
        return cornerShapeMap;
    }

    public void setShapeOrganizer(ShapeUnionFind shapeOrganizer) {
        this.shapeOrganizer = shapeOrganizer;
    }

    public void setIsolatedIndexes(int[] isolatedIndexesData, int[] IsolatedCorrespondingColor) {
        isolatedIndexes = isolatedIndexesData;
        correspondingIsolatedValues = IsolatedCorrespondingColor;
    }

    public ChannelColorTypes getChannelColorType() {
        return channelColor;
    }

    public int getIsolatedCount() {
        return isolatedIndexes.length;
    }

    public int[] getCorrespondingIsolatedValues() {
        return correspondingIsolatedValues;
    }

    public int getRectangleCount() {
        return builtRectangles.length;
    }

    public int getCornerCount() {
        return builtCorners.length;
    }

    public int[] getIsolatedIndexes() {
        return isolatedIndexes;
    }

    public CornerDataRepresentative[] getBuiltCorners() {
        return builtCorners;
    }

    public RectangleRepresentative[] getBuiltRectangles() {
        return builtRectangles;
    }

    public int getInitialPixel() {
        return initialPixel;
    }

    public void zigzagIsolated() {
        for (int i=0; i < correspondingIsolatedValues.length; i++) {
            correspondingIsolatedValues[i] = zigzagEncode(correspondingIsolatedValues[i]);
        }
    }

    public void calculateCornerOverrides() {
        calculateCornerShapeMap();
        calculateCornerRectangleOverrides();
        calculateCornerIndexesOverrides();
        //TODO: also map corners to other corners in case they stack on one another
    }

    private void calculateCornerShapeMap() {
        cornerShapeMap = new int[shapeOrganizer.getTotalShapeCount()];
        Arrays.fill(cornerShapeMap, -1);

        int[] parentMap = shapeOrganizer.getRawData();


        for (int i=0; i < builtCorners.length; i++) {
            CornerDataRepresentative corner = builtCorners[i];
            int cornerShapeID = parentMap[corner.startingIndex()];

            cornerShapeMap[cornerShapeID] = i;
        }
    }

    public static int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-2, ...
        return (number << 1) ^ (number >> 31);
    }

    private void calculateCornerRectangleOverrides() {
        cornerRectanglesMap = new int[getRectangleCount()];
        cornerRectOffsets = new int[getCornerCount() + 1];
        Arrays.fill(cornerRectanglesMap, -1);

        // calculate the corner mappings
        for (int i=0; i < builtRectangles.length; i++) {
            RectangleRepresentative thisRectangle = builtRectangles[i];
            int shapeIDParent = shapeOrganizer.getParentShapeID(thisRectangle.startingIndex());

            if (shapeIDParent == -1) continue;

            cornerRectanglesMap[i] = cornerShapeMap[shapeIDParent];
        }

        // calculate the offsets
        for (int cornerIndex : cornerRectanglesMap) {
            if (cornerIndex == -1) continue;
            cornerRectOffsets[cornerIndex + 1]++;
        }
        // convert counts into offsets
        for (int c = 0; c < cornerRectOffsets.length - 1; c++) {
            cornerRectOffsets[c + 1] += cornerRectOffsets[c];
        }

        int totalAttached = cornerRectOffsets[getCornerCount()];
        cornerRectanglesBucketed = new int[totalAttached];
        int[] writeCursors = Arrays.copyOf(cornerRectOffsets,cornerRectOffsets.length);

        for (int i = 0; i < cornerRectanglesMap.length; i++) {
            int cornerIndex = cornerRectanglesMap[i];

            if (cornerIndex == -1) continue;
            cornerRectanglesBucketed[writeCursors[cornerIndex]++] = i;
        }
    }

    private void calculateCornerIndexesOverrides() {
        cornerIndexesMap = new int[isolatedIndexes.length];
        cornerIndexOffsets = new int[getCornerCount() + 1];
        Arrays.fill(cornerIndexesMap, -1);

        // calculate the corner mappings
        for (int i=0; i < isolatedIndexes.length; i++) {
            int thisIndex = isolatedIndexes[i];
            int shapeIDParent = shapeOrganizer.getParentShapeID(thisIndex);

            if (shapeIDParent == -1) continue;

            cornerIndexesMap[i] = cornerShapeMap[shapeIDParent];
        }

        // calculate the offsets
        for (int cornerIndex : cornerIndexesMap) {
            if (cornerIndex == -1) continue;
            cornerIndexOffsets[cornerIndex + 1]++;
        }
        // convert counts into offsets
        for (int c = 0; c < cornerIndexOffsets.length - 1; c++) {
            cornerIndexOffsets[c + 1] += cornerIndexOffsets[c];
        }

        int totalAttached = cornerIndexOffsets[getCornerCount()];
        cornerIndexesBucketed = new int[totalAttached];
        int[] writeCursors = Arrays.copyOf(cornerIndexOffsets,cornerIndexOffsets.length);

        for (int i = 0; i < cornerIndexesMap.length; i++) {
            int cornerIndex = cornerIndexesMap[i];

            if (cornerIndex == -1) continue;
            cornerIndexesBucketed[writeCursors[cornerIndex]++] = i;
        }
    }
}