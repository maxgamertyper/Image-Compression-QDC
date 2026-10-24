package com.maxgamertyper1.CodecIO.Encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Adler32;

public class RawBitWriter {
    private final OutputStream buffer;
    private final Adler32 intermediaryAdler;
    private final Adler32 masterAdler;
    private boolean closed = false;
    private int bytesWritten = 0;
    private int emptyBitsFromFlushRemaining = 0;
    private int bitHead = 0;
    private int intBuffer = 0;

    public RawBitWriter(OutputStream stream) {
        this.buffer = stream;
        intermediaryAdler = new Adler32();
        masterAdler = new Adler32();
    }

    public void addBytesWritten(int count) {
        bytesWritten += count;
    }

    public void updateMasterAdler(ByteArrayOutputStream stream) {
        masterAdler.update(stream.toByteArray(), 0, stream.size());
    }

    public void writeRawValue(int value, int numBits) throws IOException {
        System.out.println("Writing value: " + value + " with bit count: " + numBits);
        if (bitHead + numBits > 32) {
            System.out.println("Overflowing, splitting payload");
            int bitsToFill = 32 - bitHead;
            int overflowBits = numBits - bitsToFill;

            int upperBits = value >>> overflowBits; // cut off overflowbits
            writeRawValue(upperBits, bitsToFill); // write data

            int lowerMask = (overflowBits == 32) ? -1 : (1 << overflowBits) - 1; // shift either -1 or like
            int lowerBits = value & lowerMask; // cut off top
            writeRawValue(lowerBits, overflowBits); // write bits
        } else {
            int mask = (numBits == 32) ? -1 : (1 << numBits) - 1; // java is stinky and only uses 5 bits as a shift on a 32-bit number so otherwise do -1 which is 0xFFFFFFFF
            int maskedValue = value & mask; // mask the value to the number of bits specified

            intBuffer <<= numBits; // shift left X values
            intBuffer |= maskedValue; // or the value into the slots
            bitHead += numBits;

            if (bitHead == 32) {
                flushBuffer();
            }
        }
    }

    public void flushIntermediaryAdler() throws IOException {
        int hash = (int) intermediaryAdler.getValue();
        writeRawValue(hash, 32);
        intermediaryAdler.reset();
    }

    public void flushRemaining() throws IOException {
        System.out.println("Called FlushRemaining with buffer: " + intBuffer + " with size: " + bitHead);
        if (bitHead > 0) {
            int bytesUsed = (bitHead + 7) / 8;
            int emptyBits = 32 - bitHead;

            // shift so that any nonfilled bytes are then filled and the last byte has 0s
            intBuffer <<= emptyBits;

            for (int i = 0; i < bytesUsed; i++) {
                int shift = 24 - (i * 8);
                writeAndHash((intBuffer >>> shift) & 0xFF);
            }

            emptyBitsFromFlushRemaining = (bytesUsed * 8) - bitHead;
            bytesWritten += bytesUsed;
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
        System.out.println("Flushing buffer with value: " + intBuffer);


        bytesWritten += 4;
        intBuffer = 0;
        bitHead = 0;
    }

    private void writeAndHash(int updateValue) throws IOException {
        buffer.write(updateValue);
        intermediaryAdler.update(updateValue);
        masterAdler.update(updateValue);
    }

    public void close(int endingBytes) throws IOException {
        if (closed) return;
        closed = true;

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
        if (closed) return;
        closed = true;

        flushRemaining();

        int masterHash = (int) masterAdler.getValue();

        buffer.write((masterHash >>> 24) & 0xFF);
        buffer.write((masterHash >>> 16) & 0xFF);
        buffer.write((masterHash >>> 8) & 0xFF);
        buffer.write(masterHash & 0xFF);

        buffer.close();
    }

    public int getBytesWritten() {
        return bytesWritten;
    }

    public int getEmptyBitsFromFlushRemaining() {
        return emptyBitsFromFlushRemaining;
    }
}