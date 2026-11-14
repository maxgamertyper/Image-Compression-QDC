package com.maxgamertyper1.CodecIO.Decoding;

public record Version(int major, int minor) {
    public boolean matches(Version comparator) {
        return (major == comparator.major) && (minor == comparator.minor);
    }
}
