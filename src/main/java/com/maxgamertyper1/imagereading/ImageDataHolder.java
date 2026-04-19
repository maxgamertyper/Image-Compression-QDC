package com.maxgamertyper1.imagereading;

import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.ColorChannel;
import com.maxgamertyper1.datatypes.ImageRepresentative;

public class ImageDataHolder {
    public static boolean isGrayscale = false;
    public static int[] rawPixels;
    public static int height;
    public static int width;
    public static int flatsize;
    public static ImageRepresentative channeledColors;
    public static String metaImageName;
    public static OriginalFileType fileType;
    public static boolean UniformMaxAlpha;
}