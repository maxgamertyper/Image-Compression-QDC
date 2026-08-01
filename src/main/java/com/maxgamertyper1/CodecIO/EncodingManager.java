package com.maxgamertyper1.CodecIO;

import com.maxgamertyper1.datatypes.CompressedColorChannel;
import com.maxgamertyper1.datatypes.CompressedImageRepresentative;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class EncodingManager {
    final FormattedWriter fileWritter;
    ChannelWriter individualWriter;
    CompressedImageRepresentative finalImage;

    public EncodingManager(CompressedImageRepresentative imageRepresentative, String outFile) throws FileNotFoundException {
        finalImage = imageRepresentative;
        fileWritter = new FormattedWriter(outFile);
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

    public void preprocessChannel(CompressedColorChannel colorChannel) {
        // order all data sequentially (will happen during encoding)

        // find what shapes belong to what corners
        colorChannel.calculateCornerOverrides();
        colorChannel.zigzagIsolated(); // turn the isolated values into zigzag form
        int[] optimalUnary = calculateChannelUnary(colorChannel);

        System.out.println(Arrays.toString(optimalUnary));


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


    public void processChannel(int index) throws IOException {
        preprocessChannel(finalImage.getCompressedChannels()[index]);
        //TODO: write the channel data
    }
}
