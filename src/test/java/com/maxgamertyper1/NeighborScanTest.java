package com.maxgamertyper1;

import com.maxgamertyper1.compressor.FinalizedCompressorData;
import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.ImageReader;
import com.maxgamertyper1.imagereading.PixelHelper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class NeighborScanTest {

    private void generateEdgeColoredBlockChannels(ChunkedImageRepresentative chunkedImageRepresentative, int width, int height, String outputFileName) throws IOException {
        Random rand = new Random(42);

        for (ChunkedColorChannel CCC : chunkedImageRepresentative.getChunkedChannels()) {
            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int IsoIndex : CCC.getChannelIsolatedIndexes()) {
                int r = rand.nextInt(200) + 55;
                int g = rand.nextInt(200) + 55;
                int b = rand.nextInt(200) + 55;
                int color = (255 << 24) | (r << 16) | (g << 8) | b;

                int[] location = PixelHelper.ReverseIndexCalculator(width,IsoIndex);

                outputImage.setRGB(location[0],location[1],color);
            }

            for (PixelChunk PC: CCC.getChannelChunks()) {
                int r = rand.nextInt(200) + 55; // 55-255 range (prevents too dark)
                int g = rand.nextInt(200) + 55;
                int b = rand.nextInt(200) + 55;

                int color = (255 << 24) | (r << 16) | (g << 8) | b;

                for (CornerPoint CP : PC.cornerPoints) {
                    int[] location = PixelHelper.ReverseIndexCalculator(width,CP.index());

                    outputImage.setRGB(location[0],location[1],color);
                }
            }

            File outputFile = new File("TestingImages/DebugOutputs/"+CCC.getChannelType()+"_debug_output.png");
            ImageIO.write(outputImage, "png", outputFile);
        }
    }

    @Test
    public void BlockedColorTestVisual() throws Exception {
        long start = System.currentTimeMillis();
        long end;

        ImageReader.ReadAllImageContent("TestingImages/testingimage.jpg");
        System.out.println("Image Data: \n" +"Name: "+ ImageDataHolder.metaImageName +"\nWidth: "+ImageDataHolder.width + "\nHeight: " + ImageDataHolder.height +"\nExtension: "+ImageDataHolder.fileType +"\nGrayscale:"+ImageDataHolder.isGrayscale + "\nUniform Maximum Alpha:" + ImageDataHolder.UniformMaxAlpha + "\nUsing Version: "+ FinalizedCompressorData.compressionVersionMajor+"."+FinalizedCompressorData.compressionVersionMinor);

        end = System.currentTimeMillis();
        System.out.println("IO processing took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();

        System.out.println("Skipping Paeth Encoding and Prediction Processing since this is the BlockedColorTestVisual");

        ChunkedImageRepresentative chunkedImage = QDC.chunkImage(ImageDataHolder.channeledColors);

        end = System.currentTimeMillis();
        System.out.println("Flag Processing and Shaping (concurrent) took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        for (ChunkedColorChannel channel : chunkedImage.getChunkedChannels()) {
            System.out.println("Color type: "+channel.getChannelType());
            System.out.println("-------------------------------Chunks: ");
            System.out.println(Arrays.toString(channel.getChannelChunks()));

            System.out.println("----------------------------------Isolated Indexes: ");
            System.out.println(Arrays.toString(channel.getChannelIsolatedIndexes()));
        }

        generateEdgeColoredBlockChannels(chunkedImage,ImageDataHolder.width,ImageDataHolder.height,ImageDataHolder.metaImageName);
    }
}
