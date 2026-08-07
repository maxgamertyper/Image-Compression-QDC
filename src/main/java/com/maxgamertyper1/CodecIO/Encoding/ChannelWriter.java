package com.maxgamertyper1.CodecIO.Encoding;

import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class ChannelWriter implements Closeable {
    RawBitWriter bitWriter;
    DataType currentChunkType;
    int[] unaryPrefixValueSet; // lsat one by default is escape sequence
    ByteArrayOutputStream stream;
    private boolean previousWasCorner;

    private static final int[] unaryValueRaw = new int[]{0,2,6,14,30,62,126,254}; //0,10,110,1110,etc


    public ChannelWriter() {
        stream = new ByteArrayOutputStream();
    }

    public void setData(DataType startingType, int[] unaryPrefixValueSet) throws IOException {
        stream.reset();
        createChannelInfo(startingType, unaryPrefixValueSet);
    }

    public void writeRawOPCode(DataType type) throws IOException {
        bitWriter.writeRawValue(type.getOpCode(),2);
    }

    private void createChannelInfo(DataType startingType, int[] unaryPrefixValueSet) throws IOException {
        bitWriter = new RawBitWriter(stream);
        currentChunkType = startingType;
        this.unaryPrefixValueSet = unaryPrefixValueSet;

        writeMetadata(startingType);
    }

    private void writeMetadata(DataType startingType) throws IOException {
        bitWriter.writeRawValue(unaryPrefixValueSet.length,4);
        for (int unaryPrefixValue : unaryPrefixValueSet) {
            bitWriter.writeRawValue(unaryPrefixValue,4);
        }
        writeRawOPCode(startingType);
    }

    private void trySwitchChunkType(DataType chunk) throws IOException {
        if (currentChunkType != chunk) {
            writeEscapeData();

            int switchBit = currentChunkType.getSwitchBit(chunk);
            bitWriter.writeRawValue(switchBit,1);
            currentChunkType = chunk;
        }
    }

    public void writeEscapeData() throws IOException {
        int lastUnaryPrefix = unaryPrefixValueSet.length - 1;
        bitWriter.writeRawValue(unaryValueRaw[lastUnaryPrefix],lastUnaryPrefix + 1); // escape code
        bitWriter.writeRawValue(1,1);
    }

    public void writeCornerData(CornerDataRepresentative data, boolean layerOverride) throws IOException {
        //CornerData: [Value, (corner 1) 10 (10 x delta), 5 (5 y delta), (corner 2) -2, 5]
        trySwitchChunkType(DataType.Corner);
        writeValueWithPrefix(data.zigzagValue());

        for (int dimensionDelta : data.convexCornerDeltaDxDy()) {
            writeValueWithPrefix(dimensionDelta);
        }

        writeEscapeData();
        bitWriter.writeRawValue(layerOverride ? 1 : 0,1);

        previousWasCorner = true;
    }

    public void writeDeltas(int[] deltas) throws IOException {
        writeValueWithPrefix(deltas[0]);
        writeValueWithPrefix(deltas[1]);
    }

    public void writeOverriden(int[] deltas, RectangleRepresentative data) throws IOException {
        writeDeltas(deltas);
        writeRectangleData(data);
    }

    public void writeOverriden(int[] deltas, int zigzagValue) throws IOException {
        System.out.println("Corner Override (RAW): Deltas: " + Arrays.toString(deltas) + "; Value: " + zigzagValue);
        writeDeltas(deltas);
        writeValueWithPrefix(zigzagValue);
    }

    public void writeRectangleData(RectangleRepresentative data) throws IOException {
        //RectangleData: [Value, Width, Height]
        trySwitchChunkType(DataType.Rectangle);
        writeValueWithPrefix(data.zigzagValue());
        writeValueWithPrefix(data.width());
        writeValueWithPrefix(data.height());
    }

    public void writeRawData(int zigzagValue) throws IOException {
        //RAW: color
        trySwitchChunkType(DataType.Raw);
        writeValueWithPrefix(zigzagValue);
    }

    private void writeValueWithPrefix(int value) throws IOException {
        int rawStorageSize = dataBitLength(value);
        int prefixIndex = getPrefixIndex(rawStorageSize);

        //write prefix
        bitWriter.writeRawValue(unaryValueRaw[prefixIndex], prefixIndex + 1);
        if (rawStorageSize > 9) {
            System.out.println(rawStorageSize);
            bitWriter.writeRawValue(0, 1); // the overflow code
            bitWriter.writeRawValue(rawStorageSize, 7); // the byte for size
            bitWriter.writeRawValue(value, rawStorageSize);
        } else {
            bitWriter.writeRawValue(value, unaryPrefixValueSet[prefixIndex]);
        }
        System.out.println("Prefix: " + unaryValueRaw[prefixIndex] + " Value: " + value);
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

    public int lastEmptyBits() {return bitWriter.getEmptyBitsFromFlushRemaining();}

    @Override
    public void close() throws IOException {
        bitWriter.close(); // write the ending bytes, flush, save
    }
}