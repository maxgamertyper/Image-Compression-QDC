package com.maxgamertyper1.datatypes;

import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.OriginalFileType;

import java.util.Arrays;

public class CompressedImageRepresentative {
    private final CompressedColorChannel[] CompressedChannels;
    private ImageMeta metaData;
    private boolean grayScale;
    private boolean uniformMaxAlpha;
    private boolean paethEncoded;
    private short imageHeight;
    private short imageWidth;
    private OriginalFileType originalFileType;

    public CompressedImageRepresentative(boolean grayScale, boolean uniformMaxAlpha, CompressedColorChannel[] CompressedChannels, boolean paethEncoded) {
        this.grayScale = grayScale;
        this.uniformMaxAlpha = uniformMaxAlpha;
        this.CompressedChannels = CompressedChannels;
        this.paethEncoded = paethEncoded;
    }

    public CompressedImageRepresentative(ImageMeta metaData, CompressedColorChannel[] CompressedChannels) {
        this.CompressedChannels = CompressedChannels;
        this.metaData = metaData;
    }

    public void setMetaData(short imageHeight, short imageWidth, OriginalFileType fileType) {
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        originalFileType = fileType;
    }

    public short getImageHeight() {return  imageHeight;}
    public short getImageWidth() {return imageWidth;}
    public OriginalFileType getOriginalFileType() {return originalFileType;}

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

    public void sortChannelOrder() {
        Arrays.sort(
                CompressedChannels,
                (a,b) -> Integer.compare(
                        a.getChannelColorType().getOrderValue(),
                        b.getChannelColorType().getOrderValue()
                )
        );
    }
}
