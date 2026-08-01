package com.maxgamertyper1.datatypes.records;

import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.PixelHelper;

import java.util.Arrays;

public record CornerDataRepresentative(
        int[] convexCornerIndexDeltas,
        int startingIndex,
        int[] cornerFlags,
        int dataValue,
        int[] convexCornerDeltaDxDy,
        int zigzagValue
) {
    public CornerDataRepresentative(
            int[] convexCornerIndexDeltas,
            int startingIndex,
            int[] cornerFlags,
            int dataValue
    ){
        this (
                convexCornerIndexDeltas,
                startingIndex,
                cornerFlags,
                dataValue,
                calculateCornerDeltaDxDy(convexCornerIndexDeltas, startingIndex),
                zigzagEncode(dataValue)
        );
    }
    @Override
    public String toString() {
        return "Index Deltas: "+ Arrays.toString(convexCornerIndexDeltas) +"; StartingIndex: "+startingIndex+" Value: "+dataValue;
    }

    private static int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-2, ...
        return (number << 1) ^ (number >> 31);
    }

    private static int[] calculateCornerDeltaDxDy(int[] convexCornerIndexDeltas, int startingIndex) {
        int[] returnHolder = new int[2 * convexCornerIndexDeltas.length];

        int currentAbsoluteIndex = startingIndex;
        int[] prevData = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,currentAbsoluteIndex);

        for (int i=0; i < convexCornerIndexDeltas.length; i++) {
            currentAbsoluteIndex += convexCornerIndexDeltas[i];
            int[] currentData = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,currentAbsoluteIndex);

            returnHolder[2 * i] = zigzagEncode(currentData[0] - prevData[0]); // dx
            returnHolder[2 * i + 1] = zigzagEncode(currentData[1] - prevData[1]); // dy

            prevData = currentData;
        }

        return returnHolder;
    }
}
