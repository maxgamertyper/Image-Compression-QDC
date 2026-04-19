package com.maxgamertyper1.datatypes;

import java.util.Arrays;

public class ChunkedColorChannel {
    private PixelChunk[] channelChunks;
    private int[] channelIsolatedIndexes;
    private final ChannelColorTypes channelColor;

    public ChunkedColorChannel(ChannelColorTypes channelColor, int pixelChunkDataLength, int isolatedIndexesDataLength) {
        this.channelColor=channelColor;
        channelChunks = new PixelChunk[pixelChunkDataLength];
        channelIsolatedIndexes = new int[isolatedIndexesDataLength];
    }

    public void setChannelChunks(PixelChunk[] pixeldata) {
        channelChunks = pixeldata;
    }

    public PixelChunk[] getChannelChunks() {
        return channelChunks;
    }

    public void setChannelChunk(int flatIndex, PixelChunk pixelData) {
        if (channelChunks.length-1 < flatIndex || flatIndex<0) {
            System.err.println("index out of ColorChannel Size; Index: "+flatIndex+" Size: "+(channelChunks.length-1));
            System.exit(1);
            return;
        }
        channelChunks[flatIndex] = pixelData;
    }

    public ChannelColorTypes getChannelType() {
        return this.channelColor;
    }

    public void setIsolatedIndexes(int[] isolatedIndexesData) {
        channelIsolatedIndexes = isolatedIndexesData;
    }

    public int[] getChannelIsolatedIndexes() {
        return channelIsolatedIndexes;
    }

    public void SequentiallyOrder() {
        Arrays.sort(channelIsolatedIndexes);
        Arrays.sort(channelChunks, (a,b) -> Integer.compare(a.StartingIndex, b.StartingIndex));
    }
}