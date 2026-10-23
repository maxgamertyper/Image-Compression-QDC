package com.maxgamertyper1.compressor;

// this will be more robust later, i just need to disable corners to i can make this work and then make corners work!

public record CompressionConfig(
        boolean cornersEnabled,
        int minRectangleSize
) {
    public static final CompressionConfig DEFAULT = new CompressionConfig(false, 5);

    public static final CompressionConfig OLD = new CompressionConfig(true, 5);

    public CompressionConfig {  // compact constructor: validate once
        if (minRectangleSize < 1) throw new IllegalArgumentException("minRectangleSize must be >= 1");
    }
}