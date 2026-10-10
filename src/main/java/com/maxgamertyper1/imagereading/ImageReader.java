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
    private int imageHeight;
    private int imageWidth;
    private String imageName;
    private OriginalFileType imageType;
    private boolean imageOpaque = true;
    private boolean imageGrayscale = true;
    private final File imageFile2;
    private int[] pixelData;


    public static void ReadAllImageContent(String ImagePath) {
        File imageFile = ImageReader.FillDataHolder(ImagePath);
        int[] rawpixels = ImageReader.GetRGBValues(imageFile);
        DataSimplifyingReturn DSR = GrayScaleVerification();
        ImageDataHolder.isGrayscale = DSR.grayscale();
        ImageDataHolder.UniformMaxAlpha = DSR.uniformAlpha();
        SplitColorArray(ImageDataHolder.flatsize,ImageDataHolder.isGrayscale,rawpixels);
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
        imageName = imageFile2.getName();
        String path = imageFile2.getAbsolutePath();

        int lastIndexOfDot = path.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            System.err.println("No file path found, continuing");
        }

        try {
            imageType = OriginalFileType.valueOf(path.substring(lastIndexOfDot + 1).toUpperCase());
        } catch (IllegalArgumentException e){
            imageType = OriginalFileType.Unknown;
        }
    }

    private  int[] getPixelData() {
        BufferedImage image;

        try {
            image = ImageIO.read(imageFile2);
        } catch (IOException e) {
            System.out.println("Error occured when reading the image file");
            throw new RuntimeException(e);
        }

        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        int[] Colors = new int[imageHeight * imageWidth];

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int colors = image.getRGB(x, y);
                Colors[IndexCalculator(imageWidth, x, y)] = colors;
            }
        }

        return Colors;
    }

    public ImageRepresentative readImageFile() {
        getFileData();

        pixelData = getPixelData();
        imageChannelVerification();

        ImageRepresentative returnHolder = splitColorArray();

        returnHolder.setMetaData(consolidateMetaData());

        return returnHolder;
    }

    public ImageReader(File imageFile) {
        imageFile2 = imageFile;
    }

    private void imageChannelVerification() {
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

    private ImageRepresentative splitColorArray() {
        ImageRepresentative returnHolder = new ImageRepresentative(imageHeight*imageWidth);

        for (int cIndex=0;cIndex< pixelData.length; cIndex++) {
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