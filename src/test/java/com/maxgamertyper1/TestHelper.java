package com.maxgamertyper1;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.ColorChannel;

import java.util.Arrays;

public class TestHelper {
    public static ColorChannel getUniformGrayColorChannel(int value,int width, int height) {
        ColorChannel Gray = new ColorChannel(ChannelColorTypes.Gray,width*height);

        int[] data = new int[width*height];
        Arrays.fill(data,value);

        Gray.setChannelData(data); // 1 uniform gray so the paeth should be 0 everywhere
        return Gray;
    }
}
