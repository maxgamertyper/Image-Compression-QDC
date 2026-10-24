package com.maxgamertyper1;

import com.maxgamertyper1.CodecIO.Assembly.Disassembler;
import com.maxgamertyper1.CodecIO.Encoding.EncodingManager;
import com.maxgamertyper1.CodecIO.Encoding.FormattedWriter;
import com.maxgamertyper1.compressor.*;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.ImageReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        String inFileName = "testing.qdc";
        String encodeFileName = "TestingImages/cross.png";
        String outFileName = "newTesting.qdca";

        //encode(encodeFileName);
        testImageReaderRefactor(encodeFileName, outFileName);
        disassemble(inFileName,outFileName);
    }

    public static void testImageReaderRefactor(String encodeFileName, String outFileName) throws IOException {
        File imageFile = new File(encodeFileName);

        ImageReader reader = new ImageReader(imageFile);
        ImageRepresentative image = reader.readImageFile();

        ImageMeta metaData = image.getMetaData();

        System.out.println("Grayscale: " + metaData.grayscale());
        System.out.println("Opaque: " + metaData.opaque());
        System.out.println("Height: " + metaData.height());
        System.out.println("Width: " + metaData.width());
        System.out.println("Flatsize: " + metaData.flatsize());
        System.out.println("originalName: " + metaData.metaImageName());
        System.out.println("originalType: " + metaData.fileType());

        System.out.println("Raw Pixel Output: ");
        for (ColorChannel channel : image.getColorChannels()) {
            System.out.println("Channel: " + channel.getChannelType());
            System.out.println("Pixels: ");
            System.out.println(Arrays.toString(channel.getChannelData()));
        }

        //NOTE: will need to adjust the ImageRep instantiation

        PaethProcessor paethProccessor = new PaethProcessor(image);
        ImageRepresentative paethImage = paethProccessor.paethEncode();

        System.out.println("Paeth Encoding Output: ");
        for (ColorChannel channel : paethImage.getColorChannels()) {
            System.out.println("Channel: " + channel.getChannelType());
            System.out.println("Initial Value: ");
            System.out.println(paethImage.getInitialPixels()[paethImage.getChannelIndex(channel.getChannelType())]);
            System.out.println("Paeth Values: ");
            System.out.println(Arrays.toString(channel.getChannelData()));
        }
        //NOTE: had to adjust initial pixel length since it needs to be set before, related to above note

        ChunkProcessor chunkProcessor = new ChunkProcessor(paethImage);
        ChunkedImageRepresentative chunkedImage = chunkProcessor.chunkImage();
        chunkedImage.sequentiallyOrder();

        for (ChunkedColorChannel channel : chunkedImage.getChunkedChannels()) {
            System.out.println("Channel: " + channel.getChannelType());
            System.out.println("Pixel Chunks: ");
            System.out.println(Arrays.toString(channel.getChannelChunks()));
            System.out.println("Isolated Pixels: ");
            System.out.println(Arrays.toString(channel.getChannelIsolatedIndexes()));
            System.out.println("Corresponding Colors: ");
            System.out.println(Arrays.toString(channel.getChannelIsolatedCorrespondingColor()));
        }

        //NOTE: compression still needs to be refactored, still bad, just stringed together in a seperate class to use meta and some clarity like EdgeFlags

        SpatialCompressor compressor = new SpatialCompressor(chunkedImage, CompressionConfig.DEFAULT);
        CompressedImageRepresentative compressedImage = compressor.QuadralizedDataCompressed();
        compressedImage.sortChannelOrder();

        System.out.println("Compressed Image:\n\n\n\n");

        for (CompressedColorChannel channel : compressedImage.getCompressedChannels()) {
            System.out.println("Channel: " + channel.getChannelColorType());
            System.out.println("Rectangles: ");
            System.out.println(Arrays.toString(channel.getBuiltRectangles()));
            System.out.println("Isolated Pixels: ");
            System.out.println(Arrays.toString(channel.getIsolatedIndexes()));
            System.out.println("Corresponding Colors: ");
            System.out.println(Arrays.toString(channel.getCorrespondingIsolatedValues()));
        }

        EncodingManager encoder = new EncodingManager(compressedImage,outFileName, CompressionConfig.DEFAULT);

        System.out.println(compressedImage.getCompressedChannels().length);
        for (CompressedColorChannel ccc : compressedImage.getCompressedChannels()) {
            System.out.println(ccc.getInitialPixel());
        }

        encoder.writeMetadata();
        for (int i=0; i < compressedImage.getCompressedChannels().length; i++) {
            encoder.processChannel(i);
        }
        encoder.finalizeFile();
    }

    public static void disassemble(String inFileName, String outFileName) throws IOException {
        try (Disassembler ds = new Disassembler(inFileName, outFileName)) {
            ds.Dissassemble();
        }
    }

//    public static void encode(String fileName) throws IOException {
//        System.out.println("PRESS ENTER TO START PROFILING...");
//        System.in.read();
//
//        long start = System.currentTimeMillis();
//        long Truestart = System.currentTimeMillis();
//        long end;
//
//        ImageReader.ReadAllImageContent(fileName);
//        System.out.println("Image Data: \n" +"Name: "+ ImageDataHolder.metaImageName +"\nWidth: "+ImageDataHolder.width + "\nHeight: " + ImageDataHolder.height + "\n Flatsize: " + ImageDataHolder.flatsize + " (Megapixels: "+ (float)ImageDataHolder.flatsize/1000000 +")" + "\nExtension: "+ImageDataHolder.fileType +"\nGrayscale:"+ImageDataHolder.isGrayscale + "\nUniform Maximum Alpha:" + ImageDataHolder.UniformMaxAlpha + "\nUsing Version: "+ FormattedWriter.compressionVersionMajor+"."+FormattedWriter.compressionVersionMinor);
//
//        end = System.currentTimeMillis();
//        long IOprocessingTime = end - start;
//        System.out.println("IO processing took: " + (IOprocessingTime) + "ms");
//        start = System.currentTimeMillis();
//
//        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
//        QDC.PaethDeltaEncoder();
//
//        end = System.currentTimeMillis();
//        System.out.println("Paeth encoding took: " + (end - start) + "ms");
//        start = System.currentTimeMillis();
//
//        ChunkedImageRepresentative chunkedImage;
//        chunkedImage = QDC.chunkImage(QDC.DeltaEncodedImage);
//
//        chunkedImage.sequentiallyOrder();
//
//        end = System.currentTimeMillis();
//        System.out.println("Flag Processing and Shaping (concurrent) took: " + (end - start) + "ms");
//        start = System.currentTimeMillis();
//
//        CompressedImageRepresentative FinalCompression = QDC.QuadralizedDataCompressed(chunkedImage, false);
//        FinalCompression.setMetaData((short) ImageDataHolder.height,(short) ImageDataHolder.width,ImageDataHolder.fileType);
//
//        end = System.currentTimeMillis();
//        System.out.println("Calculating Rectangles and Corners took: " + (end - start) + "ms");
//        start = System.currentTimeMillis();
//
//        for (CompressedColorChannel channel : FinalCompression.getCompressedChannels()) {
//            System.out.println("\n\nChannel (Color: " + channel.getChannelColorType() + ", Paeth? " + !FinalCompression.isPaethEncoded() + ") Breakdown:");
//            System.out.println("\t Rectangles: "+ channel.getRectangleCount() + " Corners: "+channel.getCornerCount()+" Isolated Indexes: "+channel.getIsolatedCount());
//
//            System.out.println("----------- Detailed Breakdown: ");
//            System.out.println("Channel Starting Index: "+channel.getInitialPixel());
//
//            System.out.println("------------------------------------");
//            System.out.println("\t Isolated Indexes: ");
//            System.out.println(Arrays.toString(channel.getIsolatedIndexes()));
//            System.out.println("\t Corresponding Values: ");
//            System.out.println(Arrays.toString(channel.getCorrespondingIsolatedValues()));
//
//            System.out.println("------------------------------------");
//            System.out.println("\t Rectangles: ");
//            System.out.println(Arrays.toString(channel.getBuiltRectangles()));
//
//            System.out.println("------------------------------------");
//            System.out.println("\t Corners: ");
//            System.out.println(Arrays.toString(channel.getBuiltCorners()));
//        }
//
//        EncodingManager tempManager = new EncodingManager(FinalCompression,"testing.qdc", CompressionConfig.OLD);
//        FinalCompression.sortChannelOrder();
//
//        System.out.println(FinalCompression.getCompressedChannels().length);
//        for ( CompressedColorChannel ccc : FinalCompression.getCompressedChannels()) {
//            System.out.println(ccc.getInitialPixel());
//        }
//
//        tempManager.writeMetadata();
//        for (int i=0; i < FinalCompression.getCompressedChannels().length; i++) {
//            tempManager.processChannel(i);
//        }
//        tempManager.finalizeFile();
//
//        end = System.currentTimeMillis();
//        System.out.println("Encoding: " + (end - start) + "ms");
//        start = System.currentTimeMillis();
//
//
//        long entireTime = System.currentTimeMillis() - Truestart;
//
//        System.out.println("Entire thing took: " + (entireTime) + "ms");
//        System.out.println("IO Time (non-encoding): " + (IOprocessingTime) + "ms");
//        System.out.println("Computational Time: " + (entireTime - IOprocessingTime) + "ms");
//        System.out.println("MP thorughput: " + (((float)ImageDataHolder.flatsize/1000000)/((float)(entireTime - IOprocessingTime)/1000)) + "MP/s");
//    }
}