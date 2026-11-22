package com.maxgamertyper1.CodecIO.Encoding;

import com.maxgamertyper1.compressor.CompressionConfig;
import com.maxgamertyper1.datatypes.CompressedColorChannel;
import com.maxgamertyper1.datatypes.CompressedImageRepresentative;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.PixelHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class QDCEncoder {
    final FormattedWriter fileWritter;
    final ChannelWriter channelWriter;
    final CompressionConfig config;
    private final CompressedImageRepresentative finalImage;
    private final ImageMeta metaData;

    public QDCEncoder(CompressedImageRepresentative imageRepresentative, Path outFile, CompressionConfig config) throws FileNotFoundException {
        finalImage = imageRepresentative;
        metaData = finalImage.getMetaData();

        fileWritter = new FormattedWriter(outFile);
        channelWriter = new ChannelWriter();
        this.config = config;
    }

    private static int zigzagEncode(int number) {
        //0: 0, 1: -1, 10: 1, 11:-2, ...
        return (number << 1) ^ (number >> 31);
    }

    public void writeMetadata() throws IOException {
        boolean maxAlpha = metaData.opaque();
        boolean grayScale = metaData.grayscale();

        CompressedColorChannel[] channels = finalImage.getCompressedChannels();
        byte[] initialPixels = new byte[channels.length];

        for (int i = 0; i < channels.length; i++) {
            initialPixels[i] = (byte) channels[i].getInitialPixel();
        }

        try (fileWritter) {
            fileWritter.calculateMetadataFlagByte(
                    grayScale,
                    maxAlpha,
                    metaData.fileType()
            );

            fileWritter.setImageData(
                    (short) metaData.height(),
                    (short) metaData.width()
            );

            fileWritter.finalizeMetadata(initialPixels);
        }
    }

    public void preprocessChannel(CompressedColorChannel colorChannel) throws IOException {
        // find what shapes belong to what corners (hierarchy basically, but its directly used later)
        if (config.cornersEnabled()) {
            colorChannel.calculateCornerOverrides();
        }

        colorChannel.zigzagIsolated(); // turn the isolated values into zigzag form
        int[] optimalUnary = calculateChannelUnary(colorChannel);

        //write the channel-META but not byte size, the formattedWriter will do that
        channelWriter.setData(colorChannel.getFirstType(), optimalUnary);
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
        for (int i = 10; i < 32; i++) {
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

    public void processChannel(int index) throws IOException { // orchestrator
        CompressedColorChannel thisChannel = finalImage.getCompressedChannels()[index];
        preprocessChannel(thisChannel);

        if (config.cornersEnabled()) {
            processCornerIncluded(thisChannel);
        } else {
            processWithoutCorners(thisChannel); // working on this right now
        }
    }

    private void processWithoutCorners(CompressedColorChannel channel) throws IOException {
        RectangleRepresentative[] rects = channel.getBuiltRectangles();
        int[] rawIndexes = channel.getIsolatedIndexes();
        int[] rawValues = channel.getCorrespondingIsolatedValues();

        int currentRectangle = 0, currentPixel = 0;
        while (currentRectangle < rects.length || currentPixel < rawIndexes.length) {
            boolean takeRect = currentPixel == rawIndexes.length ||
                    (currentRectangle < rects.length && rects[currentRectangle].startingIndex() < rawIndexes[currentPixel]);

            if (takeRect) {
                channelWriter.writeRectangleData(rects[currentRectangle++]);
            } else {
                channelWriter.writeRawData(rawValues[currentPixel++]);
            }
        }

        try (fileWritter) {
            fileWritter.writeChannel(channelWriter.getByteStream(), channelWriter.lastEmptyBits(), channel.getChannelColorType());
        }
    }

    // old processing method
    private void processCornerIncluded(CompressedColorChannel thisChannel) throws IOException {
        int totalItems = thisChannel.getRectangleCount() + thisChannel.getCornerCount() + thisChannel.getIsolatedCount();

        if (thisChannel.getIsolatedCount() != 0) totalItems--;
        if (thisChannel.getRectangleCount() != 0) totalItems--;
        if (thisChannel.getCornerCount() != 0) totalItems--;

        int rectIdx = 0;
        int cornerIdx = 0;
        int rawIdx = 0;
        CornerDataRepresentative[] builtCorners = thisChannel.getBuiltCorners();
        RectangleRepresentative[] builtRectangles = thisChannel.getBuiltRectangles();
        int[] builtRaws = thisChannel.getIsolatedIndexes();
        int[] rawValues = thisChannel.getCorrespondingIsolatedValues();

        try (channelWriter) {
            for (int i = 0; i <= totalItems; ) {
                int rectPosition = (rectIdx < builtRectangles.length) ? builtRectangles[rectIdx].startingIndex() : Integer.MAX_VALUE;
                int cornerPosition = (cornerIdx < builtCorners.length) ? builtCorners[cornerIdx].startingIndex() : Integer.MAX_VALUE;
                int rawPosition = (rawIdx < builtRaws.length) ? builtRaws[rawIdx] : Integer.MAX_VALUE;

                if (rectPosition < rawPosition && rectPosition < cornerPosition && !thisChannel.isOwnedRectangle(rectIdx)) { // rect
                    RectangleRepresentative thisRect = builtRectangles[rectIdx++];
                    channelWriter.writeRectangleData(thisRect);
                    i++;
                } else if (rawPosition < cornerPosition && !thisChannel.isOwnedIndex(rawIdx)) { //raw
                    int thisRawValue = rawValues[rawIdx++];
                    channelWriter.writeRawData(thisRawValue);
                    i++;
                } else { //corner
                    CornerDataRepresentative thisCorner = builtCorners[cornerIdx];
                    i += processCornerWrite(thisChannel, thisCorner, cornerIdx);
                    cornerIdx++;
                }
            }
        }

        try (fileWritter) {
            fileWritter.writeChannel(channelWriter.getByteStream(), channelWriter.lastEmptyBits(), thisChannel.getChannelColorType());
        }
    }

    //this is actually disgusting (old)
    private int processCornerWrite(
            CompressedColorChannel thisChannel,
            CornerDataRepresentative corner,
            int cornerIdx
    ) throws IOException {
        int[] rectOverrides = thisChannel.getOwnedRectangles(cornerIdx);
        int[] rawOverrides = thisChannel.getOwnedIndexes(cornerIdx);
        int[] cornerOverrides = thisChannel.getOwnedCorners(cornerIdx);
        int totalOverrides = rectOverrides.length + rawOverrides.length + cornerOverrides.length;

        boolean hasOverrides = totalOverrides != 0;

        channelWriter.writeCornerData(corner, hasOverrides);

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
        int[] previousLocation = PixelHelper.ReverseIndexCalculator(metaData.width(), builtCorners[cornerIdx].startingIndex());
        int[] deltaInfo = new int[2];

        if (rectOverrides.length != 0) totalOverrides--;
        if (rawOverrides.length != 0) totalOverrides--;
        if (cornerOverrides.length != 0) totalOverrides--;

        for (int i = 0; i <= totalOverrides; ) {
            int rectPosition = (rectOverrideIdx < rectOverrides.length) ? builtRects[rectOverrides[rectOverrideIdx]].startingIndex() : Integer.MAX_VALUE;
            int cornerPosition = (cornerOverrideIdx < cornerOverrides.length) ? builtCorners[cornerOverrides[cornerOverrideIdx]].startingIndex() : Integer.MAX_VALUE;
            int rawPosition = (rawOverrideIdx < rawOverrides.length) ? builtRaws[rawOverrides[rawOverrideIdx]] : Integer.MAX_VALUE;
            int[] thisLocation;

            if (rectPosition < rawPosition && rectPosition < cornerPosition) { // rect
                if (i == 0) {
                    channelWriter.writeRawOPCode(DataType.Rectangle);
                }
                RectangleRepresentative thisRect = builtRects[rectOverrides[rectOverrideIdx++]];
                thisLocation = PixelHelper.ReverseIndexCalculator(metaData.width(), thisRect.startingIndex());
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeOverriden(deltaInfo, thisRect);
                totalChanges += 1;
            } else if (rawPosition < cornerPosition) { //raw
                if (i == 0) {
                    channelWriter.writeRawOPCode(DataType.Raw);
                }
                int overrideIndex = rawOverrides[rawOverrideIdx++];
                int thisRawValue = rawValues[overrideIndex];
                int thisRawIndex = builtRaws[overrideIndex];

                thisLocation = PixelHelper.ReverseIndexCalculator(metaData.width(), thisRawIndex);
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeOverriden(deltaInfo, thisRawValue);
                totalChanges += 1;
            } else { //corner
                if (i == 0) {
                    channelWriter.writeRawOPCode(DataType.Corner);
                }
                int thisCornerIdx = cornerOverrides[cornerOverrideIdx];
                CornerDataRepresentative thisCorner = builtCorners[thisCornerIdx];

                thisLocation = PixelHelper.ReverseIndexCalculator(metaData.width(), thisCorner.startingIndex());
                deltaInfo[0] = zigzagEncode(thisLocation[0] - previousLocation[0]);
                deltaInfo[1] = zigzagEncode(thisLocation[1] - previousLocation[1]);

                channelWriter.writeDeltas(deltaInfo);
                totalChanges += processCornerWrite(thisChannel, thisCorner, thisCornerIdx);
                cornerOverrideIdx++;
            }

            previousLocation = thisLocation;
            i++;
        }
        channelWriter.writeEscapeData();

        return totalChanges;
    }
}
