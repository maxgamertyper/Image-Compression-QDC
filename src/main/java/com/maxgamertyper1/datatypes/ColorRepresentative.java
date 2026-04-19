package com.maxgamertyper1.datatypes;

import java.util.BitSet;
import java.util.HashSet;

public class ColorRepresentative {
    private final ChannelColorTypes colorType;
    private final int colorValue;
    private BitSet colorInformation;
    public int firstIndex;

    public ColorRepresentative(ChannelColorTypes colorType, int colorValue) {
        this.colorType = colorType;
        this.colorValue = colorValue;
    }

    public int getColorValue() {
        return colorValue;
    }

    public ChannelColorTypes getColorType() {
        return colorType;
    }

    public BitSet getPixelIndexes() {
        return colorInformation;
    }

    public void setPixelIndexes(BitSet pixelIndexes) {
        this.colorInformation = pixelIndexes;
    }
}