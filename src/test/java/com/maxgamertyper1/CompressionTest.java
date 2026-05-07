package com.maxgamertyper1;

import com.maxgamertyper1.compressor.QuadralizedDataCompressor;
import com.maxgamertyper1.datatypes.ShapeUnionFind;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

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

    //Next up is the actual rectangle calculation *gulp*
}