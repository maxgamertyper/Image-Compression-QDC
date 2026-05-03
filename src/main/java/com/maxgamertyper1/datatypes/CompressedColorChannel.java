package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;

public class CompressedColorChannel {
    private final RectangleRepresentative[] builtRectangles;
    private final CornerDataRepresentative[] builtCorners;
    private int[] isolatedIndexes;
    private int[] correspondingIsolatedValues;
    private final ChannelColorTypes channelColor;


    public CompressedColorChannel(ChannelColorTypes channelColor,RectangleRepresentative[] builtRectangles, CornerDataRepresentative[] builtCorners) {
        this.channelColor = channelColor;
        this.builtCorners = builtCorners;
        this.builtRectangles = builtRectangles;
    }

    public void setIsolatedIndexes(int[] isolatedIndexesData, int[] IsolatedCorrespondingColor) {
        isolatedIndexes = isolatedIndexesData;
        correspondingIsolatedValues = IsolatedCorrespondingColor;
    }


}
