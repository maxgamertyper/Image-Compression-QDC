package com.maxgamertyper1.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class PixelChunk {
    private final ChannelColorTypes colorType;
    private final int colorValue;
    public int StartingIndex;
    public ArrayList<CornerPoint> cornerPoints;
    private boolean isIsolated;
    private int IsolatedIndex;

    public PixelChunk(ChannelColorTypes colorType, int colorValue) {
        this.colorType = colorType;
        this.colorValue = colorValue;

        cornerPoints = new ArrayList<>();
    }

    public ChannelColorTypes getColorType() {
        return colorType;
    }

    public void setIsolated() {
        isIsolated = true;
    }

    public boolean getIsolated() {
        return isIsolated;
    }

    public int getIsolatedIndex() {
        return IsolatedIndex;
    }

    public void setIsolatedIndex(int index) {
        setIsolated();
        IsolatedIndex = index;
    }

    @Override
    public String toString() {
        return "Color Type: "+colorType+"; Color Value: "+colorValue+"; Starting Index: "+StartingIndex+"\nCorners: "+cornerPoints;
    }

}
