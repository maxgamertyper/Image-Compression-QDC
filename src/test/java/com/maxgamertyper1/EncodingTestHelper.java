package com.maxgamertyper1;

public class EncodingTestHelper {
    public static final int[] sequentialData = new int[]{
            0,1,2,3,4,
            5,6,7,8,9,
            10,11,12,13,14,
            15,16,17,18,19,
            20,21,22,23,24
    };
    public static final int[] straightIsolatedFlags = new int[] {
            15,15,15,15,15,
            15,15,15,15,15,
            15,15,15,15,15,
            15,15,15,15,15,
            15,15,15,15,15
    };

    public static final byte[] expectedRAWTestBytes = new byte[]{
            // <MAX,                TESTING VERSION, 5x5,             gray, max alpha, other original type
            0x3C, 0x4D, 0x41, 0x58, 0x00, 0x00, 0x00, 0x05, 0x00, 0x05, (byte) 0xC0,
            //Icolor
            0x00,
            //meta hash (calculated in test due to version
            0x00, 0x00, 0x00, 0x00,

            //channel meta: size = 24 bytes (including unary), 0 trailing
            0x00, 0x00, 0x00, 0x18, 0x06,

            //unary unary table size 4, [5,6,10]
            0x35, 0x6A,

            //0-24
            0x00, 0x08, 0x41, (byte) 0x88, 0x28, (byte) 0xC3, (byte) 0x90, 0x49, 0x45, (byte) 0x98, 0x69, (byte) 0xC7,
            (byte) 0xA8, 0x28, (byte) 0xA9, 0x29, (byte) 0xAA, 0x2A, (byte) 0xAB, 0x2B, (byte) 0xAC, 0x00,

            //channel hash
            0x6F,0x28,0x09,0x58,

            //file tail
            //red channel
            0x00,0x00,0x00,0x00,
            //green channel
            0x00,0x00,0x00,0x00,
            //blue
            0x00,0x00,0x00,0x00,
            //gray
            0x00,0x00,0x00,0x10,
            //alpha
            0x00,0x00,0x00,0x00,

            //full file adler (updated in funciton)
            0x00, 0x00, 0x00, 0x00,
            //full file (MAX!)
            0x4D,0x41,0x58,0x21
    };
}
