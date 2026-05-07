package com.maxgamertyper1;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.PixelChunk;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;

import static com.maxgamertyper1.TestHelper.buildPixelChunk;

public class ExpectedDataHolder {
    public static int[] StaticBasedCorrespondingIsolatedIndexes(int SVA) {
        return new int[]{SVA,SVA,SVA,SVA};
    }

    public static final int[] StaticBasedIsolatedIndexes = new int[]{46,57,76,87};

    public static final EdgeMapHolder BlockBasedEdgeMaps = new EdgeMapHolder(
            new int[][]{{-1,9}, {-1,0,6,9}, {-1,0,4,5,7,9}, {-1,0,4,6,7,9}, {-1,0,4,6,7,9}, {-1,0,4,5,7,9}, {-1,0,6,9}, {-1,9}, {-1,9}, {-1,9}},
            new int[][]{{-1,9}, {-1,0,1,5,6,9}, {-1,0,1,5,6,9}, {-1,0,1,5,6,9}, {-1,0,1,5,6,9}, {-1,0,1,5,6,9}, {-1,0,2,4,6,9}, {-1,1,5,9}, {-1,9}, {-1,9}}
    );

    public static final EdgeMapHolder UniformBasedEdgeMaps = new EdgeMapHolder(
            new int[][]{{-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}},
            new int[][]{{-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}, {-1,9}}
    );

    public static final EdgeMapHolder StaticBasedEdgeMaps = new EdgeMapHolder(
            new int[][]{{-1,1,3,9}, {-1,1,3,9}, {-1,2,5,7,9}, {-1,2,9}, {-1,1,9}, {-1,1,9}, {-1,3,4,6,7,9}, {-1,4,5,7,8,9}, {-1,9}, {-1,9}},
            new int[][]{{-1,5,9}, {-1,5,9}, {-1,1,3,9}, {-1,9}, {-1,1,5,6,9}, {-1,1,6,7,9}, {-1,2,9}, {-1,2,5,6,9}, {-1,1,6,7,9}, {-1,1,9}}
    );

    public static final int[] BlockBasedShapeIDS = new int[]{
                0,0,0,0,0,0,0,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,52,52,52,52,11,0,0,0,
                0,11,11,52,52,11,11,0,0,0,
                0,0,11,11,11,11,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0
    };

    public static final int[] StaticBasedShapeIDs = new int[]{
            0, 0, 0, 0, 0, 0, 6, 6, 6, 6,
            0, 0, 0, 0, 0, 0, 6, 6, 6, 6,
            6, 6, 0, 0, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            40, 40, 6, 6, 6, 6, 46, 6, 6, 6,
            40, 40, 6, 6, 6, 6, 6, 57, 6, 6,
            40, 40, 40, 6, 6, 6, 6, 6, 6, 6,
            40, 40, 40, 6, 6, 6, 76, 6, 6, 6,
            40, 40, 6, 6, 6, 6, 6, 87, 6, 6,
            40, 40, 6, 6, 6, 6, 6, 6, 6, 6
    };

    public static final int[] ExpectedBlockFlags = new int[]{
                9,12,12,12,12,12,12,8,8,10,
                3,11,9,8,8,10,11,1,0,2,
                3,3,1,0,0,2,3,1,0,2,
                3,3,1,0,0,2,3,1,0,2,
                3,3,5,4,4,6,3,1,0,2,
                3,3,13,8,8,14,3,1,0,2,
                3,5,10,5,6,9,6,1,0,2,
                1,10,5,12,12,6,9,0,0,2,
                1,0,8,8,8,8,0,0,0,2,
                5,4,4,4,4,4,4,4,4,6
    };

    public static final int[] ExpectedUniformFlags = new int[]{
            9, 8, 8, 8, 8, 8, 8, 8, 8, 10,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            5, 4, 4, 4, 4, 4, 4, 4, 4, 6
    };

    public static final int[] ExpectedStaticFlags = new int[]{
            9, 8, 8, 8, 8, 10, 9, 8, 8, 10,
            5, 4, 0, 0, 4, 6, 1, 0, 0, 2,
            9, 10, 5, 6, 9, 8, 0, 0, 0, 2,
            5, 4, 8, 8, 0, 0, 4, 0, 0, 2,
            9, 10, 1, 0, 0, 2, 15, 5, 0, 2,
            1, 2, 5, 0, 0, 0, 10, 15, 1, 2,
            1, 0, 10, 1, 0, 0, 4, 8, 0, 2,
            1, 0, 6, 1, 0, 2, 15, 5, 0, 2,
            1, 2, 9, 0, 0, 0, 10, 15, 1, 2,
            5, 6, 5, 4, 4, 4, 4, 12, 4, 6
    };

    public static PixelChunk[] getExpectedStaticChunkData(ChannelColorTypes colorType, int val, int BVA) {
        return new PixelChunk[]{
                buildPixelChunk(colorType,BVA,0,6, new int[][]{{0,9}, {5,10}, {10,5}, {15,6}, {22,5}, {23,6}}), // box 1
                buildPixelChunk(colorType,val,6,14, new int[][]{
                        {6,9}, {9,10}, {20,9}, {21,10}, {24,9}, {30,5}, {47,5}, {52,5}, {56,10}, {77,5}, {82,9}, {86,10}, {92,5}, {99,6}
                }), // background
                buildPixelChunk(colorType,BVA,40,6, new int[][]{{40,9}, {41,10}, {62,10}, {72,6}, {90,5}, {91,6}}), // box 2
        }; // isolated indexes are not included in pixel chunks
    }
    public static PixelChunk[] getExpectedBlockChunkData(ChannelColorTypes colorType, int val, int UVA, int BVA) {
        return new PixelChunk[]{
                buildPixelChunk(colorType,val,0,6, new int[][]{{0,9}, {9,10}, {71,10}, {76,9}, {90,5}, {99,6}}),
                buildPixelChunk(colorType,UVA,11,10, new int[][]{{11,11}, {16,11}, {61,5}, {62,10}, {65,9}, {66,6}, {72,5}, {75,6}}),
                buildPixelChunk(colorType,BVA,12,4, new int[][]{{12,9}, {15,10}, {42,5}, {45,6}}),
                buildPixelChunk(colorType,val,52,6, new int[][]{{52,13}, {55,14}, {63,5}, {64,6}})
        };
    }

    public static PixelChunk[] getExpectedUniformChunkData(ChannelColorTypes colorType, int val, int width, int height) {
        return new PixelChunk[]{
                buildPixelChunk(colorType,val,0,6, new int[][]{{0,9}, {width-1,10}, {height*(width-1),5}, {height*width-1,6}}),
        };
    }

    public static int[] getExpectedUniformShapeIDs(int width, int height) {
        int flatsize = width*height;
        return new int[flatsize];
    }

    // Compression Data

    public static final PixelChunk fullSquare = TestHelper.buildPixelChunk(ChannelColorTypes.Alpha,10,0,4,
            new int[][]{{0,9},{9,10},{90,5},{99,6}}
    ); // a 10x10 square (this represents all 0 hole rectangles)

    public static final PixelChunk holeSquare = TestHelper.buildPixelChunk(ChannelColorTypes.Alpha,10,0,4,
            new int[][]{{0,9},{9,10},{90,5},{99,6}}
    ); // a 10x10 square with a hole in the middle (change is in the shapeOrganizer)

    public static final PixelChunk swissCheese = TestHelper.buildPixelChunk(ChannelColorTypes.Alpha,10,0,7,
            new int[][]{{0,9},{4,10},{7,7},{16,10},{20,6},{24,6}}
    );

    public static final int[] holeSquareParentIDs = new int[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 22, 22, 0, 0, 0, 0, 0, 0,
            0, 0, 22, 22, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    public static final int[] swissCheeseParentIDs = new int[] {
            0, 0, 0, 0, 0,
            0, 6, 6, 0, 0,
            0, 0, 0, 13, 0,
            0, 16, 0, 0, 0,
            0, 0, 0, 0, 0
    };

    public static final int[] expectedHoleSquareFlags = new int[] {
            9, 8, 8, 8, 8, 8, 8, 8, 8, 10,
            1, 0, 4, 4, 0, 0, 0, 0, 0, 2,
            1, 2, 9, 10, 1, 0, 0, 0, 0, 2,
            1, 2, 5, 6, 1, 0, 0, 0, 0, 2,
            1, 0, 8, 8, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
            5, 4, 4, 4, 4, 4, 4, 4, 4, 6,
    };

    public static final int[] expectedSwissCheeseFlags= new int[] {
            9, 12, 12, 8, 10,
            3, 13, 14, 5, 2,
            1, 12, 10, 15, 3,
            3, 15, 1, 8, 2,
            5, 12, 4, 4, 6
    };
}