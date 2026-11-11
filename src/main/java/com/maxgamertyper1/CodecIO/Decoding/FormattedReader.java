package com.maxgamertyper1.CodecIO.Decoding;

import java.io.*;
import java.util.Arrays;

public class FormattedReader implements AutoCloseable {
    private final FileInputStream fileInputStream;
    private final BufferedInputStream inputBuffer;
    private long bitBuffer = 0;
    private int bitsStored = 0;
    private long byteTracker = 0;
    private int bitsTracker = 0;
    private long cursorByte = 0;
    private int cursorBit = 0;
    private final String inFileName;

    public FormattedReader(String fileName) throws FileNotFoundException {
        inFileName = fileName;
        fileInputStream = new FileInputStream(fileName);
        inputBuffer = new BufferedInputStream(fileInputStream);
    }

    public long getFileSize() throws IOException {
        return fileInputStream.getChannel().size();
    }

    public long getTotalBytesRead() {
        return cursorByte;
    }

    public int getTotalBitsRead() {
        return cursorBit;
    }

    public int getBitsTracker() {
        return bitsTracker;
    }

    public long getByteTracker() {
        return byteTracker;
    }

    private void updateTracker(int numBits) {
        bitsTracker += numBits;
        cursorBit += numBits;

        int bytesToAddTracker = bitsTracker / 8;
        if (bytesToAddTracker != 0) {
            byteTracker += bytesToAddTracker;
            bitsTracker %= 8;
        }

        int bytesToAddCursor = cursorBit / 8;
        if (bytesToAddCursor != 0) {
            cursorByte += bytesToAddCursor;
            cursorBit %= 8;
        }
    }

    public int readZigZag(int numBits) throws IOException {
        int raw = readBits(numBits);
        return (raw >>> 1) ^ -(raw & 1);
    }

    public int readBytes(int numBytes) throws IOException {
        return readBits(numBytes * 8);
    }

    public int readBits(int numBits) throws IOException {
        if (numBits == 0) return 0;
        boolean canRead = enoughBits(numBits);

        if (!canRead) fillBuffer();
        canRead = enoughBits(numBits);

        if (!canRead) throw new EOFException("EOF with " + bitsStored + " bits left; needing: " + numBits);

        updateTracker(numBits);
        int shift = bitsStored - numBits;
        int result = (int) ((bitBuffer >>> shift) & ((1L << numBits) - 1));

        bitsStored -= numBits;

        if (bitsStored == 0) {
            bitBuffer = 0;
        } else {
            bitBuffer &= (1L << bitsStored) - 1;
        }

        return result;
    }

    private boolean enoughBits(int requiredBits) {
        return bitsStored >= requiredBits;
    }

    private void fillBuffer() throws IOException {
        int bytesCanFill = 8 - ((bitsStored + 7) / 8);

        for (int i = 0; i < bytesCanFill; i++) {
            int nextByte = inputBuffer.read();

            if (nextByte == -1) {
                return;
            }

            bitBuffer = (bitBuffer << 8) | (nextByte & 0xFF);
            bitsStored += 8;
        }
    }

    public void jumpToByteBoundary() throws IOException {
        int remainder = bitsStored & 7;
        if (remainder != 0) {
            readBits(remainder);
        }
    }

    public int readUnary() throws IOException {
        int value = 0;
        while (readBits(1) == 1) {
            value++;
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        inputBuffer.close();
        fileInputStream.close();
    }

    public boolean correctType() throws IOException {
        int signatureMatch = readBytes(4);

        if (signatureMatch != 0x3C4D4158) {
            return false;
        }

        byte[] last4Bytes = FileTrailingReader.readTrailingBytes(inFileName);
        byte[] expectedBytes = new byte[]{0x4D, 0x41, 0x58, 0x21};

        return Arrays.equals(last4Bytes, expectedBytes);
    }
}