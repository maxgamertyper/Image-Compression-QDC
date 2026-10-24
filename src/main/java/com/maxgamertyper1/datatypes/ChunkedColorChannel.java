package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.datatypes.records.EdgeMapHolder;

import java.util.Arrays;

public class ChunkedColorChannel {
    private final ChannelColorTypes channelColor;
    private final ShapeUnionFind shapeOrganizer;
    private final EdgeMapHolder cardinalMaps;
    private final int[] computedFlags;
    private final byte[] refactorFlags;
    private PixelChunk[] channelChunks;
    private int[] channelIsolatedIndexes;
    private int[] channelIsolatedCorrespondingColor;

    public ChunkedColorChannel(ChannelColorTypes channelColor, int pixelChunkDataLength, int isolatedIndexesDataLength, ShapeUnionFind shapeOrganizer, EdgeMapHolder cardinalMaps, int[] computedFlags) {
        this.channelColor = channelColor;
        this.shapeOrganizer = shapeOrganizer;
        this.cardinalMaps = cardinalMaps;
        this.computedFlags = computedFlags;
        refactorFlags = new byte[1];
        channelChunks = new PixelChunk[pixelChunkDataLength];
        channelIsolatedIndexes = new int[isolatedIndexesDataLength];
    }

    public ChunkedColorChannel(ChannelColorTypes channelColor, int pixelChunkDataLength, int isolatedIndexesDataLength, ShapeUnionFind shapeOrganizer, EdgeMapHolder cardinalMaps, byte[] refactorFlags) {
        this.channelColor = channelColor;
        this.shapeOrganizer = shapeOrganizer;
        this.cardinalMaps = cardinalMaps;
        this.refactorFlags = refactorFlags;
        computedFlags = new int[1];
        channelChunks = new PixelChunk[pixelChunkDataLength];
        channelIsolatedIndexes = new int[isolatedIndexesDataLength];
    }

    public EdgeMapHolder getCardinalMaps() {
        return cardinalMaps;
    }

    public int[] getComputedFlags() {
        return computedFlags;
    }

    public byte[] getRefactorFlags() {
        return refactorFlags;
    }

    public ShapeUnionFind getShapeOrganizer() {
        return shapeOrganizer;
    }

    public PixelChunk[] getChannelChunks() {
        return channelChunks;
    }

    public void setChannelChunks(PixelChunk[] pixeldata) {
        channelChunks = pixeldata;
    }

    public void setChannelChunk(int flatIndex, PixelChunk pixelData) {
        if (channelChunks.length - 1 < flatIndex || flatIndex < 0) {
            System.err.println("index out of ColorChannel Size; Index: " + flatIndex + " Size: " + (channelChunks.length - 1));
            System.exit(1);
            return;
        }
        channelChunks[flatIndex] = pixelData;
    }

    public ChannelColorTypes getChannelType() {
        return this.channelColor;
    }

    public void setIsolatedIndexes(int[] isolatedIndexesData, int[] IsolatedCorrespondingColor) {
        channelIsolatedIndexes = isolatedIndexesData;
        channelIsolatedCorrespondingColor = IsolatedCorrespondingColor;
    }

    public int[] getChannelIsolatedIndexes() {
        return channelIsolatedIndexes;
    }

    public int[] getChannelIsolatedCorrespondingColor() {
        return channelIsolatedCorrespondingColor;
    }

    public void SequentiallyOrder() {
        Arrays.sort(channelIsolatedIndexes);
        Arrays.sort(channelChunks, (a, b) -> Integer.compare(a.startingIndex, b.startingIndex));
    }
}