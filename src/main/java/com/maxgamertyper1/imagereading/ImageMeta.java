package com.maxgamertyper1.imagereading;

public record ImageMeta(
        int height,
        int width,
        long flatsize,

        boolean grayscale, // no RGB channels
        boolean opaque, // no alpha channel

        OriginalFileType fileType,
        String metaImageName
) {

    public ImageMeta(
            int height,
            int width,
            boolean grayscale,
            boolean opaque, // no alpha channel
            OriginalFileType fileType,
            String metaImageName
    ) {
        this(
                height,
                width,
                (long) height * width,
                grayscale,
                opaque,
                fileType,
                metaImageName
        );
    }
}
