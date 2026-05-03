package com.maxgamertyper1.imagereading;
import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.records.DataSimplifyingReturn;
import com.maxgamertyper1.datatypes.ImageRepresentative;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.maxgamertyper1.imagereading.PixelHelper.IndexCalculator;

public class ImageReader {

    public static void ReadAllImageContent(String ImagePath) {
        File imageFile = ImageReader.FillDataHolder(ImagePath);
        int[] rawpixels = ImageReader.GetRGBValues(imageFile);
        DataSimplifyingReturn DSR = GrayScaleVerification();
        ImageDataHolder.isGrayscale = DSR.grayscale();
        ImageDataHolder.UniformMaxAlpha = DSR.uniformAlpha();
        SplitColorArray(ImageDataHolder.flatsize,ImageDataHolder.isGrayscale,rawpixels);
    }

    private static File FillDataHolder(String ImagePath) {
        File imageFile = new File(ImagePath);
        ImageDataHolder.metaImageName = imageFile.getName();

        int lastIndexOfDot = ImagePath.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            System.err.println("No file path found, continuing");
        }

        try {
            ImageDataHolder.fileType = OriginalFileType.valueOf(ImagePath.substring(lastIndexOfDot + 1).toUpperCase());
        } catch (IllegalArgumentException e){
            ImageDataHolder.fileType = OriginalFileType.Unknown;
        }
        return imageFile;
    }

    private static int[] GetRGBValues(File imageFile) {
        System.out.println(imageFile.getAbsolutePath());
        BufferedImage image;

        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            System.out.println("Error occured when reading the image file");
            throw new RuntimeException(e);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] Colors = new int[height * width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int colors = image.getRGB(x, y);
                Colors[IndexCalculator(width, x, y)] = colors;
            }
        }

        ImageDataHolder.width = width;
        ImageDataHolder.height = height;
        ImageDataHolder.flatsize = height*width;
        ImageDataHolder.rawPixels = Colors;
        return Colors;
    }

    private static void SplitColorArray(int flatsize, boolean baseGrayscale, int[] rawPixels) {
        ImageDataHolder.channeledColors = new ImageRepresentative(baseGrayscale,ImageDataHolder.UniformMaxAlpha, false,flatsize);

        for (int cIndex=0;cIndex< rawPixels.length; cIndex++) {
            int color = rawPixels[cIndex];
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            if (baseGrayscale) {
                ImageDataHolder.channeledColors.setPixelColorChannel(ChannelColorTypes.Gray, cIndex, r);
            } else {
                ImageDataHolder.channeledColors.setPixelColorChannel(ChannelColorTypes.Red, cIndex, r);
                ImageDataHolder.channeledColors.setPixelColorChannel(ChannelColorTypes.Green, cIndex, g);
                ImageDataHolder.channeledColors.setPixelColorChannel(ChannelColorTypes.Blue, cIndex, b);
            }
            if (!ImageDataHolder.UniformMaxAlpha) ImageDataHolder.channeledColors.setPixelColorChannel(ChannelColorTypes.Alpha, cIndex, a);
        }
    }

    private static DataSimplifyingReturn GrayScaleVerification() {
        boolean grayscale = true;
        boolean maxAlpha = true;

        for (int color : ImageDataHolder.rawPixels) {
            if (!grayscale && !maxAlpha) break;

            if (maxAlpha && ((color >> 24) & 0xFF) != 255) maxAlpha = false;

            if (grayscale) {
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (r != g || g != b) {
                    grayscale = false;
                }
            }
        }

        return new DataSimplifyingReturn(grayscale,maxAlpha);
    }
}