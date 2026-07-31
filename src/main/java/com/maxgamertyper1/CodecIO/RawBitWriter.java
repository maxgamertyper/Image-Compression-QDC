package com.maxgamertyper1.CodecIO;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Adler32;

public class RawBitWriter {
    private final OutputStream buffer;

    private int bitHead = 0;
    private int intBuffer = 0;
    Adler32 intermediaryAdler;
    Adler32 masterAdler;

    public RawBitWriter(OutputStream stream) {
        this.buffer = stream;
        intermediaryAdler = new Adler32();
        masterAdler = new Adler32();
    }
    // TODO: make a flush reminaing type fuction that adds the rest of the intbuffer to the actual buffer

    public void writeRawValue(int value, int numBits) throws IOException{
        if (bitHead+numBits>32) {
            int newDataLength = 32-bitHead;
            writeRawValue(value >>> (numBits - newDataLength), newDataLength);
            writeRawValue(value & ((1 << (numBits - newDataLength)) - 1), numBits-newDataLength); // uses an and for the rest of the btis to 0 out the top bits
        } else {
            int mask = (numBits == 32) ? -1 : (1 << numBits) - 1; // java is stinky and only uses 5 bits as a shift on a 32-bit number so otherwise do -1 which is 0xFFFFFFFF
            int maskedValue = value & mask; // mask the value to the number of bits specified

            intBuffer <<= numBits; // shift left X values
            intBuffer |= maskedValue; // or the value into the slots
            bitHead += numBits;

            if (bitHead==32) {
                flushBuffer();
            }
        }
    }

    public void flushIntermediaryAdler() throws IOException {
        int hash = (int) intermediaryAdler.getValue();
        writeRawValue(hash,32);
        intermediaryAdler.reset();
    }

    public void flushRemaining() throws IOException {
        if (bitHead > 0) {
            //left shift
            int bytesUsed = (bitHead+7) / 8;
            int emptyBits = bytesUsed*8 - bitHead;
            // unused bytes = 4-bytesUsed

            // shift so that any nonfilled bytes are then filled and the last byte has 0s
            intBuffer <<= emptyBits;

            for (int i = 0; i < bytesUsed; i++) {
                int shift = 24 - (i * 8);
                writeAndHash((intBuffer >>> shift) & 0xFF);
            }

            bitHead = 0;
            intBuffer = 0;
        }

        buffer.flush();
    }

    private void flushBuffer() throws IOException {
        writeAndHash((intBuffer >>> 24) & 0xFF);
        writeAndHash((intBuffer >>> 16) & 0xFF);
        writeAndHash((intBuffer >>> 8) & 0xFF);
        writeAndHash(intBuffer & 0xFF);

        intBuffer = 0;
        bitHead = 0;
    }

    private void writeAndHash(int updateValue) throws IOException {
        buffer.write(updateValue);
        intermediaryAdler.update(updateValue);
        masterAdler.update(updateValue);
    }

    public void close(int endingBytes) throws IOException {
        flushRemaining();

        int masterHash = (int) masterAdler.getValue();

        buffer.write((masterHash >>> 24) & 0xFF);
        buffer.write((masterHash >>> 16) & 0xFF);
        buffer.write((masterHash >>> 8) & 0xFF);
        buffer.write(masterHash & 0xFF);
        // write ending bytes
        buffer.write((endingBytes >>> 24) & 0xFF);
        buffer.write((endingBytes >>> 16) & 0xFF);
        buffer.write((endingBytes >>> 8) & 0xFF);
        buffer.write(endingBytes & 0xFF);

        buffer.close();
    }

    public void close() throws IOException {
        flushRemaining();

        int masterHash = (int) masterAdler.getValue();

        buffer.write((masterHash >>> 24) & 0xFF);
        buffer.write((masterHash >>> 16) & 0xFF);
        buffer.write((masterHash >>> 8) & 0xFF);
        buffer.write(masterHash & 0xFF);

        buffer.close();
    }
}
