package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.CornerPoint;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;
import com.maxgamertyper1.datatypes.records.ShapeGrouperResult;
import com.maxgamertyper1.imagereading.ImageMeta;

import java.util.Arrays;
import java.util.BitSet;

public class ChunkProcessor {
    private static final int IMAGE_WALL = -1;
    private static final int NO_CHUNK = -1;

    ImageRepresentative imageToChunk;

    public ChunkProcessor(ImageRepresentative image) {
        imageToChunk = image;
    }

    public ChunkedImageRepresentative chunkImage() {
        ColorChannel[] currentChannels = imageToChunk.getColorChannels();
        ChunkedColorChannel[] returnData = new ChunkedColorChannel[currentChannels.length];

        for (int i=0;i<currentChannels.length;i++) {
            ColorChannel CR = currentChannels[i];
            int[] rawValues = CR.getChannelData();
            NeighborScanResult NSR = neighborScan(CR);


            ShapeUnionFind shapeOrganizer = NSR.Shapes();

            shapeOrganizer.flatten();
            shapeOrganizer.calculateShapeAreas();
            shapeOrganizer.calculateShapeContinuity();
            //shapeOrganizer.calculatePerimiterAndHoles(NSR.Flags());
            shapeOrganizer.calculateIndexGroupings();

            ShapeGrouperResult SGR = convertToPixelChunks(NSR.Shapes(), CR, NSR.Flags());

            int[] isoIndexes = SGR.IsolatedIndexes().toIntArray();
            PixelChunk[] PCs = SGR.Chunks();
            ChunkedColorChannel CCC = new ChunkedColorChannel(
                    CR.getChannelType(),
                    PCs.length,
                    isoIndexes.length,
                    NSR.Shapes(),
                    NSR.CardinalMap(),
                    NSR.Flags()
            );

            int[] correspondingIsolatedColors = new int[isoIndexes.length];

            for (int j=0; j < isoIndexes.length; j++) {
                correspondingIsolatedColors[j] = rawValues[isoIndexes[j]];
            }

            CCC.setIsolatedIndexes(isoIndexes,correspondingIsolatedColors);
            CCC.setChannelChunks(PCs);
            returnData[i] = CCC;
        }

        ImageMeta metaData = imageToChunk.getMetaData();

        ChunkedImageRepresentative CIR = new ChunkedImageRepresentative(metaData.grayscale());
        CIR.setMetaData(metaData);

        CIR.setChunkedChannels(returnData);
        CIR.SetImageCompressionInformation();
        CIR.setInitialPaethValues(imageToChunk.getInitialPixels());

        return CIR;
    }

    private NeighborScanResult neighborScan(ColorChannel colorChannel) {
        int[] pixelData = colorChannel.getChannelData();
        ImageMeta metaData = imageToChunk.getMetaData();
        int width = metaData.width();
        int height = metaData.height();
        byte[] pixelFlags = new byte[metaData.flatsize()];
        ShapeUnionFind shapeOrganizer = new ShapeUnionFind(metaData.flatsize());

        BitSet[] rowRightEdgeBits = new BitSet[height];
        BitSet[] columnBottomEdgeBits = new BitSet[width];

        for (int i=0; i<height;i++) {
            rowRightEdgeBits[i] = new BitSet(width); // right edges indexed by the row
        }
        for (int i=0; i<width;i++) {
            columnBottomEdgeBits[i] = new BitSet(height); // bottom edges indexed by column
        }

        int flatIndex = 0; // this is basically the flat coordinate of the pixel
        for (int y=0; y<height;y++) {
            for (int x=0; x<width;x++,flatIndex++) {
                int color = pixelData[flatIndex];
                byte flagMask = 0;
                // check EdgeFlags to understand how the mask works

                int pixelAbove = flatIndex - width;
                int pixelLeft = flatIndex - 1;

                if (y==0) { // on the top of the image
                    flagMask |= EdgeFlags.UP;
                } else if (pixelData[pixelAbove]==color) { // if the pixel above has the same color, union them
                    shapeOrganizer.union(flatIndex, pixelAbove);
                } else { // not the same color: mark as an edge
                    flagMask |= EdgeFlags.UP;
                    pixelFlags[pixelAbove] |= EdgeFlags.DOWN; // add the down edge to the pixel above
                    columnBottomEdgeBits[x].set(y - 1); // mark as a bottom edge
                }

                if (x==0) { // if on the left edge
                    flagMask |= EdgeFlags.LEFT;
                } else if (pixelData[pixelLeft]==color) { // if the left pixel is the same color, union them
                    shapeOrganizer.union(flatIndex, pixelLeft);
                } else {
                    flagMask |= EdgeFlags.LEFT;
                    pixelFlags[pixelLeft] |= EdgeFlags.RIGHT; // add the right edge to the pixel on the left
                    rowRightEdgeBits[y].set(x - 1); // mark as an edge
                }
                // image edges
                if (y == height - 1) { // add to bottom since that wont work as a bottom pixel isnt there
                    flagMask |= EdgeFlags.DOWN;
                    columnBottomEdgeBits[x].set(y);
                }
                if (x==width-1) { // add to the right since there isnt a pixel to mark as a left edge
                    flagMask |= EdgeFlags.RIGHT;
                    rowRightEdgeBits[y].set(x);
                }

                pixelFlags[flatIndex] |= flagMask;
            }
        }

        int[][] columnBottomEdges = toEdgeArrays(columnBottomEdgeBits);
        int[][] rowRightEdges = toEdgeArrays(rowRightEdgeBits);

        return new NeighborScanResult(shapeOrganizer,pixelFlags,new EdgeMapHolder(columnBottomEdges,rowRightEdges));
    }

    private static int[][] toEdgeArrays(BitSet[] edgeBits) {
        int[][] edgeArray = new int[edgeBits.length][];

        for (int i=0; i<edgeBits.length;i++) {
            int[] edges = edgeArray[i];
            BitSet bits = edgeBits[i];

            int count = bits.cardinality();
            edges = new int[count+1];
            edges[0] = IMAGE_WALL;

            int j =1;
            for (int bit = bits.nextSetBit(0); bit >= 0; bit = bits.nextSetBit(bit + 1)) {
                edges[j++] = bit;
            }
        }

        return edgeArray;
    }

    private ShapeGrouperResult convertToPixelChunks(ShapeUnionFind shapeOrganizer, ColorChannel colorChannel, byte[] computedFlags) {
        int[] shapeHolder = shapeOrganizer.getRawData();
        int[] rawPixels = colorChannel.getChannelData();

        IntArrayList isolatedPixels = new IntArrayList(2048);
        PixelChunk[] chunks = new PixelChunk[shapeOrganizer.getTotalShapeCount()];
        int[] chunkIndexByShape = new int[shapeHolder.length]; // same as flatsize
        int chunkCount = 0;
        Arrays.fill(chunkIndexByShape,NO_CHUNK);

        for (int pixel=0; pixel<shapeHolder.length; pixel++) {
            int shapeID = shapeHolder[pixel];
            int flag = computedFlags[pixel];

            int chunkIndex = chunkIndexByShape[shapeID];
            if (chunkIndex == NO_CHUNK) {
                if (flag==EdgeFlags.ALL) {
                    isolatedPixels.add(pixel);
                    continue;
                }
                PixelChunk chunk = new PixelChunk(colorChannel.getChannelType(),rawPixels[pixel]);

                chunk.setStartingIndex(shapeID);
                chunk.setTotalArea(shapeOrganizer.getShapeArea(shapeID));
                chunks[chunkCount] = chunk;
                chunkIndex = chunkCount;
                chunkIndexByShape[shapeID] = chunkCount++;
            }

            int cornerCount = EdgeFlags.cornerCount(flag);
            if (cornerCount > 0) {
                PixelChunk chunk = chunks[chunkIndex];
                chunk.cornerPoints.add(new CornerPoint(pixel, flag));
                chunk.convexCorners += cornerCount;
            }
        }

        return new ShapeGrouperResult(Arrays.copyOf(chunks, chunkCount),isolatedPixels);
    }
}
