package com.maxgamertyper1.CodecIO.Decoding;

public record Dimensions(int width, int height, int flatsize) {
    public Dimensions(int width, int height) {
        this(
                width,
                height,
                width*height
        );
    }
}
