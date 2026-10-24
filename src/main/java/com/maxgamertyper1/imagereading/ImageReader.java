package com.maxgamertyper1.imagereading;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.ImageRepresentative;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageReader {
    private final File imageFile;
    private int imageHeight;
    private int imageWidth;
    private String imageName;
    private OriginalFileType imageType;
    private boolean imageOpaque = true;
    private boolean imageGrayscale = true;

    public ImageReader(File imageFile) {
        this.imageFile = imageFile;
    }

    public ImageRepresentative readImageFile() throws IOException {
        getFileData();

        int[] pixelData = getPixelData();

        imageChannelVerification(pixelData);

        ImageMeta metaData = consolidateMetaData();

        return splitColorArray(pixelData, metaData);
    }

    private ImageMeta consolidateMetaData() {
        return new ImageMeta(
                imageHeight,
                imageWidth,
                imageGrayscale,
                imageOpaque,
                imageType,
                imageName
        );
    }

    private void getFileData() {
        imageName = imageFile.getName();
        String path = imageFile.getAbsolutePath();

        int lastIndexOfDot = path.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            System.err.println("No file path found, continuing");
        }

        try {
            imageType = OriginalFileType.valueOf(path.substring(lastIndexOfDot + 1).toUpperCase());
        } catch (IllegalArgumentException e) {
            imageType = OriginalFileType.Unknown;
        }
    }

    private int[] getPixelData() throws IOException {
        BufferedImage image;

        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            System.out.println("Error occured when reading the image file");
            throw new RuntimeException(e);
        }

        if (image == null) {
            throw new IOException("Unsupported Image Format: " + imageFile);
        }

        imageWidth = image.getWidth();
        imageHeight = image.getHeight();

        return image.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
    }

    private void imageChannelVerification(int[] pixelData) {
        for (int color : pixelData) {
            if (!imageGrayscale && !imageOpaque) break;

            if (imageOpaque && ((color >> 24) & 0xFF) != 255) imageOpaque = false;

            if (imageGrayscale) {
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (r != g || g != b) {
                    imageGrayscale = false;
                }
            }
        }
    }

    private ImageRepresentative splitColorArray(int[] pixelData, ImageMeta metaData) {
        ImageRepresentative returnHolder = new ImageRepresentative(metaData);

        for (int cIndex = 0; cIndex < pixelData.length; cIndex++) {
            int color = pixelData[cIndex];
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            if (imageGrayscale) {
                returnHolder.setPixelColorChannel(ChannelColorTypes.Gray, cIndex, r);
            } else {
                returnHolder.setPixelColorChannel(ChannelColorTypes.Red, cIndex, r);
                returnHolder.setPixelColorChannel(ChannelColorTypes.Green, cIndex, g);
                returnHolder.setPixelColorChannel(ChannelColorTypes.Blue, cIndex, b);
            }
            if (!imageOpaque) returnHolder.setPixelColorChannel(ChannelColorTypes.Alpha, cIndex, a);
        }

        return returnHolder;
    }
}