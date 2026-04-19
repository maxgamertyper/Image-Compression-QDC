package com.maxgamertyper1.datatypes;

import java.util.ArrayList;

public class ColorChannel {
    private int[] channelData;
    private final ChannelColorTypes channelColor;

    public ColorChannel(ChannelColorTypes channelColor, int datalength) {
        this.channelColor=channelColor;
        channelData = new int[datalength];
    }

    public void setChannelData(int[] pixeldata) {
        channelData = pixeldata;
    }

    public int[] getChannelData() {
        return channelData;
    }

    public void setChannelPixel(int flatIndex, int pixelData) {
        if (channelData.length-1 < flatIndex || flatIndex<0) {
            System.err.println("index out of ColorChannel Size; Index: "+flatIndex+" Size: "+(channelData.length-1));
            System.exit(1);
            return;
        }
        channelData[flatIndex] = pixelData;
    }

    public ChannelColorTypes getChannelType() {
        return this.channelColor;
    }
}