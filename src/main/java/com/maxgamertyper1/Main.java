package com.maxgamertyper1;

import com.maxgamertyper1.CodecIO.Assembly.QDCDisassembler;
import com.maxgamertyper1.CodecIO.Decoding.QDCDecoder;
import com.maxgamertyper1.CodecIO.Encoding.QDCEncoder;
import com.maxgamertyper1.compressor.ChunkProcessor;
import com.maxgamertyper1.compressor.CompressionConfig;
import com.maxgamertyper1.compressor.PaethProcessor;
import com.maxgamertyper1.compressor.SpatialCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.ImageReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, DataFormatException {
        String encodeFile = "TestingImages/QDCHelloWorld.png";
        String outEncodeFile = "testing.qdc";
        testImageReaderRefactor(encodeFile, outEncodeFile);

        String inDisassembleFile = "testing.qdc";
        String outDisassembleFile = "testing.qdca";
        disassemble(inDisassembleFile, outDisassembleFile);

        String inDecodingFile = "testing.qdc";
        String outDecodingName = "outputTest";
        decode(inDecodingFile,outDecodingName);
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
            //System.out.println(Arrays.toString(channel.getChannelData()));
        }

        //NOTE: will need to adjust the ImageRep instantiation
        ImageRepresentative paethImage = PaethProcessor.paethEncode(image);

        System.out.println("Paeth Encoding Output: ");
        for (ColorChannel channel : paethImage.getColorChannels()) {
            System.out.println("Channel: " + channel.getChannelType());
            System.out.println("Initial Value: ");
            System.out.println(paethImage.getInitialPixels()[paethImage.getChannelIndex(channel.getChannelType())]);
            System.out.println("Paeth Values: ");
            //System.out.println(Arrays.toString(channel.getChannelData()));
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

        System.out.println("Compressed Image:\n");

        for (CompressedColorChannel channel : compressedImage.getCompressedChannels()) {
            System.out.println("Channel: " + channel.getChannelColorType());
            System.out.println("Rectangles: ");
            System.out.println(Arrays.toString(channel.getBuiltRectangles()));
            System.out.println("Isolated Pixels: ");
            System.out.println(Arrays.toString(channel.getIsolatedIndexes()));
            System.out.println("Corresponding Colors: ");
            System.out.println(Arrays.toString(channel.getCorrespondingIsolatedValues()));
        }

        System.out.println(compressedImage.getCompressedChannels().length);
        for (CompressedColorChannel ccc : compressedImage.getCompressedChannels()) {
            System.out.println(ccc.getInitialPixel());
        }

        QDCEncoder encoder = new QDCEncoder(compressedImage, outFileName, CompressionConfig.DEFAULT);

        encoder.writeMetadata();
        for (int i = 0; i < compressedImage.getCompressedChannels().length; i++) {
            encoder.processChannel(i);
        }
        encoder.finalizeFile();
    }

    public static void disassemble(String inFileName, String outFileName) throws IOException {
        try (QDCDisassembler ds = new QDCDisassembler(inFileName, outFileName)) {
            ds.dissassemble();
        }
    }

    public static void decode(String inFileName, String outFileName) throws IOException, DataFormatException {
        try (QDCDecoder dc = new QDCDecoder(inFileName, outFileName)) {
            dc.decode();
        }
    }
}