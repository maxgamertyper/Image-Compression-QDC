package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.CornerPoint;

import java.util.ArrayList;

public class PixelChunk {
    private final ChannelColorTypes colorType;
    private final int colorValue;
    public int startingIndex;
    public ArrayList<CornerPoint> cornerPoints;
    public int convexCorners;
    private int totalArea;

    public PixelChunk(ChannelColorTypes colorType, int colorValue) {
        this.colorType = colorType;
        this.colorValue = colorValue;

        cornerPoints = new ArrayList<>();
    }

    public int getColorValue() {
        return colorValue;
    }

    public ChannelColorTypes getColorType() {
        return colorType;
    }

    public int getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(int totalArea) {
        this.totalArea = totalArea;
    }

    public int getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(int startingIndex) {
        this.startingIndex = startingIndex;
    }

    @Override
    public String toString() {
        return "Color Type: " + colorType + "; Color Value: " + colorValue + "; Starting Index: " + startingIndex +
                "; Area: " + totalArea + "\nCorners: " + cornerPoints;
    }

}
