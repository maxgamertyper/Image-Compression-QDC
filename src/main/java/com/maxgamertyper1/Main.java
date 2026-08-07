package com.maxgamertyper1;

import com.maxgamertyper1.CodecIO.Decoding.Disassembler;
import com.maxgamertyper1.CodecIO.Encoding.EncodingManager;
import com.maxgamertyper1.CodecIO.Encoding.FormattedWriter;
import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.ImageReader;

import java.io.IOException;
import java.util.Arrays;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        String inFileName = "testing.qdc";
        String encodeFileName = "TestingImages/cross.png";
        String outFileName = "testing.qdca";

        encode(encodeFileName);
        disassemble(inFileName,outFileName);
    }

    public static void disassemble(String inFileName, String outFileName) throws IOException {
        try (Disassembler ds = new Disassembler(inFileName, outFileName)) {
            ds.Dissassemble();
        }
    }

    public static void encode(String fileName) throws IOException {
        System.out.println("PRESS ENTER TO START PROFILING...");
        System.in.read();

        long start = System.currentTimeMillis();
        long Truestart = System.currentTimeMillis();
        long end;

        ImageReader.ReadAllImageContent(fileName);
        System.out.println("Image Data: \n" +"Name: "+ ImageDataHolder.metaImageName +"\nWidth: "+ImageDataHolder.width + "\nHeight: " + ImageDataHolder.height + "\n Flatsize: " + ImageDataHolder.flatsize + " (Megapixels: "+ (float)ImageDataHolder.flatsize/1000000 +")" + "\nExtension: "+ImageDataHolder.fileType +"\nGrayscale:"+ImageDataHolder.isGrayscale + "\nUniform Maximum Alpha:" + ImageDataHolder.UniformMaxAlpha + "\nUsing Version: "+ FormattedWriter.compressionVersionMajor+"."+FormattedWriter.compressionVersionMinor);

        end = System.currentTimeMillis();
        long IOprocessingTime = end - start;
        System.out.println("IO processing took: " + (IOprocessingTime) + "ms");
        start = System.currentTimeMillis();

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        QDC.PaethDeltaEncoder();

        end = System.currentTimeMillis();
        System.out.println("Paeth encoding took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        ChunkedImageRepresentative chunkedImage;
        chunkedImage = QDC.chunkImage(QDC.DeltaEncodedImage);

        chunkedImage.SequentiallyOrder();

        end = System.currentTimeMillis();
        System.out.println("Flag Processing and Shaping (concurrent) took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        CompressedImageRepresentative FinalCompression = QDC.QuadralizedDataCompressed(chunkedImage, false);
        FinalCompression.setMetaData((short) ImageDataHolder.height,(short) ImageDataHolder.width,ImageDataHolder.fileType);

        end = System.currentTimeMillis();
        System.out.println("Calculating Rectangles and Corners took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        for (CompressedColorChannel channel : FinalCompression.getCompressedChannels()) {
            System.out.println("\n\nChannel (Color: " + channel.getChannelColorType() + ", Paeth? " + !FinalCompression.isPaethEncoded() + ") Breakdown:");
            System.out.println("\t Rectangles: "+ channel.getRectangleCount() + " Corners: "+channel.getCornerCount()+" Isolated Indexes: "+channel.getIsolatedCount());

            System.out.println("----------- Detailed Breakdown: ");
            System.out.println("Channel Starting Index: "+channel.getInitialPixel());

            System.out.println("------------------------------------");
            System.out.println("\t Isolated Indexes: ");
            System.out.println(Arrays.toString(channel.getIsolatedIndexes()));
            System.out.println("\t Corresponding Values: ");
            System.out.println(Arrays.toString(channel.getCorrespondingIsolatedValues()));

            System.out.println("------------------------------------");
            System.out.println("\t Rectangles: ");
            System.out.println(Arrays.toString(channel.getBuiltRectangles()));

            System.out.println("------------------------------------");
            System.out.println("\t Corners: ");
            System.out.println(Arrays.toString(channel.getBuiltCorners()));
        }

        EncodingManager tempManager = new EncodingManager(FinalCompression,"testing.qdc");
        FinalCompression.sortChannelOrder();

        System.out.println(FinalCompression.getCompressedChannels().length);
        for ( CompressedColorChannel ccc : FinalCompression.getCompressedChannels()) {
            System.out.println(ccc.getInitialPixel());
        }

        tempManager.writeMetadata();
        for (int i=0; i < FinalCompression.getCompressedChannels().length; i++) {
            tempManager.processChannel(i);
        }
        tempManager.finalizeFile();

        end = System.currentTimeMillis();
        System.out.println("Encoding: " + (end - start) + "ms");
        start = System.currentTimeMillis();


        long entireTime = System.currentTimeMillis() - Truestart;

        System.out.println("Entire thing took: " + (entireTime) + "ms");
        System.out.println("IO Time (non-encoding): " + (IOprocessingTime) + "ms");
        System.out.println("Computational Time: " + (entireTime - IOprocessingTime) + "ms");
        System.out.println("MP thorughput: " + (((float)ImageDataHolder.flatsize/1000000)/((float)(entireTime - IOprocessingTime)/1000)) + "MP/s");
    }
}