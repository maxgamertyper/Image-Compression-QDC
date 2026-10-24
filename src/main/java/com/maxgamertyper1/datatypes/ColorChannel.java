package com.maxgamertyper1.datatypes;

public class ColorChannel {
    private final ChannelColorTypes channelColor;
    private final int[] channelData;

    public ColorChannel(ChannelColorTypes channelColor, int datalength) {
        this.channelColor = channelColor;
        channelData = new int[datalength];
    }

    public int[] getChannelData() {
        return channelData;
    }

    public void setChannelPixel(int flatIndex, int pixelData) {
        if (channelData.length - 1 < flatIndex || flatIndex < 0) {
            System.err.println("index out of ColorChannel Size; Index: " + flatIndex + " Size: " + (channelData.length - 1));
            System.exit(1);
            return;
        }
        channelData[flatIndex] = pixelData;
    }

    public ChannelColorTypes getChannelType() {
        return this.channelColor;
    }
}