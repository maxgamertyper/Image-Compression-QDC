package com.maxgamertyper1;

import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.ColorChannel;
import com.maxgamertyper1.datatypes.ImageRepresentative;
import com.maxgamertyper1.datatypes.records.ChannelConcurrencyPercents;
import com.maxgamertyper1.datatypes.records.NeighborScanReturn;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.maxgamertyper1.TestHelper.getUniformGrayColorChannel;
import static org.junit.jupiter.api.Assertions.*;

public class QDCTester {
    @Test
    @DisplayName("Should successfully make a uniform image all 0 in paeth")
    public void PaethEncodingTest() {
        // Testing Setup
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        ImageDataHolder.flatsize = 100;
        ImageDataHolder.width = 10;
        ImageDataHolder.height = 10;
        int val = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);
        TestingImage.getColorChannels()[0] = getUniformGrayColorChannel(val,10,10);

        ImageDataHolder.channeledColors = TestingImage;


        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        QDC.PaethDeltaEncoder();


        Assertions.assertArrayEquals(
                getUniformGrayColorChannel(0,10,10).getChannelData(),
                QDC.DeltaEncodedImage.getColorChannels()[0].getChannelData(),
                "Paeth Encoding Failure"
        );
    }

    @Test
    @DisplayName("100% Continuity for uniform image")
    public void ImageContinuityTest() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        ImageDataHolder.flatsize = 100;
        ImageDataHolder.width = 10;
        ImageDataHolder.height = 10;
        int val = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);
        TestingImage.getColorChannels()[0] = getUniformGrayColorChannel(val,10,10);

        ImageDataHolder.channeledColors = TestingImage;

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        QDC.PaethDeltaEncoder();


        ChannelConcurrencyPercents CCP = QDC.getChannelConncurrencyPercents();

        assertAll("Concurrency Properties",
                () -> assertEquals(1, CCP.colorChunkRate(), "Incorrect Color Chunk Calculation"),
                () -> assertEquals(1, CCP.deltaChunkRate(), "Incorrect Paeth Chunk Calculation")
        );
    }

    @Test
    @DisplayName("Neighbor Scan Test with U and Box")
    public void NeighborScanTest() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        ImageDataHolder.flatsize = 100;
        ImageDataHolder.width = 10;
        ImageDataHolder.height = 10;
        int val = ThreadLocalRandom.current().nextInt(0, 255);
        int UValue = ThreadLocalRandom.current().nextInt(0, 255);
        int BoxValue = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);

        ColorChannel GrayTestingColorChannel = new ColorChannel(ChannelColorTypes.Gray,100);
        // there should be a 4x4 box inside of a U-Shape
        GrayTestingColorChannel.setChannelData(new int[]{
                val,val,val,val,val,val,val,val,val,val,
                val,UValue,BoxValue,BoxValue,BoxValue,BoxValue,UValue,val,val,val,
                val,UValue,BoxValue,BoxValue,BoxValue,BoxValue,UValue,val,val,val,
                val,UValue,BoxValue,BoxValue,BoxValue,BoxValue,UValue,val,val,val,
                val,UValue,BoxValue,BoxValue,BoxValue,BoxValue,UValue,val,val,val,
                val,UValue,val,val,val,val,UValue,val,val,val,
                val,UValue,UValue,val,val,UValue,UValue,val,val,val,
                val,val,UValue,UValue,UValue,UValue,val,val,val,val,
                val,val,val,val,val,val,val,val,val,val,
                val,val,val,val,val,val,val,val,val,val
        });

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        NeighborScanReturn NSR = QDC.NeighborScan(GrayTestingColorChannel);

        // first bit is left 1
        // second bit is right 2
        // third bit is down 4
        // fourth bit is up 8

        int[] actualFlags = new int[]{
                9,12,12,12,12,12,12,8,8,10,
                3,11,9,8,8,10,11,1,0,2,
                3,3,1,0,0,2,3,1,0,2,
                3,3,1,0,0,2,3,1,0,2,
                3,3,5,4,4,6,3,1,0,2,
                3,3,13,8,8,14,3,1,0,2,
                3,5,10,5,6,9,6,1,0,2,
                1,10,5,12,12,6,9,0,0,2,
                1,0,8,8,8,8,0,0,0,2,
                5,4,4,4,4,4,4,4,4,6
        };
        int[] rawShapeParents = new int[]{
                0,0,0,0,0,0,0,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,12,12,12,12,11,0,0,0,
                0,11,52,52,52,52,11,0,0,0,
                0,11,11,52,52,11,11,0,0,0,
                0,0,11,11,11,11,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0
        };
        int[][] rawYBasedMap = new int[][]{
                new int[]{0,9},
                new int[]{0,1,2,5,6,7,9},
                new int[]{0,1,2,5,6,7,9},
                new int[]{0,1,2,5,6,7,9},
                new int[]{0,1,2,5,6,7,9},
                new int[]{0,1,2,5,6,7,9},
                new int[]{0,1,2,3,4,5,6,7,9},
                new int[]{0,1,2,5,6,9},
                new int[]{0,9},
                new int[]{0,9},
        };
        int[][] rawXBasedMap = new int[][]{
                new int[]{0,9},
                new int[]{0,1,6,7,9},
                new int[]{0,1,4,5,6,7,8,9},
                new int[]{0,1,4,5,6,7,8,9},
                new int[]{0,1,4,5,6,7,8,9},
                new int[]{0,1,4,5,6,7,8,9},
                new int[]{0,1,6,7,9},
                new int[]{0,9},
                new int[]{0,9},
                new int[]{0,9},
        };

        assertAll("Shape Organization Properties",
                () -> assertArrayEquals(actualFlags, NSR.Flags(), "Incorrect Flag Calculation"),
                () -> assertArrayEquals(rawShapeParents, NSR.Shapes().getRawData(), "Incorrect Shape Grouping"),
                () -> assertArrayEquals(rawXBasedMap,NSR.CardinalMap().xBasedMap(), "Incorrect X-Based Cardinal Map"),
                () -> assertArrayEquals(rawYBasedMap,NSR.CardinalMap().yBasedMap(), "Incorrect Y-Based Cardinal Map")
        );
    }
}