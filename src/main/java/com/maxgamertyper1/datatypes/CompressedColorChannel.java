package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;

public class CompressedColorChannel {
    private final RectangleRepresentative[] builtRectangles;
    private final CornerDataRepresentative[] builtCorners;
    private int[] isolatedIndexes;
    private int[] correspondingIsolatedValues;
    private final int initialPixel;
    private final ChannelColorTypes channelColor;


    public CompressedColorChannel(ChannelColorTypes channelColor, RectangleRepresentative[] builtRectangles, CornerDataRepresentative[] builtCorners, int initialPixel) {
        this.channelColor = channelColor;
        this.builtCorners = builtCorners;
        this.builtRectangles = builtRectangles;
        this.initialPixel = initialPixel;
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
}
