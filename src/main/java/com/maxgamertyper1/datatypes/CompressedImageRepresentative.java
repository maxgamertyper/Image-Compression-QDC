package com.maxgamertyper1.datatypes;

public class CompressedImageRepresentative {
    private final CompressedColorChannel[] CompressedChannels;
    private final boolean grayScale;
    private final boolean uniformMaxAlpha;
    private final boolean paethEncoded;

    public CompressedImageRepresentative(boolean grayScale, boolean uniformMaxAlpha, CompressedColorChannel[] CompressedChannels, boolean paethEncoded) {
        this.grayScale = grayScale;
        this.uniformMaxAlpha = uniformMaxAlpha;
        this.CompressedChannels = CompressedChannels;
        this.paethEncoded = paethEncoded;
    }

    public CompressedColorChannel[] getCompressedChannels() {
        return CompressedChannels;
    }

    public boolean isGrayScale() {
        return grayScale;
    }

    public boolean isUniformMaxAlpha() {
        return uniformMaxAlpha;
    }

    public boolean isPaethEncoded() {
        return paethEncoded;
    }
}
