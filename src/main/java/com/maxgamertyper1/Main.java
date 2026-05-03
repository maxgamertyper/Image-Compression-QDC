package com.maxgamertyper1;

import com.maxgamertyper1.compressor.FinalizedCompressorData;
import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.ChannelConcurrencyPercents;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.ImageReader;

import java.util.Arrays;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        long end;

        ImageReader.ReadAllImageContent("TestingImages/blankimage2.png");
        System.out.println("Image Data: \n" +"Name: "+ ImageDataHolder.metaImageName +"\nWidth: "+ImageDataHolder.width + "\nHeight: " + ImageDataHolder.height +"\nExtension: "+ImageDataHolder.fileType +"\nGrayscale:"+ImageDataHolder.isGrayscale + "\nUniform Maximum Alpha:" + ImageDataHolder.UniformMaxAlpha + "\nUsing Version: "+ FinalizedCompressorData.compressionVersionMajor+"."+FinalizedCompressorData.compressionVersionMinor);

        end = System.currentTimeMillis();
        System.out.println("IO processing took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        QDC.PaethDeltaEncoder();

        end = System.currentTimeMillis();
        System.out.println("Paeth encoding took: " + (end - start) + "ms");
        start = System.currentTimeMillis();


        ChannelConcurrencyPercents dataPercents = QDC.getChannelConncurrencyPercents(); // IMPORTANT FOR LATER

        System.out.println("Delta noise percent: "+(1-dataPercents.deltaChunkRate())*100);
        System.out.println("Color noise percent: "+(1-dataPercents.colorChunkRate())*100);

        boolean useColoredBFSPrediction = dataPercents.colorChunkRate() >= dataPercents.deltaChunkRate();
        FinalizedCompressorData.usesColorCompression = useColoredBFSPrediction;

        end = System.currentTimeMillis();
        System.out.println("Prediction processing took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        ChunkedImageRepresentative chunkedImage;
        if (useColoredBFSPrediction) {
            chunkedImage = QDC.chunkImage(ImageDataHolder.channeledColors);
        } else {
            chunkedImage = QDC.chunkImage(QDC.DeltaEncodedImage);
        }

        end = System.currentTimeMillis();
        System.out.println("Flag Processing and Shaping (concurrent) took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        QDC.QuadralizedDataCompressed(chunkedImage);

        end = System.currentTimeMillis();
        System.out.println("Calculating Rectangles and Corners took: " + (end - start) + "ms");
        start = System.currentTimeMillis();

        chunkedImage.SequentiallyOrder();

        end = System.currentTimeMillis();
        System.out.println("Sorting took: " + (end - start) + "ms");

        System.out.println("Colored Chunking? "+useColoredBFSPrediction+";\nChunked:\n\t Actual Chunks: "+chunkedImage.getChunkCount()+"; Isolated Indexes: "+chunkedImage.getIndexCount());

        for (ChunkedColorChannel channel : chunkedImage.getChunkedChannels()) {
            System.out.println("Color type: "+channel.getChannelType());
            System.out.println("-------------------------------Chunks: ");
            System.out.println(Arrays.toString(channel.getChannelChunks()));

            System.out.println("----------------------------------Isolated Indexes: ");
            System.out.println(Arrays.toString(channel.getChannelIsolatedIndexes()));
        }
    }
}