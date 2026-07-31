package com.maxgamertyper1.CodecIO;

import com.maxgamertyper1.datatypes.CompressedColorChannel;
import com.maxgamertyper1.datatypes.CompressedImageRepresentative;

import java.io.FileNotFoundException;
import java.io.IOException;

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
}
