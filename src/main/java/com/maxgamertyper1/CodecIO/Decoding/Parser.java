package com.maxgamertyper1.CodecIO.Decoding;

import com.maxgamertyper1.CodecIO.Encoding.DataType;
import com.maxgamertyper1.datatypes.IntArrayList;

import java.io.IOException;

public class Parser {
    private final FormattedReader reader;
    private final IntArrayList values = new IntArrayList(4);
    private long storedByteLocation;
    private int storedBitLocation;
    private int[] unaryTable;
    private int overflowIndex;
    private int valueLength;
    private StreamLocation loggedPosition;
    private DataType currentType;
    private int totalChannels;
    private StreamLocation channelEndLocaiton;
    //data Tracking
    private boolean inOverride;
    private int dataLength;
    private boolean justExitedOverride = false;

    public Parser(String inFileName) throws IOException {
        reader = new FormattedReader(inFileName);
    }

    private int readBit() throws IOException {
        return reader.readBits(1);
    }

    private int readInt8() throws IOException {
        return reader.readBytes(1);
    }

    private int readInt16() throws IOException {
        return reader.readBytes(2);
    }

    private int readInt32() throws IOException {
        return reader.readBytes(4);
    }

    public void startChannelTracker(int byteLength, int trailingBits) {
        StreamLocation cursor = cursorLocation();

        int channelLength = byteLength * 8 - trailingBits;
        channelEndLocaiton = new StreamLocation(cursor.totalBits() + channelLength);
    }

    public boolean hasMoreBits() {
        return cursorLocation().totalBits() < channelEndLocaiton.totalBits();
    }

    public StreamLocation getLoggedPosition() {
        return loggedPosition;
    }

    public StreamLocation getTrackedPosition() {
        return new StreamLocation(reader.getByteTracker(), reader.getBitsTracker());
    }

    public StreamLocation cursorLocation() {
        return new StreamLocation(reader.getTotalBytesRead(), reader.getTotalBitsRead());
    }

    public StreamLocation getLoggedDistance() {
        long totalDistance = cursorLocation().totalBits() - loggedPosition.totalBits();
        return new StreamLocation(totalDistance / 8, (int) (totalDistance & 7));
    }

    public boolean checkFile() throws IOException {
        return reader.correctType();
    }

    public long getFileSize() throws IOException {
        return reader.getFileSize();
    }

    public Dimensions readCanvasDimensions() throws IOException {
        return new Dimensions(readInt16(), readInt16());
    }

    public Version readCompressionVersion() throws IOException {
        return new Version(readInt8(), readInt8());
    }

    public HeaderFlags readFlag() throws IOException {
        HeaderFlags returnHolder = new HeaderFlags(
                readBit() == 1,
                readBit() == 1,
                reader.readBits(2)
        );
        reader.readBits(4); // eat the rest
        calculateChannelCount(returnHolder.isGrayscale(), returnHolder.isMaxAlpha());
        return returnHolder;
    }

    private void calculateChannelCount(boolean grayscale, boolean maxAlpha) {
        totalChannels = grayscale ? 2 : 4;
        totalChannels -= maxAlpha ? 1 : 0;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public int[] readInitialColors() throws IOException {
        int[] colors = new int[totalChannels];
        for (int i = 0; i < totalChannels; i++) {
            colors[i] = readInt8();
        }
        return colors;
    }

    public int readHash() throws IOException {
        reader.jumpToByteBoundary();
        return readInt32();
    }

    public ChannelSizeInfo readChannelSizeData() throws IOException {
        int byteLength = readInt32();
        int trailingBits = readInt8();
        startChannelTracker(byteLength, trailingBits);
        return new ChannelSizeInfo(byteLength, trailingBits);
    }

    public DataType readExtendedOPCode() throws IOException {
        int a = reader.readBits(2);
        currentType = DataType.fromOPCode(a);
        setDataLength();
        return currentType;
    }

    public boolean justExitedOverride() {
        return justExitedOverride;
    }


    public int processEscape() throws IOException {
        int branch = 0;

        if (inOverride) {
            currentType = DataType.Corner;
            inOverride = false;
            setDataLength();
            justExitedOverride = true;
        } else if (justExitedOverride && currentType == DataType.Corner) {
            justExitedOverride = false;
            readOPCode();
            branch = 1;
        } else {
            justExitedOverride = false;
            readOPCode();
            branch = 2;
        }
        return branch;
    }

    public void readOPCode() throws IOException {
        currentType = currentType.getNextType(readBit());
        setDataLength();
    }

    private void setDataLength() {
        switch (currentType) {
            case Raw -> {
                dataLength = 1;
            }
            case Rectangle -> {
                dataLength = 3;
            }
            case Corner -> {
                dataLength = 10;
            }
            default -> dataLength = -1;
        }
        if (inOverride) {
            dataLength += 2; // for the corner deltas
        }
    }

    public int readSeekEntry() throws IOException {
        return readInt32();
    }

    public void loadUnaryTable() throws IOException {
        unaryTable = new int[reader.readBits(4)];
        overflowIndex = unaryTable.length - 1;

        for (int i = 0; i < unaryTable.length; i++) {
            unaryTable[i] = reader.readBits(4);
        }
    }

    public int[] getUnaryTable() {
        return unaryTable;
    }

    public void updateOverrideData(boolean overrideData) throws IOException {
        inOverride = overrideData;
        if (overrideData) {
            readExtendedOPCode();
        }
    }

    public int[] processShape() throws IOException {
        values.clear();
        int iDataLength = dataLength;
        // already prefix processed because of if statement
        values.add(processDataEntry(iDataLength));


        if (currentType == DataType.Corner) { // is a corner
            while (!prepareNextLength()) {
                values.add(processDataEntry(iDataLength));
            }
            int oValue = reader.readBits(1);
            values.add(oValue); // override Value
        } else { // is a non-corner
            iDataLength--; // subtract for the first color value (above if statement)
            while (iDataLength > 0 && !prepareNextLength()) { // prepareNextLength should never be an escape here, but just in case
                values.add(processDataEntry(iDataLength));
                iDataLength--;
            }
        }

        return values.toIntArray();
    }

    public int processDataEntry(int iDataLength) throws IOException {
        System.out.println("Prefix bit read count: " + valueLength);
        if (inOverride && iDataLength > dataLength - 3) { // the override deltas and first color
            return readDataEntry(true);
        } else if (!inOverride && iDataLength > dataLength - 1) { // first color
            return readDataEntry(true);
        } else {
            return readDataEntry(false);
        }
    }

    public boolean prepareNextLength() throws IOException {
        int prefixLength = reader.readUnary();

        if (prefixLength == overflowIndex) {
            boolean isEscape = readBit() == 1;

            if (isEscape) {
                valueLength = -1;
                return true;
            }

            valueLength = reader.readBits(7);
        } else {
            valueLength = unaryTable[prefixLength];
        }

        return false;
    }

    public int readDataEntry(boolean zigzag) throws IOException {
        if (valueLength == -1) {
            throw new IllegalStateException("Cannot read data entry: prepareNextLength() must be called first");
        }

        int tempLength = valueLength;
        valueLength = -1;

        return zigzag ? reader.readZigZag(tempLength) : reader.readBits(tempLength);
    }

    public void logCursorLocaiton() {
        loggedPosition = new StreamLocation(reader.getTotalBytesRead(), reader.getTotalBitsRead());
    }

    public DataType getCurrentType() {
        return currentType;
    }

    public boolean isInOverride() {
        return inOverride;
    }
}