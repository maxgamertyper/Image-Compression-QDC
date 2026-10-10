package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.CornerPoint;

import java.util.ArrayList;

public class PixelChunk {
    private final ChannelColorTypes colorType;
    private final int colorValue;
    private int totalArea;
    public int startingIndex;
    public ArrayList<CornerPoint> cornerPoints;
    public int convexCorners;

    public PixelChunk(ChannelColorTypes colorType, int colorValue) {
        this.colorType = colorType;
        this.colorValue = colorValue;

        cornerPoints = new ArrayList<>();
    }

    public void setTotalArea(int totalArea) {
        this.totalArea = totalArea;
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

    public int getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(int startingIndex) {
        this.startingIndex = startingIndex;
    }

    @Override
    public String toString() {
        return "Color Type: "+colorType+"; Color Value: "+colorValue+"; Starting Index: "+ startingIndex +"\nCorners: "+cornerPoints;
    }

}
