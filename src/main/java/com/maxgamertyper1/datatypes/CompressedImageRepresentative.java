package com.maxgamertyper1.datatypes;

public class CompressedImageRepresentative {
    private final CompressedColorChannel[] CompressedChannels;
    private final boolean grayScale;
    private final boolean uniformMaxAlpha;

    public CompressedImageRepresentative(boolean grayScale, boolean uniformMaxAlpha, CompressedColorChannel[] CompressedChannels) {
        this.grayScale = grayScale;
        this.uniformMaxAlpha = uniformMaxAlpha;
        this.CompressedChannels = CompressedChannels;
    }
}
