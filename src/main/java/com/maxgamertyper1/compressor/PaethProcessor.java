package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.ColorChannel;
import com.maxgamertyper1.datatypes.ImageRepresentative;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.PixelHelper;

public final class PaethProcessor {
    private PaethProcessor() {}

    private static int getPrediction(int leftPixelColor, int topPixelColor, int upperLeftPixelColor) {
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

    public static ImageRepresentative paethEncode(ImageRepresentative image) {
        ImageMeta metaData = image.getMetaData();
        int width = metaData.width();

        ImageRepresentative paethImage = new ImageRepresentative(metaData);

        for (ColorChannel channel : image.getColorChannels()) {
            int[] rawPixelData = channel.getChannelData();

            for (int pixelIndex = 0; pixelIndex < rawPixelData.length; pixelIndex++) {
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

    public static void paethDecode(int[] pixelData, int initialPixel, int width) {
        pixelData[0] = initialPixel;

        int height = pixelData.length / width;

        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = (y == 0 ? 1 : 0); x < width; x++) {
                int i = row + x;
                int left = x > 0 ? pixelData[i - 1] : 0;
                int up = y > 0 ? pixelData[i - width] : 0;
                int upperLeft = (x > 0 && y > 0) ? pixelData[i - width - 1] : 0;
                pixelData[i] += getPrediction(left, up, upperLeft);
            }
        }
    }
}
