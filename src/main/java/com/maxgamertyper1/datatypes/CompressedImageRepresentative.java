package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageMeta;

import java.util.Arrays;
import java.util.Comparator;

public class CompressedImageRepresentative {
    private final CompressedColorChannel[] CompressedChannels;
    private final ImageMeta metaData;

    public CompressedImageRepresentative(ImageMeta metaData, CompressedColorChannel[] CompressedChannels) {
        this.CompressedChannels = CompressedChannels;
        this.metaData = metaData;
    }

    public ImageMeta getMetaData() {
        return metaData;
    }

    public CompressedColorChannel[] getCompressedChannels() {
        return CompressedChannels;
    }

    public void sortChannelOrder() {
        Arrays.sort(
                CompressedChannels,
                Comparator.comparingInt(a -> a.getChannelColorType().getOrderValue())
        );
    }
}
