package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageMeta;

public class ChunkedImageRepresentative {
    private final ImageMeta metaData;
    private ChunkedColorChannel[] ChunkedChannels;
    private int ChunkCount;
    private int IndexCount;
    private int[] initialPaethValues;

    public ChunkedImageRepresentative(ImageMeta metaData) {
        this.metaData = metaData;
        InitializeChunkChannels();
    }

    public ImageMeta getMetaData() {
        return metaData;
    }

    private void InitializeChunkChannels() {
        if (metaData.grayscale()) {
            ChunkedChannels = new ChunkedColorChannel[2];
        } else {
            ChunkedChannels = new ChunkedColorChannel[4];
        }
    }

    public void sequentiallyOrder() {
        for (ChunkedColorChannel channel : ChunkedChannels) {
            channel.SequentiallyOrder();
        }
    }

    public ChunkedColorChannel[] getChunkedChannels() {
        return ChunkedChannels;
    }

    public void setChunkedChannels(ChunkedColorChannel[] ChunkedChannels) {
        this.ChunkedChannels = ChunkedChannels;
    }

    public void SetImageCompressionInformation() {
        for (ChunkedColorChannel channel : ChunkedChannels) {
            ChunkCount += channel.getChannelChunks().length;
            IndexCount += channel.getChannelIsolatedIndexes().length;
        }
    }

    public int[] getInitialPaethValues() {
        return initialPaethValues;
    }

    public void setInitialPaethValues(int[] values) {
        this.initialPaethValues = values;
    }
}