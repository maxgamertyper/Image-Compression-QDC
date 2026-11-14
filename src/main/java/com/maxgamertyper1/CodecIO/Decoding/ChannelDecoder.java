package com.maxgamertyper1.CodecIO.Decoding;

import com.maxgamertyper1.CodecIO.Encoding.DataType;
import com.maxgamertyper1.imagereading.ImageMeta;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class ChannelDecoder {
    private static final int COLOR_POSITON = 0;
    private static final int WIDTH_POSITON = 1;
    private static final int HEIGHT_POSITON = 2;

    private final ImageMeta metaData;
    private final Parser parser;

    private final int[] channelColorData;
    private final BitSet processedMap;
    private int headPointer = 0;


    public ChannelDecoder(ImageMeta metaData, Parser parser) {
        this.metaData = metaData;
        this.parser = parser;

        processedMap = new BitSet(metaData.flatsize());
        channelColorData = new int[metaData.flatsize()];
    }

    public void processChannelMeta() throws IOException {
        parser.readChannelSizeData();

        parser.loadUnaryTable();
        parser.getUnaryTable();

        parser.readExtendedOPCode();
        //genuinley just load the stuff into the parser since i dont need it here, i just need shapes
    }

    public void processChannelData() throws IOException, UnsupportedOperationException {
        // literally just the one from the disassembler
        while (parser.hasMoreBits()) {
            //processes 1 "shape" per loop

            if (parser.prepareNextLength()) { // set the value length and return if its an escape or not
                //ESCAPE code (not overflow, already handled)
                parser.processEscape();

                //op code switch from escape here
            } else {
                int[] shapeValues = parser.processShape();

                if (parser.isInOverride()) {
                    throw new UnsupportedOperationException("Corner Encoding not yet implemented");
                }

                shapeDispatcher(shapeValues, parser.getCurrentType());

                // value reads here

                if (shapeValues[shapeValues.length - 1] == 1 && parser.getCurrentType() == DataType.Corner) { // if override
                    parser.updateOverrideData(true);
                    //opcode switch out of override here
                }
            }
        }
    }

    private void shapeDispatcher(int[] shapeValues, DataType dataType) throws UnsupportedOperationException { // this will need override data later i guess

        if (dataType == DataType.Raw) {
            handleRawData(shapeValues[COLOR_POSITON]); // auto un-zigzaged
        } else if (dataType == DataType.Rectangle) {
            handleRectangleData(shapeValues[COLOR_POSITON],shapeValues[WIDTH_POSITON],shapeValues[HEIGHT_POSITON]);
        } else if (dataType == DataType.Corner) {
            throw new UnsupportedOperationException("Corner Encoding not yet implemented");
        } else {
            throw new UnsupportedOperationException("Unknown Shape type Encoding not yet implemented");
        }

    }

    private void advanceHead() {
        headPointer = processedMap.nextClearBit(headPointer);
    }

    private void handleRawData(int color) {
        channelColorData[headPointer] = color;
        processedMap.set(headPointer);
        advanceHead();
    }

    private void placePixels(int color, int from, int to) {
        processedMap.set(from,to);
        Arrays.fill(channelColorData,from,to,color);
        advanceHead();
    }

    private void handleRectangleData(int color, int width, int height) {
        int start = headPointer;
        int imageWidth = metaData.width();

        for (int row = 0; row < height; row++) {
            int from = start + row * imageWidth;
            placePixels(color, from, from + width);
        }
    }

    public void processChannelTail()  throws IOException {
        parser.readHash();
        System.out.println("ADLER CHANNEL HASH NEEDS IMPLEMENTATION");
    }

    public int[] getChannelColorData() {
        return channelColorData;
    }
}
