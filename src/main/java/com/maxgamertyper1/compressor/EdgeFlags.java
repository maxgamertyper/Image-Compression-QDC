package com.maxgamertyper1.compressor;

public class EdgeFlags {
    private EdgeFlags() {}

    public static final int LEFT  = 1; // bit 0 (1)
    public static final int RIGHT = 1 << 1; // bit 1 (2)
    public static final int DOWN  = 1 << 2; // bit 2 (4)
    public static final int UP    = 1 << 3; // bit 3 (8)
    public static final int ALL   = LEFT | RIGHT | DOWN | UP;  // 15, isolated pixel

    // index = edge flags
    // value = convex corners at that pixel
    private static final byte[] CORNERS = {0,0,0,0,0,1,1,2,0,1,1,2,0,2,2,4};

    public static boolean has(int flags, int edge) { return (flags & edge) != 0; }
    public static boolean isIsolated(int flags)    { return flags == ALL; }
    public static int cornerCount(int flags)       { return CORNERS[flags]; }
}
