package com.maxgamertyper1.CodecIO.Decoding;

public record StreamLocation(
        long byteLocation,
        int bitLocation,
        long totalBits
) {
    StreamLocation(long byteLocation, int bitLocation) {
        this(
                byteLocation,
                bitLocation,
                byteLocation * 8 + bitLocation
        );
    }

    StreamLocation(long totalBits) {
        this(
                totalBits / 8,
                (int) (totalBits & 7),
                totalBits
        );
    }
}
