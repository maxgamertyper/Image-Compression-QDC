package com.maxgamertyper1.CodecIO.Encoding;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.imagereading.OriginalFileType;

import java.io.*;

public class FormattedWriter implements Closeable {
    public static final byte COMPRESSION_VERSION_MAJOR = 0;
    public static final byte COMPRESSION_VERSION_MINOR = 3;
    private static final int FILE_SIGNATURE = 0x3C4D4158; // this is <MAX when converted to ascii
    private static final int FILE_END_BYTES = 0x4D415821; // this is MAX! when converted to ascii
    private final RawBitWriter bitWriter;
    private final BufferedOutputStream bufferStream;
    private final int[] channelSeekTable = {0, 0, 0, 0, 0};
    private short thisImageHeight; // unsigned but manually
    private short thisImageWidth; // unsigned but manually
    private byte flagByte;


    public FormattedWriter(String outFile) throws FileNotFoundException {
        FileOutputStream fileStream = new FileOutputStream(outFile);
        bufferStream = new BufferedOutputStream(fileStream);
        bitWriter = new RawBitWriter(bufferStream);
    }

    //METAdata
    public void setImageData(short imageHeight, short imageWidth) {
        thisImageHeight = imageHeight;
        thisImageWidth = imageWidth;
    }

    public void calculateMetadataFlagByte(boolean isGrayscale, boolean isMaximumAlpha, OriginalFileType originalFileType) {
        // isGrayscale (1 bit), isMaxAlpha (1 bit), original type (2 bits), empty bits (4)
        flagByte = 0;
        byte typeBits = originalFileType == OriginalFileType.Unknown ? (byte) 0 :
                originalFileType == OriginalFileType.PNG ? (byte) 3 : 2;

        flagByte |= isGrayscale ? (byte) (1 << 7) : 0;
        flagByte |= isMaximumAlpha ? (byte) (1 << 6) : 0;
        //unknow is 00, png is 11, jpg is 10
        flagByte |= (byte) (typeBits << 4);
    }

    public void finalizeMetadata(byte[] initialColors) throws IOException {
        bitWriter.writeRawValue(FILE_SIGNATURE, 32);
        bitWriter.writeRawValue(COMPRESSION_VERSION_MAJOR, 8);
        bitWriter.writeRawValue(COMPRESSION_VERSION_MINOR, 8);
        bitWriter.writeRawValue(thisImageWidth, 16);
        bitWriter.writeRawValue(thisImageHeight, 16);
        bitWriter.writeRawValue(flagByte, 8);

        //RGBYA
        for (byte color : initialColors) {
            bitWriter.writeRawValue(color, 8);
        }
        //metadata checksum
        bitWriter.flushIntermediaryAdler();
    }

    public void finishFile() throws IOException {
        bitWriter.close(FILE_END_BYTES); // write the ending bytes, flush, save
    }

    public void writeChannel(ByteArrayOutputStream channelData, int emptyBits, ChannelColorTypes channelColor) throws IOException {
        channelSeekTable[channelColor.getOrderValue()] = bitWriter.getBytesWritten();
        System.out.println(channelData.size());

        bitWriter.writeRawValue(channelData.size() - 4, 32); // 4 bytes to hold the channel size (-4 from the integrity hash)
        bitWriter.writeRawValue(emptyBits, 8);
        bitWriter.flushRemaining();
        channelData.writeTo(bufferStream);
        bitWriter.addBytesWritten(channelData.size());

        //update masterAdler
        bitWriter.updateMasterAdler(channelData);
    }

    public void writeTail() throws IOException {
        for (int byteLocation : channelSeekTable) {
            bitWriter.writeRawValue(byteLocation, 32); // writes the byte position and in order
        }
    }

    @Override
    public void close() throws IOException {
        bitWriter.flushRemaining(); // byte aligns the next segment
    }
}
