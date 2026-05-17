package com.maxgamertyper1;

import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.PixelChunk;
import com.maxgamertyper1.datatypes.ShapeUnionFind;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.CornerPoint;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import static com.maxgamertyper1.TestHelper.emptyBitSet;
import static org.junit.jupiter.api.Assertions.*;

public class CompressionTest {
    @Test
    @Order(1)
    @DisplayName("Corner Heuristic Test")
    public void cornerRectangleHeuristic() {
        ShapeUnionFind squareSUF = new ShapeUnionFind(ExpectedDataHolder.StaticBasedShapeIDs);
        ShapeUnionFind holeSquareSUF = new ShapeUnionFind(ExpectedDataHolder.holeSquareParentIDs);
        ShapeUnionFind swissCheeseSUF = new ShapeUnionFind(ExpectedDataHolder.swissCheeseParentIDs);

        TestHelper.DataholderSetter(10,10);
        squareSUF.calculateShapeContinuity();
        squareSUF.calculatePerimiterAndHoles(ExpectedDataHolder.ExpectedUniformFlags);

        holeSquareSUF.calculateShapeContinuity();
        holeSquareSUF.calculatePerimiterAndHoles(ExpectedDataHolder.expectedHoleSquareFlags);

        TestHelper.DataholderSetter(5,5);
        swissCheeseSUF.calculateShapeContinuity();
        swissCheeseSUF.calculatePerimiterAndHoles(ExpectedDataHolder.expectedSwissCheeseFlags);

        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();

        assertAll(
                () -> assertTrue(QDC.PixelChunkUseRectangles(ExpectedDataHolder.fullSquare.convexCorners,squareSUF.getContinuity(0),squareSUF.getHoles(0)),"Calculated Square as Corners"),
                () -> assertFalse(QDC.PixelChunkUseRectangles(ExpectedDataHolder.holeSquare.convexCorners,holeSquareSUF.getContinuity(0),holeSquareSUF.getHoles(0)),"Calculated Single Hole as Rect"),
                () -> assertFalse(QDC.PixelChunkUseRectangles(ExpectedDataHolder.swissCheese.convexCorners,swissCheeseSUF.getContinuity(0),swissCheeseSUF.getHoles(0)), "Calculated Swiss as Rect")
        );
    }

    @Test
    @Order(2)
    @DisplayName("Corner Data Test")
    public void cornerRepresentativeCalculation() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();

        CornerDataRepresentative fullSquareCDR = new CornerDataRepresentative(
                new int[]{9,81,9},
                0,
                new int[]{9,10,5,6},
                10
        );
        CornerDataRepresentative holeSquareCDR = new CornerDataRepresentative(
                new int[]{9,81,9},
                0,
                new int[]{9,10,5,6},
                10
        ); // this is technically ambiguous with the first one, but you have to trust me with the decoding; corner's are handled differently
        CornerDataRepresentative swissCheeseCDR = new CornerDataRepresentative(
                new int[]{4,3,9,4,4},
                0,
                new int[]{9,10,7,10,6,6},
                10
        );

        CornerDataRepresentative fullSquareCalculated = QDC.CalculateCornerData(ExpectedDataHolder.fullSquare, ExpectedDataHolder.ExpectedUniformFlags);
        CornerDataRepresentative holeSquareCalculated = QDC.CalculateCornerData(ExpectedDataHolder.holeSquare, ExpectedDataHolder.expectedHoleSquareFlags);
        CornerDataRepresentative swissCheeseCalculated = QDC.CalculateCornerData(ExpectedDataHolder.swissCheese, ExpectedDataHolder.expectedSwissCheeseFlags);


        assertAll(
                //full square
                () -> assertEquals(fullSquareCDR.startingIndex(), fullSquareCalculated.startingIndex(),"Incorrect Starting Index"),
                () -> assertEquals(fullSquareCDR.dataValue(), fullSquareCalculated.dataValue(),"Incorrect Data Value"),
                () -> assertArrayEquals(fullSquareCDR.cornerFlags(), fullSquareCalculated.cornerFlags(),"Incorrect Corner Flags"),
                () -> assertArrayEquals(fullSquareCDR.convexCornerIndexDeltas(), fullSquareCalculated.convexCornerIndexDeltas(),"Incorrect Corner Index Deltas"),

                //hole square
                () -> assertEquals(holeSquareCDR.startingIndex(),holeSquareCalculated.startingIndex(),"Incorrect Starting Index (1)"),
                () -> assertEquals(holeSquareCDR.dataValue(),holeSquareCalculated.dataValue(),"Incorrect Data Value (1)"),
                () -> assertArrayEquals(holeSquareCDR.cornerFlags(),holeSquareCalculated.cornerFlags(),"Incorrect Corner Flags (1)"),
                () -> assertArrayEquals(holeSquareCDR.convexCornerIndexDeltas(),holeSquareCalculated.convexCornerIndexDeltas(),"Incorrect Corner Index Deltas (1)"),

                //swiss cheese
                () -> assertEquals(swissCheeseCDR.startingIndex(), swissCheeseCalculated.startingIndex(),"Incorrect Starting Index (2)"),
                () -> assertEquals(swissCheeseCDR.dataValue(), swissCheeseCalculated.dataValue(),"Incorrect Data Value (2)"),
                () -> assertArrayEquals(swissCheeseCDR.cornerFlags(), swissCheeseCalculated.cornerFlags(),"Incorrect Corner Flags (2)"),
                () -> assertArrayEquals(swissCheeseCDR.convexCornerIndexDeltas(),swissCheeseCalculated.convexCornerIndexDeltas(),"Incorrect Corner Index Deltas (2)")
        );
    }

    @Test
    @Order(3)
    @DisplayName("Uniform Limit Grabbing Test")
    public void uniformLimitCalculationTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        assertAll(
                // checking top-left to bottom-right diagonal for uniform as its a good base
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(~9,0,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{8,8},QDC.CalculateLimits(~9,11,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{7,7},QDC.CalculateLimits(~9,22,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{6,6},QDC.CalculateLimits(~9,33,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{5,5},QDC.CalculateLimits(~9,44,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Left diagonal Limit for Uniform data"),

                // bottom-right to top-left
                () -> assertArrayEquals(new int[]{0,0},QDC.CalculateLimits(9,0,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{1,1},QDC.CalculateLimits(9,11,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{2,2},QDC.CalculateLimits(9,22,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{3,3},QDC.CalculateLimits(9,33,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{4,4},QDC.CalculateLimits(9,44,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Right diagonal Limit for Uniform data"),

                // top-right to bottom-left diagonal
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(~10,9,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{8,8},QDC.CalculateLimits(~10,18,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{7,7},QDC.CalculateLimits(~10,27,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{6,6},QDC.CalculateLimits(~10,36,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Right diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{5,5},QDC.CalculateLimits(~10,45,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect top-Right diagonal Limit for Uniform data"),

                // bottom-left to top-right
                () -> assertArrayEquals(new int[]{0,0},QDC.CalculateLimits(10,9,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{1,1},QDC.CalculateLimits(10,18,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{2,2},QDC.CalculateLimits(10,27,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{3,3},QDC.CalculateLimits(10,36,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Left diagonal Limit for Uniform data"),
                () -> assertArrayEquals(new int[]{4,4},QDC.CalculateLimits(10,45,ExpectedDataHolder.UniformBasedEdgeMaps),"Incorrect bottom-Left diagonal Limit for Uniform data")
        );
    }

    @Test
    @Order(4)
    @DisplayName("Block Limit Grabbing Test")
    public void blockLimitCalculationTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        EdgeMapHolder EMH = ExpectedDataHolder.BlockBasedEdgeMaps;

        assertAll(
                //Double Directional Flags
                // checking top-left to bottom-right diagonal for uniform as its a good base
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(~9,0,EMH),"Incorrect top-Left diagonal Limit for Block  1"), // shape ID 0
                () -> assertArrayEquals(new int[]{0,5},QDC.CalculateLimits(~9,11,EMH),"Incorrect top-Left diagonal Limit for Block data 2"), // shape ID 11
                () -> assertArrayEquals(new int[]{3,2},QDC.CalculateLimits(~9,22,EMH),"Incorrect top-Left diagonal Limit for Block data 3"), // shape ID 12
                () -> assertArrayEquals(new int[]{2,1},QDC.CalculateLimits(~9,33,EMH),"Incorrect top-Left diagonal Limit for Block data 4"), // shape ID 12
                () -> assertArrayEquals(new int[]{1,0},QDC.CalculateLimits(~9,44,EMH),"Incorrect top-Left diagonal Limit for Block data 5"), // shape ID 12

                // checking top-right to bottom-left
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(~10,9,EMH),"Incorrect top-Right diagonal Limit for Block data 1"), // shape ID 0
                () -> assertArrayEquals(new int[]{1,8},QDC.CalculateLimits(~10,18,EMH),"Incorrect top-Right diagonal Limit for Block data 2"), // shape ID 0
                () -> assertArrayEquals(new int[]{0,7},QDC.CalculateLimits(~10,27,EMH),"Incorrect top-Right diagonal Limit for Block data 3"), // shape ID 0
                () -> assertArrayEquals(new int[]{0,3},QDC.CalculateLimits(~10,36,EMH),"Incorrect top-Right diagonal Limit for Block data 4"), // shape ID 11
                () -> assertArrayEquals(new int[]{3,0},QDC.CalculateLimits(~10,45,EMH),"Incorrect top-Right diagonal Limit for Block data 5"), // shape ID 12

                // checking bottom-right to top-left
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(9,99,EMH),"Incorrect bottom-Right diagonal Limit for Block data 1"),
                () -> assertArrayEquals(new int[]{8,8},QDC.CalculateLimits(9,88,EMH),"Incorrect bottom-Right diagonal Limit for Block data 2"),
                () -> assertArrayEquals(new int[]{1,7},QDC.CalculateLimits(9,77,EMH),"Incorrect bottom-Right diagonal Limit for Block data 3"),
                () -> assertArrayEquals(new int[]{1,5},QDC.CalculateLimits(9,66,EMH),"Incorrect bottom-Right diagonal Limit for Block data 4"),
                () -> assertArrayEquals(new int[]{3,0},QDC.CalculateLimits(9,55,EMH),"Incorrect bottom-Right diagonal Limit for Block data 5"),

                // checking bottom-left to top-right
                () -> assertArrayEquals(new int[]{9,9},QDC.CalculateLimits(10,90,EMH),"Incorrect bottom-Left diagonal Limit for Block data 1"), // shape ID 0
                () -> assertArrayEquals(new int[]{8,1},QDC.CalculateLimits(10,81,EMH),"Incorrect bottom-Left diagonal Limit for Block data 2"), // shape ID 0
                () -> assertArrayEquals(new int[]{3,1},QDC.CalculateLimits(10,72,EMH),"Incorrect bottom-Left diagonal Limit for Block data 3"), // shape ID 11
                () -> assertArrayEquals(new int[]{1,1},QDC.CalculateLimits(10,63,EMH),"Incorrect bottom-Left diagonal Limit for Block data 4"), // shape ID 52
                () -> assertArrayEquals(new int[]{1,0},QDC.CalculateLimits(10,54,EMH),"Incorrect bottom-Left diagonal Limit for Block data 5"),

                //Single Directional Flags
                () -> assertArrayEquals(new int[]{0,5},QDC.CalculateLimits(4,11,EMH),"Incorrect down-only limit"), // down only
                () -> assertArrayEquals(new int[]{0,5},QDC.CalculateLimits(8,66,EMH),"Incorrect up-only limit"), // up only
                () -> assertArrayEquals(new int[]{2,0},QDC.CalculateLimits(1,74,EMH),"Incorrect left-only limit"), // left only
                () -> assertArrayEquals(new int[]{2,0},QDC.CalculateLimits(2,73,EMH),"Incorrect right-only limit") // right only
        );
    }

    @Test
    @Order(5)
    @DisplayName("Static Limit Grabbing Test")
    public void staticLimitCalculationTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        EdgeMapHolder EMH = ExpectedDataHolder.StaticBasedEdgeMaps;

        assertAll(
                //Double Directional Flags
                // checking top-left to bottom-right diagonal for uniform as its a good base
                () -> assertArrayEquals(new int[]{5,1},QDC.CalculateLimits(~9,0,EMH),"Incorrect top-Left diagonal Limit for Static data 1"), // shape ID 0
                () -> assertArrayEquals(new int[]{4,0},QDC.CalculateLimits(~9,11,EMH),"Incorrect top-Left diagonal Limit for Static data 2"), // shape ID 11
                () -> assertArrayEquals(new int[]{1,0},QDC.CalculateLimits(~9,22,EMH),"Incorrect top-Left diagonal Limit for Static data 3"), // shape ID 12
                () -> assertArrayEquals(new int[]{6,6},QDC.CalculateLimits(~9,33,EMH),"Incorrect top-Left diagonal Limit for Static data 4"), // shape ID 12
                () -> assertArrayEquals(new int[]{1,5},QDC.CalculateLimits(~9,44,EMH),"Incorrect top-Left diagonal Limit for Static data 5"), // shape ID 12

                // checking top-right to bottom-left
                () -> assertArrayEquals(new int[]{3,9},QDC.CalculateLimits(~10,9,EMH),"Incorrect top-Right diagonal Limit for Static data 1"), // shape ID 0
                () -> assertArrayEquals(new int[]{2,8},QDC.CalculateLimits(~10,18,EMH),"Incorrect top-Right diagonal Limit for Static data 2"), // shape ID 0
                () -> assertArrayEquals(new int[]{3,2},QDC.CalculateLimits(~10,27,EMH),"Incorrect top-Right diagonal Limit for Static data 3"), // shape ID 0
                () -> assertArrayEquals(new int[]{6,0},QDC.CalculateLimits(~10,36,EMH),"Incorrect top-Right diagonal Limit for Static data 4"), // shape ID 11
                () -> assertArrayEquals(new int[]{3,5},QDC.CalculateLimits(~10,45,EMH),"Incorrect top-Right diagonal Limit for Static data 5"), // shape ID 12

                // checking bottom-right to top-left
                () -> assertArrayEquals(new int[]{7,9},QDC.CalculateLimits(9,99,EMH),"Incorrect bottom-Right diagonal Limit for Static data 1"),
                () -> assertArrayEquals(new int[]{0,8},QDC.CalculateLimits(9,88,EMH),"Incorrect bottom-Right diagonal Limit for Static data 2"),
                () -> assertArrayEquals(new int[]{0,1},QDC.CalculateLimits(9,77,EMH),"Incorrect bottom-Right diagonal Limit for Static data 3"),
                () -> assertArrayEquals(new int[]{3,1},QDC.CalculateLimits(9,66,EMH),"Incorrect bottom-Right diagonal Limit for Static data 4"),
                () -> assertArrayEquals(new int[]{3,3},QDC.CalculateLimits(9,55,EMH),"Incorrect bottom-Right diagonal Limit for Static data 5"),

                // checking bottom-left to top-right
                () -> assertArrayEquals(new int[]{1,5},QDC.CalculateLimits(10,90,EMH),"Incorrect bottom-Left diagonal Limit for Static data 1"), // shape ID 0
                () -> assertArrayEquals(new int[]{0,4},QDC.CalculateLimits(10,81,EMH),"Incorrect bottom-Left diagonal Limit for Static data 2"), // shape ID 0
                () -> assertArrayEquals(new int[]{0,1},QDC.CalculateLimits(10,72,EMH),"Incorrect bottom-Left diagonal Limit for Static data 3"), // shape ID 11
                () -> assertArrayEquals(new int[]{6,3},QDC.CalculateLimits(10,63,EMH),"Incorrect bottom-Left diagonal Limit for Static data 4"), // shape ID 52
                () -> assertArrayEquals(new int[]{2,3},QDC.CalculateLimits(10,54,EMH),"Incorrect bottom-Left diagonal Limit for Static data 5"),

                //Single Directional Flags
                () -> assertArrayEquals(new int[]{0,1},QDC.CalculateLimits(4,62,EMH),"Incorrect down-only limit"), // down only
                () -> assertArrayEquals(new int[]{0,2},QDC.CalculateLimits(8,61,EMH),"Incorrect up-only limit"), // up only
                () -> assertArrayEquals(new int[]{0,0},QDC.CalculateLimits(1,57,EMH),"Incorrect left-only limit"), // left only
                () -> assertArrayEquals(new int[]{1,0},QDC.CalculateLimits(2,22,EMH),"Incorrect right-only limit") // right only
        );
    }

    @Test
    @Order(6)
    @DisplayName("Uniform Rectangle Rep Test")
    public void rectangleRectangleRepTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        PixelChunk[] chunks = ExpectedDataHolder.getExpectedUniformChunkData(ChannelColorTypes.Gray,10,10,10);

        assertAll(
                () -> assertArrayEquals(
                        new RectangleRepresentative[]{new RectangleRepresentative(10,10,0,10)}, // as its a perfect fit, only one is made
                        QDC.CalculateLongRectangles(ExpectedDataHolder.UniformBasedEdgeMaps, 0, 9, chunks[0],emptyBitSet), // right and down check
                        "Incorrect Right and Down Rectangle"
                        ),
                () -> assertArrayEquals(
                        new RectangleRepresentative[]{new RectangleRepresentative(10,10,0,10)}, // right and up check
                        QDC.CalculateLongRectangles(ExpectedDataHolder.UniformBasedEdgeMaps, 90, 5, chunks[0],emptyBitSet),
                        "Incorrect Right and Up Rectangle"
                ),
                () -> assertArrayEquals(
                        new RectangleRepresentative[]{new RectangleRepresentative(10,10,0,10)},
                        QDC.CalculateLongRectangles(ExpectedDataHolder.UniformBasedEdgeMaps, 9, 10, chunks[0],emptyBitSet),// left and down check
                        "Incorrect Left and Down Rectangle"
                ),
                () -> assertArrayEquals(
                        new RectangleRepresentative[]{new RectangleRepresentative(10,10,0,10)},
                        QDC.CalculateLongRectangles(ExpectedDataHolder.UniformBasedEdgeMaps, 99, 6, chunks[0],emptyBitSet), // left and up check
                        "Incorrect Left and Up Rectangle"
                ),

                //Squares
                () -> assertEquals(
                        new RectangleRepresentative(10,10,0,10), // as its a perfect fit, only one is made
                        QDC.CalculateSquare(ExpectedDataHolder.UniformBasedEdgeMaps, 0, 9, chunks[0],emptyBitSet), // right and down check
                        "Incorrect Right and Down Square"
                ),
                () -> assertEquals(
                        new RectangleRepresentative(10,10,0,10), // right and up check
                        QDC.CalculateSquare(ExpectedDataHolder.UniformBasedEdgeMaps, 90, 5, chunks[0],emptyBitSet),
                        "Incorrect Right and Up Square"
                ),
                () -> assertEquals(
                        new RectangleRepresentative(10,10,0,10),
                        QDC.CalculateSquare(ExpectedDataHolder.UniformBasedEdgeMaps, 9, 10, chunks[0],emptyBitSet),// left and down check
                        "Incorrect Left and Down Square"
                ),
                () -> assertEquals(
                        new RectangleRepresentative(10,10,0,10),
                        QDC.CalculateSquare(ExpectedDataHolder.UniformBasedEdgeMaps, 99, 6, chunks[0],emptyBitSet), // left and up check
                        "Incorrect Left and Up Square"
                )
        );
    }

    @Test
    @Order(7)
    @DisplayName("Block Rectangle Rep Test")
    public void blockRectangleRepTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        PixelChunk[] chunks = ExpectedDataHolder.getExpectedBlockChunkData(ChannelColorTypes.Gray,10,100,255);

        assertAll(
                //Rectangles
                // 0's
                () -> assertArrayEquals( // the 0 from the top-left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 10, 0, 10),
                                new RectangleRepresentative(10, 1, 0, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,0,9,chunks[0],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 0 from the bottom-right
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(2, 10, 80, 10),
                                new RectangleRepresentative(10, 3, 7, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,99,6,chunks[0],emptyBitSet)
                ),

                // 11's
                () -> assertArrayEquals( // the 11 at the top left part
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(6, 1, 11, 100),
                                new RectangleRepresentative(6, 1, 11, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,11,11,chunks[1],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 11 at the bottom corner connection of the U
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 4, 72, 100),
                                new RectangleRepresentative(2, 1, 62, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,72,5,chunks[1],emptyBitSet)
                ),

                // 12's [this is just a square so only 1 test]
                () -> assertArrayEquals( // the 12 in the top-left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(4, 4, 12, 255)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,12,9,chunks[2],emptyBitSet)
                ),

                // 52's
                () -> assertArrayEquals( // the 52 in the top-left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 4, 52, 10),
                                new RectangleRepresentative(1, 4, 52, 10),
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,52,13,chunks[3],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 52 in the under part at the left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(2, 2, 53, 10),
                                new RectangleRepresentative(2, 2, 53, 10),
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,63,5,chunks[3],emptyBitSet)
                ),

                // basic squares test
                //0
                () -> assertEquals( // the 0 from the bottom-right
                        new RectangleRepresentative(3, 3, 77, 10),
                        QDC.CalculateSquare(ExpectedDataHolder.BlockBasedEdgeMaps,99,6,chunks[0],emptyBitSet)
                ),

                // 11's
                () -> assertEquals( // the 11 at the U corner grab on the left
                        new RectangleRepresentative(1, 1, 62, 100),
                        QDC.CalculateSquare(ExpectedDataHolder.BlockBasedEdgeMaps,62,10,chunks[1],emptyBitSet)
                ),

                // 12's
                () -> assertEquals( // the 12 at the bottom left
                        new RectangleRepresentative(4, 4, 12, 255),
                        QDC.CalculateSquare(ExpectedDataHolder.BlockBasedEdgeMaps,42,5,chunks[2],emptyBitSet)
                ),

                //52's
                () -> assertEquals( // the 52 at the bottom right
                        new RectangleRepresentative(2, 2, 53, 10),
                        QDC.CalculateSquare(ExpectedDataHolder.BlockBasedEdgeMaps,64,6,chunks[3],emptyBitSet)
                )
        );
    }

    @Test
    @Order(8)
    @DisplayName("Static Rectangle Rep Test")
    public void staticRectangleRepTest() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        PixelChunk[] chunks = ExpectedDataHolder.getExpectedStaticChunkData(ChannelColorTypes.Gray,10,100);

        assertAll(
                // Rectangles
                // 0's
                () -> assertArrayEquals( // the 0 from the top-left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(2, 6, 0, 100),
                                new RectangleRepresentative(2, 6, 0, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,0,9,chunks[0],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 0 from the bottom in the left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(3, 2, 2, 100),
                                new RectangleRepresentative(3, 2, 2, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,22,5,chunks[0],emptyBitSet)
                ),

                // 40's
                () -> assertArrayEquals( // the 40 at the top left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(6, 2, 40, 100),
                                new RectangleRepresentative(6, 2, 40, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,40,9,chunks[2],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 40 at the right-most area at the top
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(2, 3, 60, 100),
                                new RectangleRepresentative(2, 3, 60, 100)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,62,10,chunks[2],emptyBitSet)
                ),

                // 6's
                () -> assertArrayEquals( // the 6 at the top-left most point
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(4, 4, 6, 10),
                                new RectangleRepresentative(4, 4, 6, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,6,9,chunks[1],emptyBitSet)
                ),
                () -> assertArrayEquals( // the 6 at the top-left most point
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(2, 6, 24, 10),
                                new RectangleRepresentative(8, 2, 24, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.StaticBasedEdgeMaps,24,9,chunks[1],emptyBitSet)
                ),

                // Squares
                // 0's
                () -> assertEquals( // the 0 at the bottom-right most point in the outdent
                        new RectangleRepresentative(2,2,12,100),
                        QDC.CalculateSquare(ExpectedDataHolder.StaticBasedEdgeMaps,23,6,chunks[0],emptyBitSet)
                ),
                // 6's
                () -> assertEquals( // the 6 before the 57
                        new RectangleRepresentative(2,2,55,10),
                        QDC.CalculateSquare(ExpectedDataHolder.StaticBasedEdgeMaps,56,10,chunks[1],emptyBitSet)
                ),
                // 40's
                () -> assertEquals( // the 40 at the bottom-right most point in the outdent
                        new RectangleRepresentative(2,2,61,100),
                        QDC.CalculateSquare(ExpectedDataHolder.StaticBasedEdgeMaps,72,6,chunks[2],emptyBitSet)
                )
        );
    }

    // Rectangle Processing
    @Test
    @Order(9)
    @DisplayName("Full Square Rectangle Processing")
    public void fullSquareRectangleProcess() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();

        BitSet trackingSet = new BitSet(100);

        TestHelper.DataholderSetter(10,10);

        ArrayList<CornerPoint> trackingCorners = new ArrayList<>(ExpectedDataHolder.fullSquare.cornerPoints);

        QDC.processBestRectangle(new RectangleRepresentative(10,10,0,0),trackingCorners, trackingSet);

        assertAll(
                () -> assertArrayEquals(new long[]{-1, 68719476735L}, trackingSet.toLongArray(), "trackingSet not Empty for Full Square"),
                () -> assertArrayEquals(new ArrayList<CornerPoint>(){}.toArray(), trackingCorners.toArray(), "Found more Corners for full square")
        );
    }

    @Test
    @Order(10)
    @DisplayName("Hole Square Rectangle Processing")
    public void holeSquareRectangleProcess() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();

        BitSet trackingSet = new BitSet(100);

        // flip the hole
        trackingSet.flip(22);
        trackingSet.flip(23);
        trackingSet.flip(32);
        trackingSet.flip(33);

        TestHelper.DataholderSetter(10,10);

        ArrayList<CornerPoint> trackingCorners = new ArrayList<>(ExpectedDataHolder.holeSquare.cornerPoints);

        QDC.processBestRectangle(new RectangleRepresentative(2,10,0,0),trackingCorners, trackingSet);

        assertAll(
                () -> assertArrayEquals(new long[]{12898533375L}, trackingSet.toLongArray(), "trackingSet not correct for hole Square"),

                () -> assertArrayEquals(new ArrayList<CornerPoint>(){{
                    add(new CornerPoint(90,5));
                    add(new CornerPoint(99,6));
                    add(new CornerPoint(20,9));
                    add(new CornerPoint(21,10));
                    add(new CornerPoint(24,9));
                    add(new CornerPoint(29,10));
                }}.toArray(),
                        trackingCorners.toArray(),
                        "Found wrong corners fore hole square")
        );
    }

    // BitSet tests
    @Test
    @Order(11)
    @DisplayName("Hole Square Rectangle Processing with PreProcessed")
    public void holeSquareRectangleProcessWithCutOut() {
        QuadralizedDataCompressor QDC = new QuadralizedDataCompressor();
        TestHelper.DataholderSetter(10,10);

        PixelChunk[] chunks = ExpectedDataHolder.getExpectedBlockChunkData(ChannelColorTypes.Gray,10,100,255);

        BitSet processedPixels = new BitSet(100);
        processedPixels.flip(80,100); // cut out the last 2 rows

        assertAll(
                //Rectangles
                // 0's
                () -> assertArrayEquals( // the 0 from the top-left
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 10, 0, 10),
                                new RectangleRepresentative(8, 1, 0, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,0,9,chunks[0],processedPixels)
                ),
                () -> assertArrayEquals( // the 0 from the top-right
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 10, 0, 10),
                                new RectangleRepresentative(8, 3, 7, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,9,10,chunks[0],processedPixels)
                ),
                () -> assertArrayEquals( // the 0 from the top-right
                        new RectangleRepresentative[]{
                                new RectangleRepresentative(1, 2, 70, 10),
                                new RectangleRepresentative(8, 1, 0, 10)
                        },
                        QDC.CalculateLongRectangles(ExpectedDataHolder.BlockBasedEdgeMaps,70,5,chunks[0],processedPixels) // the 5 is stored in the corner point but is not default value of this point
                ),

                // Square test
                () -> assertEquals(
                        new RectangleRepresentative(3,3,57,10),
                        QDC.CalculateSquare(ExpectedDataHolder.BlockBasedEdgeMaps,79,6,chunks[0],processedPixels)
                )
        );
    }
}