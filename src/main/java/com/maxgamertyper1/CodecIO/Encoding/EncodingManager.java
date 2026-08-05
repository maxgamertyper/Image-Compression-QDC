package com.maxgamertyper1.CodecIO.Encoding;

import com.maxgamertyper1.datatypes.CompressedColorChannel;
import com.maxgamertyper1.datatypes.CompressedImageRepresentative;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.PixelHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class EncodingManager {
    final FormattedWriter fileWritter;
    final ChannelWriter channelWriter;
    CompressedImageRepresentative finalImage;

    public EncodingManager(CompressedImageRepresentative imageRepresentative, String outFile) throws FileNotFoundException {
        finalImage = imageRepresentative;
        fileWritter = new FormattedWriter(outFile);
        channelWriter = new ChannelWriter();
    }

    public void writeMetadata() throws IOException {
        boolean maxAlpha = finalImage.isUniformMaxAlpha();
        boolean grayScale = finalImage.isGrayScale();

        CompressedColorChannel[] channels = finalImage.getCompressedChannels();
        byte[] initialPixels = new byte[channels.length];

        for (int i=0; i < channels.length; i++) {
            initialPixels[i] = (byte) channels[i].getInitialPixel();
        }

        try (fileWritter) {
            fileWritter.calculateMetadataFlagByte(
                    grayScale,
                    maxAlpha,
                    finalImage.getOriginalFileType()
            );

            fileWritter.setImageData(
                    finalImage.getImageHeight(),
                    finalImage.getImageWidth()
            );

            fileWritter.finalizeMetadata(initialPixels);
        }
    }

    public void preprocessChannel(CompressedColorChannel colorChannel) throws IOException {

        // find what shapes belong to what corners (hierarchy basically, but its directly used later)
        colorChannel.calculateCornerOverrides();
        colorChannel.zigzagIsolated(); // turn the isolated values into zigzag form
        int[] optimalUnary = calculateChannelUnary(colorChannel);
        System.out.println(Arrays.toString(optimalUnary));

        //write the channel-META but not byte size, the formattedWriter will do that
        channelWriter.setData(colorChannel.getFirstType(),optimalUnary);
    }

    private int[] calculateChannelUnary(CompressedColorChannel colorChannel) {
        int[] bitLengthCount = countBitLengths(colorChannel);// index 0 is 1 bit length, 1 is 2, ..., 9 is overflow

        UnaryCalculator calculator = new UnaryCalculator(bitLengthCount);

        return calculator.calculateOptimalCombination();
    }

    private int[] countBitLengths(CompressedColorChannel colorChannel) {
        int[] bitCounts = new int[32];

        for (int isolatedValue : colorChannel.getCorrespondingIsolatedValues()) {
            bitCounts[dataBitLength(isolatedValue) - 1]++;
        }
        for (RectangleRepresentative rectangle : colorChannel.getBuiltRectangles()) {
            bitCounts[dataBitLength(rectangle.width()) - 1]++;
            bitCounts[dataBitLength(rectangle.height()) - 1]++;
            bitCounts[dataBitLength(rectangle.zigzagValue()) - 1]++;
        }
        for (CornerDataRepresentative corner : colorChannel.getBuiltCorners()) {
            bitCounts[dataBitLength(corner.zigzagValue()) - 1]++;

            for (int cornerDelta : corner.convexCornerDeltaDxDy()) {
                bitCounts[dataBitLength(cornerDelta) - 1]++;
            }
        }

        //compress >9
        for (int i=10; i < 32; i++) {
            bitCounts[9] += bitCounts[i]; //bitcounts[9] requires the OVERFLOW prefix and the rest of it does too so it just pushes all of them there
        }
        return Arrays.copyOf(bitCounts, 10);
    }

    private int dataBitLength(int data) {
        int baseReturn = (32 - Integer.numberOfLeadingZeros(data));
        return baseReturn == 0 ? 1 : baseReturn;
    }

    public void finalizeFile() throws IOException {
        fileWritter.writeTail();
        fileWritter.finishFile();
    }


    public void processChannel(int index) throws IOException {
        CompressedColorChannel thisChannel = finalImage.getCompressedChannels()[index];
        preprocessChannel(thisChannel);

        int totalItems = thisChannel.getRectangleCount() + thisChannel.getCornerCount() + thisChannel.getIsolatedCount();

        int rectIdx = 0;
        int cornerIdx = 0;
        int rawIdx = 0;
        CornerDataRepresentative[] builtCorners = thisChannel.getBuiltCorners();
        RectangleRepresentative[] builtRectangles = thisChannel.getBuiltRectangles();
        int[] builtRaws = thisChannel.getIsolatedIndexes();
        int[] rawValues = thisChannel.getCorrespondingIsolatedValues();

        try (channelWriter) {
            for (int i = 0; i < totalItems;) {
                int rectPosition = (rectIdx < builtRectangles.length) ? builtRectangles[rectIdx].startingIndex() : Integer.MAX_VALUE;
                int cornerPosition = (cornerIdx < builtCorners.length) ? builtCorners[cornerIdx].startingIndex() : Integer.MAX_VALUE;
                int rawPosition = (rawIdx < builtRaws.length) ? builtRaws[rawIdx] : Integer.MAX_VALUE;

                if (rectPosition < rawPosition && rectPosition < cornerPosition) { // rect
                    RectangleRepresentative thisRect = builtRectangles[rectIdx++];
                    channelWriter.writeRectangleData(thisRect);
                    i++;
                } else if (rawPosition < cornerPosition) { //raw
                    int thisRawValue = rawValues[rawIdx++];
                    channelWriter.writeRawData(thisRawValue);
                    i++;
                } else { //corner
                    CornerDataRepresentative thisCorner = builtCorners[cornerIdx];
                    i += processCornerWrite(thisChannel,thisCorner,cornerIdx);
                    cornerIdx++;
                }
            }
        }

        try (fileWritter) {
            fileWritter.writeChannel(channelWriter.getByteStream(), channelWriter.lastEmptyBits(), thisChannel.getChannelColorType());
        }
    }

    //this is actually disgusting
    private int processCornerWrite(
            CompressedColorChannel thisChannel,
            CornerDataRepresentative corner,
            int cornerIdx
    ) throws IOException{
        int[] rectOverrides = thisChannel.getOwnedRectangles(cornerIdx);
        int[] rawOverrides = thisChannel.getOwnedIndexes(cornerIdx);
        int[] cornerOverrides = thisChannel.getOwnedCorners(cornerIdx);
        int totalOverrides = rectOverrides.length+rawOverrides.length+cornerOverrides.length;

        boolean hasOverrides = totalOverrides!=0;

        channelWriter.writeCornerData(corner,hasOverrides);

        if (!hasOverrides) {
            return 1;
        }

        int rectOverrideIdx = 0;
        int cornerOverrideIdx = 0;
        int rawOverrideIdx = 0;
        RectangleRepresentative[] builtRects = thisChannel.getBuiltRectangles();
        CornerDataRepresentative[] builtCorners = thisChannel.getBuiltCorners();
        int[] builtRaws = thisChannel.getIsolatedIndexes();
        int[] rawValues = thisChannel.getCorrespondingIsolatedValues();
        int totalChanges = 1;
        int[] previousLocation = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,builtCorners[cornerIdx].startingIndex());
        int[] deltaInfo = new int[2];


        for (int i=0; i < totalOverrides;) {
            int rectPosition = (rectOverrideIdx < rectOverrides.length) ? builtRects[rectOverrides[rectOverrideIdx]].startingIndex() : Integer.MAX_VALUE;
            int cornerPosition = (cornerOverrideIdx < cornerOverrides.length) ? builtCorners[cornerOverrides[cornerOverrideIdx]].startingIndex() : Integer.MAX_VALUE;
            int rawPosition = (rawOverrideIdx < rawOverrides.length) ? builtRaws[rawOverrides[rawOverrideIdx]] : Integer.MAX_VALUE;
            int[] thisLocation;

            if (rectPosition < rawPosition && rectPosition < cornerPosition) { // rect
                RectangleRepresentative thisRect = builtRects[rectOverrides[rectOverrideIdx++]];
                thisLocation = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,thisRect.startingIndex());
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeOverriden(deltaInfo,thisRect);
            } else if (rawPosition < cornerPosition) { //raw
                int overrideIndex = rawOverrides[rawOverrideIdx++];
                int thisRawValue = rawValues[overrideIndex];
                int thisRawIndex = builtRaws[overrideIndex];

                thisLocation = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,thisRawIndex);
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeOverriden(deltaInfo, thisRawValue);
            } else { //corner
                int thisCornerIdx = cornerOverrides[cornerOverrideIdx];
                CornerDataRepresentative thisCorner = builtCorners[thisCornerIdx];

                thisLocation = PixelHelper.ReverseIndexCalculator(ImageDataHolder.width,thisCorner.startingIndex());
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeDeltas(deltaInfo);
                totalChanges += processCornerWrite(thisChannel,thisCorner,thisCornerIdx);
                cornerOverrideIdx++;
            }

            previousLocation = thisLocation;
            i++;
        }

        return totalChanges;
    }

    private static int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-2, ...
        return (number << 1) ^ (number >> 31);
    }
}
