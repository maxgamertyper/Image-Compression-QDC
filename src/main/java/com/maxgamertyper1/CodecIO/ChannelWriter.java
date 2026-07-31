package com.maxgamertyper1.CodecIO;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class ChannelWriter implements Closeable {
    RawBitWriter bitWriter;
    DataType currentChunkType;
    int[] unaryPrefixValueSet; // lsat one by default is escape sequence
    ByteArrayOutputStream stream;

    private static final int[] unaryValueRaw = new int[]{0,2,6,14,30,62,126,254}; //0,10,110,1110,etc


    public ChannelWriter(DataType startingType, int[] unaryPrefixValueSet) throws IOException {
        stream = new ByteArrayOutputStream();
        bitWriter = new RawBitWriter(stream);

        bitWriter.writeRawValue(startingType.getOpCode(),2);
        currentChunkType = startingType;
        this.unaryPrefixValueSet = unaryPrefixValueSet;
    }

    public void setUnaryList(int[] unaryPrefixValues) {
        unaryPrefixValueSet = unaryPrefixValues;
    }

    private void trySwitchChunkType(DataType chunk) throws IOException {
        if (currentChunkType != chunk) {
            int switchBit = currentChunkType.getSwitchBit(chunk);
            bitWriter.writeRawValue(switchBit,1);
            currentChunkType = chunk;
        }
    }

    public void writeCornerData(int[] convexCornerDimensionDeltas, int rawValue, boolean layerOverride) throws IOException {
        //CornerData: [Value, (corner 1) 10 (10 x delta), 5 (5 y delta), (corner 2) -2, 5]
        trySwitchChunkType(DataType.Corner);
        writeValueWithPrefix(zigzagEncode(rawValue));

        for (int dimensionDelta : convexCornerDimensionDeltas) {
            writeValueWithPrefix(zigzagEncode(dimensionDelta));
        }

        bitWriter.writeRawValue(layerOverride ? 1 : 0,1);
    }

    public void writeRectangleData(int rectangleHeight, int rectangleWidth, int rawValue) throws IOException {
        //RectangleData: [Value, Width, Height]
        trySwitchChunkType(DataType.Rectangle);
        writeValueWithPrefix(zigzagEncode(rawValue));
        writeValueWithPrefix(rectangleWidth);
        writeValueWithPrefix(rectangleHeight);
    }

    public void writeRawData(int rawValue) throws IOException {
        //RAW: color
        trySwitchChunkType(DataType.Raw);
        writeValueWithPrefix(zigzagEncode(rawValue));
    }

    private void writeValueWithPrefix(int value) throws IOException {
        int rawStorageSize = dataBitLength(value);
        int prefixIndex = getPrefixIndex(rawStorageSize);

        //write prefix
        bitWriter.writeRawValue(unaryValueRaw[prefixIndex], prefixIndex + 1);
        //write value
        bitWriter.writeRawValue(value, unaryPrefixValueSet[prefixIndex]);
    }

    private int getPrefixIndex(int bitLength) {
        int index = Arrays.binarySearch(unaryPrefixValueSet, bitLength);

        if (index >= 0) {
            return index;
        }
        int reversedIndex = -index - 1;

        return Math.min(reversedIndex, unaryPrefixValueSet.length - 1);
    }


    public int dataBitLength(int data) {
        int baseReturn = (32 - Integer.numberOfLeadingZeros(data));
        return baseReturn == 0 ? 1 : baseReturn;
    }

    public ByteArrayOutputStream getByteStream() throws IOException {
        bitWriter.close();
        return stream;
    }

    public int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-1
        return (number << 1) ^ (number >> 31);
    }

    @Override
    public void close() throws IOException {
        bitWriter.close(); // write the ending bytes, flush, save
    }
}