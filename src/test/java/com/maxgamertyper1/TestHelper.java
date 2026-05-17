package com.maxgamertyper1;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.ColorChannel;
import com.maxgamertyper1.datatypes.PixelChunk;
import com.maxgamertyper1.datatypes.records.CornerPoint;
import com.maxgamertyper1.imagereading.ImageDataHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class TestHelper {
    public static ColorChannel getUniformColorChannel(ChannelColorTypes color, int value, int width, int height) {
        ColorChannel Gray = new ColorChannel(color,width*height);

        int[] data = new int[width*height];
        Arrays.fill(data,value);

        Gray.setChannelData(data); // 1 uniform gray so the paeth should be 0 everywhere
        return Gray;
    }

    public static ColorChannel getBlockedTestingChannel(ChannelColorTypes colorTypes, int val, int UVA, int BVA) {
        ColorChannel TestingColorChannel = new ColorChannel(colorTypes,100);
        // there should be a 4x4 box inside of a U-Shape
        TestingColorChannel.setChannelData(new int[]{
                val, val, val, val, val, val, val, val, val, val,
                val, UVA, BVA, BVA, BVA, BVA, UVA, val, val, val,
                val, UVA, BVA, BVA, BVA, BVA, UVA, val, val, val,
                val, UVA, BVA, BVA, BVA, BVA, UVA, val, val, val,
                val, UVA, BVA, BVA, BVA, BVA, UVA, val, val, val,
                val, UVA, val, val, val, val, UVA, val, val, val,
                val, UVA, UVA, val, val, UVA, UVA, val, val, val,
                val, val, UVA, UVA, UVA, UVA, val, val, val, val,
                val, val, val, val, val, val, val, val, val, val,
                val, val, val, val, val, val, val, val, val, val
        });
        return TestingColorChannel;
    }
    // "static" probably isnt the best descriptor but idk what else to call it
    public static ColorChannel getStaticTestingChannel(ChannelColorTypes colorType, int val, int SVA, int BVA) {
        ColorChannel TestingColorChannel = new ColorChannel(colorType,100);
        // Used for testing Isolated Indexes and more Boxes
        TestingColorChannel.setChannelData(new int[]{
                BVA, BVA, BVA, BVA, BVA, BVA, val, val, val, val,
                BVA, BVA, BVA, BVA, BVA, BVA, val, val, val, val,
                val, val, BVA, BVA, val, val, val, val, val, val,
                val, val, val, val, val, val, val, val, val, val,
                BVA, BVA, val, val, val, val, SVA, val, val, val,
                BVA, BVA, val, val, val, val, val, SVA, val, val,
                BVA, BVA, BVA, val, val, val, val, val, val, val,
                BVA, BVA, BVA, val, val, val, SVA, val, val, val,
                BVA, BVA, val, val, val, val, val, SVA, val, val,
                BVA, BVA, val, val, val, val, val, val, val, val
        });
        return TestingColorChannel;
    }




    public static PixelChunk buildPixelChunk(ChannelColorTypes colorType, int val, int startingIndex, int convexCorners, int[][] CornerPairs,int totalArea) {
        PixelChunk PC = new PixelChunk(colorType,val);
        PC.StartingIndex = startingIndex;
        PC.convexCorners = convexCorners;
        PC.setTotalArea(totalArea);

        ArrayList<CornerPoint> Corners = new ArrayList<CornerPoint>();
        for (int[] cornerPair : CornerPairs) {
            Corners.add(new CornerPoint(cornerPair[0],cornerPair[1]));
        }

        PC.cornerPoints = Corners;
        return PC;
    }


    public static void DataholderSetter(int width, int height) {
        ImageDataHolder.height = height;
        ImageDataHolder.width = width;
        ImageDataHolder.flatsize = width*height;
    }

    public static final BitSet emptyBitSet =  new BitSet(100);
}