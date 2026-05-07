package com.maxgamertyper1;

import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.ChannelConcurrencyPercents;
import com.maxgamertyper1.datatypes.records.NeighborScanReturn;
import com.maxgamertyper1.datatypes.records.ShapeGrouperReturn;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

import static com.maxgamertyper1.TestHelper.*;
import static com.maxgamertyper1.TestHelper.getUniformColorChannel;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkTest {
    @Test
    @Order(1)
    @DisplayName("Should successfully make a uniform image all 0 in paeth")
    public void PaethEncodingTest() {
        // Testing Setup
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);
        TestingImage.getColorChannels()[0] = getUniformColorChannel(ChannelColorTypes.Gray ,val,10,10);

        ImageDataHolder.channeledColors = TestingImage;


        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        QDC.PaethDeltaEncoder();


        Assertions.assertArrayEquals(
                getUniformColorChannel(ChannelColorTypes.Gray ,0,10,10).getChannelData(),
                QDC.DeltaEncodedImage.getColorChannels()[0].getChannelData(),
                "Paeth Encoding Failure"
        );
    }

    @Test
    @Order(2)
    @DisplayName("100% Continuity for uniform image")
    public void ImageContinuityTest() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);
        TestingImage.getColorChannels()[0] = getUniformColorChannel(ChannelColorTypes.Gray, val,10,10);

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
    @Order(5)
    @DisplayName("Neighbor Scan Test with U and Box")
    public void NeighborScanBlockTest() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);
        int UVA = ThreadLocalRandom.current().nextInt(0, 255);
        int BVA = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);

        ColorChannel GrayTestingColorChannel = getBlockedTestingChannel(ChannelColorTypes.Gray, val, UVA, BVA);

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        NeighborScanReturn NSR = QDC.NeighborScan(GrayTestingColorChannel);

        assertAll("Shape Organization Properties",
                () -> assertArrayEquals(ExpectedDataHolder.ExpectedBlockFlags, NSR.Flags(), "Incorrect Flag Calculation"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedShapeIDS, NSR.Shapes().getRawData(), "Incorrect Shape Grouping"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedEdgeMaps.xBasedMap(),NSR.CardinalMap().xBasedMap(), "Incorrect X-Based Cardinal Map"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedEdgeMaps.yBasedMap(),NSR.CardinalMap().yBasedMap(), "Incorrect Y-Based Cardinal Map")
        );
    }

    @Test
    @Order(6)
    @DisplayName("Neighbor Scan Test Static")
    public void NeighborScanStaticTest() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);
        int SVA = ThreadLocalRandom.current().nextInt(0, 255);
        int BVA = ThreadLocalRandom.current().nextInt(0, 255);

        ImageRepresentative TestingImage = new ImageRepresentative(true,true,false,100);

        ColorChannel GrayTestingColorChannel = getStaticTestingChannel(ChannelColorTypes.Gray, val, SVA, BVA);

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        NeighborScanReturn NSR = QDC.NeighborScan(GrayTestingColorChannel);

        assertAll("Shape Organization Properties",
                () -> assertArrayEquals(ExpectedDataHolder.ExpectedStaticFlags, NSR.Flags(), "Incorrect Flag Calculation"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedShapeIDs, NSR.Shapes().getRawData(), "Incorrect Shape Grouping"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedEdgeMaps.xBasedMap(),NSR.CardinalMap().xBasedMap(), "Incorrect X-Based Cardinal Map"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedEdgeMaps.yBasedMap(),NSR.CardinalMap().yBasedMap(), "Incorrect Y-Based Cardinal Map")
        );
    }

    @Test
    @Order(3)
    @DisplayName("ShapeUnionFind correct perimiter, area, holes, and continuity calculations (non-Static)")
    public void shapeUnionFindTestBlock() {
        TestHelper.DataholderSetter(10,10);


        ShapeUnionFind testingDataShapes = new ShapeUnionFind(ExpectedDataHolder.BlockBasedShapeIDS);
        testingDataShapes.calculateShapeAreas();
        testingDataShapes.calculateShapeContinuity();
        testingDataShapes.calculatePerimiterAndHoles(ExpectedDataHolder.ExpectedBlockFlags);


        int[] AreaCombined = new int[]{
                testingDataShapes.getShapeArea(0),
                testingDataShapes.getShapeArea(11),
                testingDataShapes.getShapeArea(12),
                testingDataShapes.getShapeArea(52)
        };
        int[] ContinuityCombined = new int[]{
                testingDataShapes.getContinuity(0),
                testingDataShapes.getContinuity(11),
                testingDataShapes.getContinuity(12),
                testingDataShapes.getContinuity(52)
        };
        int[] HoleCombined = new int[]{
                testingDataShapes.getHoles(0),
                testingDataShapes.getHoles(11),
                testingDataShapes.getHoles(12),
                testingDataShapes.getHoles(52)
        };

        assertAll(
                () -> assertEquals(4, testingDataShapes.getTotalShapeCount(),"Incorrect Total Shape Number"),
                () -> assertArrayEquals(new int[]{60,18,16,6},AreaCombined,"Incorrect Area Calculations"),
                () -> assertArrayEquals(new int[]{0,0,0,0},HoleCombined,"Calculated Holes when there are none"),
                () -> assertArrayEquals(new int[]{87,17,24,6},ContinuityCombined,"Incorrect Continuity Calculation")
        );
    }


    @Test
    @Order(7)
    @DisplayName("Convert Shapes to Pixel Chunks (non-Static)")
    public void PixelChunkConversionTestBlock() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);
        int UVA = ThreadLocalRandom.current().nextInt(0, 255);
        int BVA = ThreadLocalRandom.current().nextInt(0, 255);


        ShapeUnionFind testingDataShapes = new ShapeUnionFind(ExpectedDataHolder.BlockBasedShapeIDS);
        testingDataShapes.calculateShapeAreas();
        testingDataShapes.calculateShapeContinuity();
        testingDataShapes.calculatePerimiterAndHoles(ExpectedDataHolder.ExpectedBlockFlags);

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        ColorChannel GrayTestingColorChannel = getBlockedTestingChannel(ChannelColorTypes.Gray, val, UVA, BVA);


        ShapeGrouperReturn SGR = QDC.ConvertToPixelChunks(testingDataShapes,GrayTestingColorChannel, ExpectedDataHolder.ExpectedBlockFlags);
        PixelChunk[] chunks = SGR.Chunks();

        Arrays.sort(chunks, Comparator.comparingInt(a -> a.StartingIndex));

        int[][] cornerLocations = new int[chunks.length][];
        for (int i=0; i<chunks.length; i++) {
            PixelChunk PC = chunks[i];
            cornerLocations[i] = new int[PC.cornerPoints.size()];
            for (int j=0; j<PC.cornerPoints.size();j++) {
                cornerLocations[i][j] = PC.cornerPoints.get(j).index();
            }
        }
        // no need to test the shape organizer as its just passed in to read data and is not modified

        assertAll(
                () -> assertEquals(0,SGR.IsolatedIndexes().size(),"Incorrect Isolated Index Calculation"),
                () -> assertEquals(4,chunks.length,"Incorrect Chunk Count"),

                // first Pixel Chunk (shape ID: 0); background
                () -> assertEquals(0,SGR.Chunks()[0].StartingIndex,"Incorrect Shape ID"),
                () -> assertEquals(6,SGR.Chunks()[0].convexCorners,"Incorrect Convex Corner Count"),
                () -> assertArrayEquals(new int[]{0,9,71,76,90,99},cornerLocations[0],"Incorrect Corner Locations"),

                // Second Pixel Chunk (Shape ID: 11); U
                () -> assertEquals(11,SGR.Chunks()[1].StartingIndex,"Incorrect Shape ID (2)"),
                () -> assertEquals(10,SGR.Chunks()[1].convexCorners,"Incorrect Convex Corner Count (2)"),
                () -> assertArrayEquals(new int[]{11,16,61,62,65,66,72,75},cornerLocations[1],"Incorrect Corner Locations (2)"),

                // Third Pixel Chunk (Shape ID: 12); rectangle
                () -> assertEquals(12,SGR.Chunks()[2].StartingIndex,"Incorrect Shape ID (3)"),
                () -> assertEquals(4,SGR.Chunks()[2].convexCorners,"Incorrect Convex Corner Count (3)"),
                () -> assertArrayEquals(new int[]{12,15,42,45},cornerLocations[2],"Incorrect Corner Locations (3)"),

                // Final Pixel Chunk (Shape ID: 52); inner shape
                () -> assertEquals(52,SGR.Chunks()[3].StartingIndex,"Incorrect Shape ID (4)"),
                () -> assertEquals(6,SGR.Chunks()[3].convexCorners,"Incorrect Convex Corner Count (4)"),
                () -> assertArrayEquals(new int[]{52,55,63,64},cornerLocations[3],"Incorrect Corner Locations (4)")
        );
    }

    @Test
    @Order(4)
    @DisplayName("ShapeUnionFind correct perimiter, area, holes, and continuity calculations (Static)")
    public void shapeUnionFindTestStatic() {
        TestHelper.DataholderSetter(10,10);


        ShapeUnionFind testingDataShapes = new ShapeUnionFind(ExpectedDataHolder.StaticBasedShapeIDs);
        testingDataShapes.calculateShapeRoots();
        testingDataShapes.calculateShapeAreas();
        testingDataShapes.calculateShapeContinuity();
        testingDataShapes.calculatePerimiterAndHoles(ExpectedDataHolder.ExpectedStaticFlags);


        int[] AreaCombined = new int[]{
                testingDataShapes.getShapeArea(0),
                testingDataShapes.getShapeArea(6),
                testingDataShapes.getShapeArea(40),
        };
        int[] ContinuityCombined = new int[]{
                testingDataShapes.getContinuity(0),
                testingDataShapes.getContinuity(6),
                testingDataShapes.getContinuity(40)
        };
        int[] HoleCombined = new int[]{
                testingDataShapes.getHoles(0),
                testingDataShapes.getHoles(6),
                testingDataShapes.getHoles(40)
        };

        assertAll(
                () -> assertEquals(7, testingDataShapes.getTotalShapeCount(),"Incorrect Total Shape Number (Including Holes)"),
                () -> assertArrayEquals(new int[]{14,68,14},AreaCombined,"Incorrect Area Calculations"),
                () -> assertArrayEquals(new int[]{0,4,0},HoleCombined,"Calculated Correct Hole Amount"),
                () -> assertArrayEquals(new int[]{19,106,19},ContinuityCombined,"Incorrect Continuity Calculation")
        );
    }

    @Test
    @Order(8)
    @DisplayName("Convert Shapes to Pixel Chunks (Static)")
    public void PixelChunkConversionTestStatic() {
        ImageDataHolder.UniformMaxAlpha = true;
        ImageDataHolder.isGrayscale = true;
        TestHelper.DataholderSetter(10,10);
        int val = ThreadLocalRandom.current().nextInt(0, 255);
        int SVA = ThreadLocalRandom.current().nextInt(0, 255);
        int BVA = ThreadLocalRandom.current().nextInt(0, 255);


        ShapeUnionFind testingDataShapes = new ShapeUnionFind(ExpectedDataHolder.StaticBasedShapeIDs);
        testingDataShapes.calculateShapeRoots();
        testingDataShapes.calculateShapeAreas();
        testingDataShapes.calculateShapeContinuity();
        testingDataShapes.calculatePerimiterAndHoles(ExpectedDataHolder.ExpectedStaticFlags);

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        ColorChannel GrayTestingColorChannel = getStaticTestingChannel(ChannelColorTypes.Gray, val, SVA, BVA);


        ShapeGrouperReturn SGR = QDC.ConvertToPixelChunks(testingDataShapes,GrayTestingColorChannel, ExpectedDataHolder.ExpectedStaticFlags);
        PixelChunk[] chunks = SGR.Chunks();

        Arrays.sort(chunks, Comparator.comparingInt(a -> a.StartingIndex));

        int[][] cornerLocations = new int[chunks.length][];
        for (int i=0; i<chunks.length; i++) {
            PixelChunk PC = chunks[i];
            cornerLocations[i] = new int[PC.cornerPoints.size()];
            for (int j=0; j<PC.cornerPoints.size();j++) {
                cornerLocations[i][j] = PC.cornerPoints.get(j).index();
            }
        }
        // no need to test the shape organizer as its just passed in to read data and is not modified

        assertAll(
                () -> assertEquals(4,SGR.IsolatedIndexes().size(),"Incorrect Isolated Index Calculation"),
                () -> assertEquals(3,chunks.length,"Incorrect Chunk Count"),

                // first Pixel Chunk (shape ID: 0); background
                () -> assertEquals(0,SGR.Chunks()[0].StartingIndex,"Incorrect Shape ID"),
                () -> assertEquals(6,SGR.Chunks()[0].convexCorners,"Incorrect Convex Corner Count"),
                () -> assertArrayEquals(new int[]{0,5,10,15,22,23},cornerLocations[0],"Incorrect Corner Locations"),

                // Second Pixel Chunk (Shape ID: 11); U
                () -> assertEquals(6,SGR.Chunks()[1].StartingIndex,"Incorrect Shape ID (2)"),
                () -> assertEquals(14,SGR.Chunks()[1].convexCorners,"Incorrect Convex Corner Count (2)"),
                () -> assertArrayEquals(new int[]{6,9,20,21,24,30,47,52,56,77,82,86,92,99},cornerLocations[1],"Incorrect Corner Locations (2)"),

                // Third Pixel Chunk (Shape ID: 12); rectangle
                () -> assertEquals(40,SGR.Chunks()[2].StartingIndex,"Incorrect Shape ID (3)"),
                () -> assertEquals(6,SGR.Chunks()[2].convexCorners,"Incorrect Convex Corner Count (3)"),
                () -> assertArrayEquals(new int[]{40,41,62,72,90,91},cornerLocations[2],"Incorrect Corner Locations (3)")
        );
    }


    @Test
    @Order(9)
    @DisplayName("Entire ChunkImage Test")
    public void ChunkImageTest() { // the previous tests are just the parts, this is the whole thing so its assertAll is super long
        TestHelper.DataholderSetter(10,10);

        ImageRepresentative testingImage = new ImageRepresentative(false,true,false,100);
        ColorChannel[] testingData = testingImage.getColorChannels();
        testingData[0] = getBlockedTestingChannel(ChannelColorTypes.Red,10,100,255); // 4 chunks
        testingData[1] = getUniformColorChannel(ChannelColorTypes.Green,10,10,10); // 1 chunk
        testingData[2] = getStaticTestingChannel(ChannelColorTypes.Blue,10,100,255); // 3 chunks

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        ChunkedImageRepresentative outputImage = QDC.chunkImage(testingImage);

        ChunkedColorChannel[] outputChannels = outputImage.getChunkedChannels();

        System.out.println(Arrays.toString(ExpectedDataHolder.getExpectedStaticChunkData(ChannelColorTypes.Blue, 10, 255)));
        System.out.println("2nd array");
        System.out.println(Arrays.toString(outputChannels[2].getChannelChunks()));

        assertAll("Overall Image Data",
                // overall data
                () -> assertEquals(8,outputImage.getChunkCount(), "Incorrect Chunk Counting"),
                () -> assertEquals(4,outputImage.getIndexCount(), "Incorrect Index Counting"),

                // Red Color Channel [block]
                () -> assertEquals(ChannelColorTypes.Red,outputChannels[0].getChannelType(),"Unexpected Color Swap"),
                () -> assertEquals(0,outputChannels[0].getChannelIsolatedIndexes().length,"Unexpected Isolated Indexes"),
                () -> assertEquals(0,outputChannels[0].getChannelIsolatedCorrespondingColor().length,"Unexpected Corresponding Isolated Indexes"),
                () -> assertArrayEquals(ExpectedDataHolder.ExpectedBlockFlags,outputChannels[0].getComputedFlags(),"Unexpected Flags Map"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedEdgeMaps.xBasedMap(),outputChannels[0].getCardinalMaps().xBasedMap(),"Unexpected X-Cardinal Map"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedEdgeMaps.yBasedMap(),outputChannels[0].getCardinalMaps().yBasedMap(),"Unexpected Y-Cardinal Map"),
                () -> assertArrayEquals(ExpectedDataHolder.BlockBasedShapeIDS,outputChannels[0].getShapeOrganizer().getRawData(),"Unexpected Shape Organizer Data"),
                () -> assertEquals(Arrays.toString(ExpectedDataHolder.getExpectedBlockChunkData(ChannelColorTypes.Red, 10, 100, 255)), Arrays.toString(outputChannels[0].getChannelChunks()),"Unexpected Pixel Chunks"), //  I know im technically supposed to make an Equals function, but i dont want to

                // Green Color Channel [Uniform]
                () -> assertEquals(ChannelColorTypes.Green,outputChannels[1].getChannelType(),"Unexpected Color Swap (2)"),
                () -> assertEquals(0,outputChannels[1].getChannelIsolatedIndexes().length,"Unexpected Isolated Indexes (2)"),
                () -> assertEquals(0,outputChannels[1].getChannelIsolatedCorrespondingColor().length,"Unexpected Corresponding Isolated Indexes (2)"),
                () -> assertArrayEquals(ExpectedDataHolder.ExpectedUniformFlags,outputChannels[1].getComputedFlags(),"Unexpected Flags Map (2)"),
                () -> assertArrayEquals(ExpectedDataHolder.UniformBasedEdgeMaps.xBasedMap(),outputChannels[1].getCardinalMaps().xBasedMap(),"Unexpected X-Cardinal Map (2)"),
                () -> assertArrayEquals(ExpectedDataHolder.UniformBasedEdgeMaps.yBasedMap(),outputChannels[1].getCardinalMaps().yBasedMap(),"Unexpected Y-Cardinal Map (2)"),
                () -> assertArrayEquals(ExpectedDataHolder.getExpectedUniformShapeIDs(10,10),outputChannels[1].getShapeOrganizer().getRawData(),"Unexpected Shape Organizer Data (2)"),
                () -> assertEquals(Arrays.toString(ExpectedDataHolder.getExpectedUniformChunkData(ChannelColorTypes.Green, 10, 10, 10)), Arrays.toString(outputChannels[1].getChannelChunks()),"Unexpected Pixel Chunks (2)"),

                // Blue Color Channel [Static (not really but idk another name)]
                () -> assertEquals(ChannelColorTypes.Blue,outputChannels[2].getChannelType(),"Unexpected Color Swap (3)"),
                () -> assertEquals(4,outputChannels[2].getChannelIsolatedIndexes().length,"Unexpected Isolated Indexes Length (3)"),
                () -> assertEquals(4,outputChannels[2].getChannelIsolatedCorrespondingColor().length,"Unexpected Corresponding Isolated Indexes Length (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedIsolatedIndexes,outputChannels[2].getChannelIsolatedIndexes(),"Unexpected Isolated Indexes (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedCorrespondingIsolatedIndexes(255),outputChannels[2].getChannelIsolatedCorrespondingColor(),"Unexpected Corresponding Isolated Indexes (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.ExpectedStaticFlags,outputChannels[2].getComputedFlags(),"Unexpected Flags Map (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedEdgeMaps.xBasedMap(),outputChannels[2].getCardinalMaps().xBasedMap(),"Unexpected X-Cardinal Map (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedEdgeMaps.yBasedMap(),outputChannels[2].getCardinalMaps().yBasedMap(),"Unexpected Y-Cardinal Map (3)"),
                () -> assertArrayEquals(ExpectedDataHolder.StaticBasedShapeIDs,outputChannels[2].getShapeOrganizer().getRawData(),"Unexpected Shape Organizer Data (3)"),
                () -> assertEquals(Arrays.toString(ExpectedDataHolder.getExpectedStaticChunkData(ChannelColorTypes.Blue, 10, 255)), Arrays.toString(outputChannels[2].getChannelChunks()),"Unexpected Pixel Chunks (3)")
        );
    }
}
// I know i could modularize all of this and i will later