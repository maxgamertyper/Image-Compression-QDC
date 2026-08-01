package com.maxgamertyper1.CodecIO;

import com.maxgamertyper1.datatypes.IntArrayList;

import java.util.Arrays;

public class UnaryCalculator {
    int[] bitCounts;
    int[][] unaryCombinations; // can do all 512 since its just basic multiplication

    public UnaryCalculator(int[] bitcounts) {
        bitCounts = bitcounts;
        createCombinations();
    }

    private void createCombinations() {
        unaryCombinations = new int[512][];
        IntArrayList thisCombination = new IntArrayList(10);

        for (int i=0; i < 512; i++) {
            thisCombination.Clear();

            for (int bitNumber = 0; bitNumber < 9; bitNumber++) {
                if ((i & (1 << bitNumber)) != 0) { // last bit set 0b00000001
                    thisCombination.Add(bitNumber + 1);
                }
            }
            thisCombination.Add(10); // ESCAPE code always needs to be present
            unaryCombinations[i] = thisCombination.toIntArray();
        }
    }

    public int[] calculateOptimalCombination() {
        int minScore = Integer.MAX_VALUE;
        int[] bestCombination = null;

        for (int[] combination : unaryCombinations) {
            int score = getCombinationScore(combination);
            if (score < minScore) {
                minScore = score;
                bestCombination = combination;
            }
        }
        return bestCombination;
    }

    private int getCombinationScore(int[] combination) {
        int totalCost = 0;
        for (int bitLength = 0; bitLength < bitCounts.length; bitLength++) {
            int prefixIndex = Arrays.binarySearch(combination,bitLength + 1);

            if (prefixIndex < 0) {
                prefixIndex = -prefixIndex - 1; // undo the negative insertion
            }
            int dataSize = combination[prefixIndex]; // the size of the data (according to the prefix)

            prefixIndex++; // make it equal to the length of the unary
            int bitCount = bitCounts[bitLength]; // offset by 1 so 0 is 1 bit, 1 is 2, 9 is OVERFLOW

            int costPerItem = dataSize + prefixIndex + (bitLength == 9 ? 8 : 0); // 8 bits needed to say the ESCAPE size
            totalCost += bitCount * costPerItem;
        }
        return totalCost;
    }
}
