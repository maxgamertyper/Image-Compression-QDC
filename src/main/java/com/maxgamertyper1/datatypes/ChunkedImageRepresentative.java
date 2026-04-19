package com.maxgamertyper1.datatypes;

import java.util.Arrays;
import java.util.Comparator;

public class ChunkedImageRepresentative {
    private boolean SequentiallyOrdered = false;
    private ChunkedColorChannel[] ChunkedChannels;
    private final boolean grayScale;
    private final boolean uniformMaxAlpha;
    private int ChunkCount;
    private int IndexCount;

    public ChunkedImageRepresentative(boolean isGrayscale, boolean uniformMaxAlpha) {
        this.grayScale = isGrayscale;
        this.uniformMaxAlpha = uniformMaxAlpha;
        InitializeChunkChannels();
    }

    private void InitializeChunkChannels() {
        if (grayScale) {
            ChunkedChannels = new ChunkedColorChannel[2];
        } else {
            ChunkedChannels = new ChunkedColorChannel[4];
        }
    }

    public void setChunkedColorChannel(ChannelColorTypes colorType, ChunkedColorChannel channel) {
        ChunkedChannels[getChannelIndex(colorType)] = channel;
    }

    public void setChunkedChannels(ChunkedColorChannel[] ChunkedChannels) {
        this.ChunkedChannels = ChunkedChannels;
    }

    public int getChannelIndex(ChannelColorTypes channelType) {
        int channelIndex = grayScale ? channelType.ordinal()-3 : channelType.ordinal();

        if (channelType==ChannelColorTypes.Alpha && !grayScale) {
            channelIndex--;
        }

        return channelIndex;
    }

    public boolean getSequentiallyOrdered(){
        return SequentiallyOrdered;
    }

    public void SequentiallyOrder() {
        SequentiallyOrdered=true;

        for (ChunkedColorChannel channel : ChunkedChannels) {
            channel.SequentiallyOrder();
        }
    }

    public ChunkedColorChannel[] getChunkedChannels() {
        return ChunkedChannels;
    }

    public void SetImageCompressionInformation() {
        for (ChunkedColorChannel channel : ChunkedChannels) {
            ChunkCount+=channel.getChannelChunks().length;
            IndexCount+=channel.getChannelIsolatedIndexes().length;
        }
    }

    public int getChunkCount() {
        return ChunkCount;
    }

    public int getIndexCount() {
        return IndexCount;
    }
}