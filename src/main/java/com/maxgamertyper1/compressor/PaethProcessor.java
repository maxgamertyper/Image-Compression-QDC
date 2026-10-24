package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.ColorChannel;
import com.maxgamertyper1.datatypes.ImageRepresentative;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.PixelHelper;

public class PaethProcessor {
    ImageRepresentative image;

    public PaethProcessor(ImageRepresentative imageToProcess) {
        image = imageToProcess;
    }

    private int getPrediction(int leftPixelColor, int topPixelColor, int upperLeftPixelColor) {
        int paethPrediction = leftPixelColor + topPixelColor - upperLeftPixelColor;

        int leftDistance = Math.abs(paethPrediction - leftPixelColor);
        int topDistance = Math.abs(paethPrediction - topPixelColor);
        int upperLeftDistance = Math.abs(paethPrediction - upperLeftPixelColor);

        int prediction;
        if (leftDistance <= topDistance && leftDistance <= upperLeftDistance) {
            prediction = leftPixelColor;
        } else if (topDistance <= upperLeftDistance) {
            prediction = topPixelColor;
        } else {
            prediction = upperLeftPixelColor;
        }
        return prediction;
    }

    public ImageRepresentative paethEncode() {
        ImageMeta metaData = image.getMetaData();
        int width = metaData.width();

        ImageRepresentative paethImage = new ImageRepresentative(metaData);

        for (ColorChannel channel : image.getColorChannels()) {
            int[] rawPixelData = channel.getChannelData();

            for (int pixelIndex = 0; pixelIndex < rawPixelData.length - 1; pixelIndex++) {
                int pixelColor = rawPixelData[pixelIndex];
                boolean onTop = false;

                int leftPixelColor, topPixelColor, upperLeftPixelColor;
                if (PixelHelper.IndexIsOnTopRow(width, pixelIndex)) {
                    topPixelColor = 0;
                    upperLeftPixelColor = 0;
                    onTop = true;
                } else {
                    topPixelColor = rawPixelData[pixelIndex - width];
                    upperLeftPixelColor = 1; // mark top row clear
                }

                if (PixelHelper.IndexIsOnLeftColomn(width, pixelIndex)) {
                    leftPixelColor = 0;
                    upperLeftPixelColor = 0;

                    if (onTop) { // very first pixel
                        paethImage.setIntialPixel(channel.getChannelType(), pixelColor);
                        paethImage.setPixelColorChannel(channel.getChannelType(), pixelIndex, 0);
                        continue;
                    }
                } else {
                    leftPixelColor = rawPixelData[pixelIndex - 1];
                    if (upperLeftPixelColor == 1) upperLeftPixelColor = rawPixelData[pixelIndex - 1 - width];
                }

                int prediction = getPrediction(leftPixelColor, topPixelColor, upperLeftPixelColor);
                int delta = pixelColor - prediction;
                paethImage.setPixelColorChannel(channel.getChannelType(), pixelIndex, delta);
            }
        }

        return paethImage;
    }
}
