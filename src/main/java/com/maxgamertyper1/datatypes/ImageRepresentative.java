package com.maxgamertyper1.datatypes;

public class ImageRepresentative {
    private ColorChannel[] colorChannels;
    private final boolean deltaEncoded;
    private final boolean grayScale;
    private int[] initialPixels;
    private boolean uniformMaxAlpha;

    public ImageRepresentative(boolean grayScale, boolean uniformMaxAlpha, boolean deltaEncoded, int flatsize) {
        this.grayScale=grayScale;
        this.deltaEncoded=deltaEncoded;
        this.uniformMaxAlpha = uniformMaxAlpha;
        InitializeColorChannels(flatsize);

        if (deltaEncoded) initialPixels = new int[grayScale ? 2 : 4];
    }

    public ColorChannel[] getColorChannels() {
        return colorChannels;
    }

    public boolean isDeltaEncoded() {
        return deltaEncoded;
    }

    public boolean isGrayScale() {
        return grayScale;
    }

    public boolean isUniformMaxAlpha() {
        return uniformMaxAlpha;
    }

    private void InitializeColorChannels(int flatsize) {
        if (grayScale) {
            colorChannels = new ColorChannel[uniformMaxAlpha ? 1 : 2];
            colorChannels[0] = new ColorChannel(ChannelColorTypes.Gray, flatsize);
            if (!uniformMaxAlpha) colorChannels[1] = new ColorChannel(ChannelColorTypes.Alpha, flatsize);
        } else {
            colorChannels = new ColorChannel[uniformMaxAlpha ? 3 : 4];
            colorChannels[0] = new ColorChannel(ChannelColorTypes.Red, flatsize);
            colorChannels[1] = new ColorChannel(ChannelColorTypes.Green, flatsize);
            colorChannels[2] = new ColorChannel(ChannelColorTypes.Blue, flatsize);
            if (!uniformMaxAlpha) colorChannels[3] = new ColorChannel(ChannelColorTypes.Alpha, flatsize);
        }
    }

    public int getChannelIndex(ChannelColorTypes channelType) {
        int channelIndex = grayScale ? channelType.ordinal()-3 : channelType.ordinal();

        if (channelType==ChannelColorTypes.Alpha && !grayScale) {
            channelIndex--;
        }

        return channelIndex;
    }

    public void setPixelColorChannel(ChannelColorTypes channelType, int flatindex, int value) {
        getOneColorChannel(channelType).setChannelPixel(flatindex,value);
    }

    public void setIntialPixel(ChannelColorTypes channelColor, int initialPixelColor) {
        if (!this.deltaEncoded) return;

        int channelIndex = getChannelIndex(channelColor);

        this.initialPixels[channelIndex] = initialPixelColor;
    }

    public int[] getInitialPixels() {
        return this.initialPixels;
    }

    public ColorChannel getOneColorChannel(ChannelColorTypes channelType) {
        return this.getColorChannels()[this.getChannelIndex(channelType)];
    }

    public static int staticGetChannelIndex(ChannelColorTypes channelType, boolean grayScale, boolean uniformMaxAlpha) {
        int channelIndex = grayScale ? channelType.ordinal()-3 : channelType.ordinal();

        if (channelType==ChannelColorTypes.Alpha && !grayScale) channelIndex--;
        if (uniformMaxAlpha && channelType==ChannelColorTypes.Alpha) return -1;

        return channelIndex;
    }
}
