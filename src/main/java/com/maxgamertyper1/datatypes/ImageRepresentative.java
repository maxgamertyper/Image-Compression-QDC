package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageMeta;

public class ImageRepresentative {
    private final ImageMeta metaData;
    private final int[] initialPixels;
    private ColorChannel[] colorChannels;

    public ImageRepresentative(ImageMeta metaData) {
        this.metaData = metaData;

        intializeColorChannels();
        initialPixels = new int[colorChannels.length];
    }

    public ImageMeta getMetaData() {
        return metaData;
    }

    public ColorChannel[] getColorChannels() {
        return colorChannels;
    }

    private void intializeColorChannels() {
        int flatsize = metaData.flatsize();
        boolean opaque = metaData.opaque();

        if (metaData.grayscale()) {
            colorChannels = new ColorChannel[opaque ? 1 : 2];
            colorChannels[0] = new ColorChannel(ChannelColorTypes.Gray, flatsize);
            if (!opaque) colorChannels[1] = new ColorChannel(ChannelColorTypes.Alpha, flatsize);
        } else {
            colorChannels = new ColorChannel[opaque ? 3 : 4];
            colorChannels[0] = new ColorChannel(ChannelColorTypes.Red, flatsize);
            colorChannels[1] = new ColorChannel(ChannelColorTypes.Green, flatsize);
            colorChannels[2] = new ColorChannel(ChannelColorTypes.Blue, flatsize);
            if (!opaque) colorChannels[3] = new ColorChannel(ChannelColorTypes.Alpha, flatsize);
        }
    }

    public int getChannelIndex(ChannelColorTypes channelType) {
        int channelIndex = metaData.grayscale() ? channelType.ordinal() - 3 : channelType.ordinal();

        if (channelType == ChannelColorTypes.Alpha && !metaData.grayscale()) {
            channelIndex--;
        }

        return channelIndex;
    }

    public void setPixelColorChannel(ChannelColorTypes channelType, int flatindex, int value) {
        getOneColorChannel(channelType).setChannelPixel(flatindex, value);
    }

    public void setIntialPixel(ChannelColorTypes channelColor, int initialPixelColor) {
        int channelIndex = getChannelIndex(channelColor);

        this.initialPixels[channelIndex] = initialPixelColor;
    }

    public int[] getInitialPixels() {
        return this.initialPixels;
    }

    public ColorChannel getOneColorChannel(ChannelColorTypes channelType) {
        return this.getColorChannels()[this.getChannelIndex(channelType)];
    }
}