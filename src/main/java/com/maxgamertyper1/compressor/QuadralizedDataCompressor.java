package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.PixelHelper;
import java.util.*;

import static com.maxgamertyper1.imagereading.PixelHelper.*;

public class QuadralizedDataCompressor {
    public ImageRepresentative DeltaEncodedImage;
    public ImageRepresentative QuadralizedCompressedImage;


    public QuadralizedDataCompressor() {}

    public void PaethDeltaEncoder() {
        DeltaEncodedImage = new ImageRepresentative(ImageDataHolder.isGrayscale,ImageDataHolder.UniformMaxAlpha,true,ImageDataHolder.flatsize);

        for (ColorChannel channel : ImageDataHolder.channeledColors.getColorChannels()) {
            int[] rawPixelData = channel.getChannelData();
            int channelConcurrency = 0;

            for (int pixelIndex=0; pixelIndex<rawPixelData.length-1; pixelIndex++) {
                int pixelColor = rawPixelData[pixelIndex];
                boolean onTop = false;

                int leftPixelColor,topPixelColor,upperLeftPixelColor;
                if (PixelHelper.IndexIsOnTopRow(ImageDataHolder.width,pixelIndex)) {
                    topPixelColor=0;
                    upperLeftPixelColor=0;
                    onTop=true;
                } else {
                    topPixelColor = rawPixelData[pixelIndex-ImageDataHolder.width];
                    upperLeftPixelColor=1; // mark top row clear
                }

                if (PixelHelper.IndexIsOnLeftColomn(ImageDataHolder.width,pixelIndex)) {
                    leftPixelColor=0;
                    upperLeftPixelColor=0;

                    if (onTop) { // very first pixel
                        DeltaEncodedImage.setIntialPixel(channel.getChannelType(),pixelColor);
                        DeltaEncodedImage.setPixelColorChannel(channel.getChannelType(),pixelIndex,0);
                        continue;
                    }
                } else {
                    leftPixelColor = rawPixelData[pixelIndex-1];
                    if (upperLeftPixelColor==1) upperLeftPixelColor=rawPixelData[pixelIndex-1-ImageDataHolder.width];
                }

                int prediction = getPrediction(leftPixelColor, topPixelColor, upperLeftPixelColor);
                int delta = pixelColor - prediction;
                DeltaEncodedImage.setPixelColorChannel(channel.getChannelType(),pixelIndex,delta);
            }
        }
    }

    public ChannelConcurrencyPercents getChannelConncurrencyPercents() {
        float deltaChunkRate = getImageChunkRate(DeltaEncodedImage);
        float colorChunkRate = getImageChunkRate(ImageDataHolder.channeledColors);

        System.out.println("Delta noise percent: "+(1-deltaChunkRate)*100);
        System.out.println("Color noise percent: "+(1-colorChunkRate)*100);

        return new ChannelConcurrencyPercents(deltaChunkRate,colorChunkRate);
    }

    private static float getImageChunkRate(ImageRepresentative IR) {
        int channelConcurrenciesTotal=0;
        int channelCount = IR.getColorChannels().length;
        long maxPossible = (long) ImageDataHolder.height * (ImageDataHolder.width - 1) + (long) ImageDataHolder.width * (ImageDataHolder.height - 1);

        for (ColorChannel ch : IR.getColorChannels()) {
            int channelConcurrencies = getChannelConcurrencies(ch);
            channelConcurrenciesTotal+=channelConcurrencies;
        }
        return (float) channelConcurrenciesTotal / (maxPossible*channelCount);
    }

    private static int getChannelConcurrencies(ColorChannel ch) {
        int channelConcurrencies = 0;
        int[] pixels = ch.getChannelData();

        for (int pixelIndex=0; pixelIndex<pixels.length; pixelIndex++) {
            int pixelColor = pixels[pixelIndex];
            int bottomIndex = pixelIndex+ImageDataHolder.width;
            int rightIndex = pixelIndex+1;

            boolean bottomOOB = false;
            boolean rightOOB = false;

            if (!IndexInImage(bottomIndex)) bottomOOB=true;
            if (!IndexInImage(rightIndex) || !IndexsInSameRow(rightIndex,pixelIndex)) rightOOB=true;


            if (!rightOOB) {
                if (pixelColor==pixels[rightIndex]) channelConcurrencies++;
            }
            if (!bottomOOB) {
                if (pixelColor==pixels[bottomIndex]) channelConcurrencies++;
            }
        }
        return channelConcurrencies;
    }

    private static int getPrediction(int leftPixelColor, int topPixelColor, int upperLeftPixelColor) {
        int paethPrediction = leftPixelColor + topPixelColor - upperLeftPixelColor;

        int leftDistance = Math.abs(paethPrediction- leftPixelColor);
        int topDistance = Math.abs(paethPrediction- topPixelColor);
        int upperLeftDistance = Math.abs(paethPrediction- upperLeftPixelColor);

        int prediction;
        if (leftDistance <= topDistance && leftDistance <= upperLeftDistance) {
            prediction = leftPixelColor;
        } else if (topDistance <= upperLeftDistance) {
            prediction = topPixelColor;
        } else {
            prediction = upperLeftPixelColor;
        }
        return prediction;
    }

    public GetChunksReturn GetChunks(ImageRepresentative IR, float estimatedChunkPercent, boolean grayscale, boolean uniformMaxAlpha) {
        int size = (grayscale ? 2 : 4) - (uniformMaxAlpha ? 1 : 0);
        ArrayList<PixelChunk>[] chunks = new ArrayList[size];
        IntArrayList[] isolatedIndexes = new IntArrayList[size];

        double noiseFactor = 1-estimatedChunkPercent;
        int floor = Math.max(128, (int) Math.sqrt(ImageDataHolder.flatsize));
        int ceiling = (int) Math.sqrt(ImageDataHolder.flatsize)*50;
        int defaultSize = Math.toIntExact((long) Math.ceil(ImageDataHolder.flatsize * noiseFactor * 0.5));

        int finalSize = Math.max(floor,Math.min(ceiling,defaultSize));
        System.out.println("base BFS arraylist size: "+finalSize);

        for (int i = 0; i < size; i++) {
            chunks[i] = new ArrayList<>(finalSize);
            isolatedIndexes[i] = new IntArrayList(finalSize);
        }

        Queue<Integer> BFSQueue = new ArrayDeque<>();

        for (ColorChannel channel : IR.getColorChannels()) {
            ColorRepresentative[] IndependentColors = IsolateSeperateColors(channel.getChannelType());

            for (ColorRepresentative CR : IndependentColors) {
                BitSet remainingData = CR.getPixelIndexes();
                BitSet visited = new BitSet(ImageDataHolder.flatsize);

                int index=remainingData.nextSetBit(0);
                while (index!=-1) {
                    BFSQueue.add(index);
                    visited.set(index);
                    FindChunkReturn chunkReturn = FindChunk(CR,visited,BFSQueue);

                    PixelChunk PC = chunkReturn.PixelChunk();
                    int channelIndex = ImageRepresentative.staticGetChannelIndex(CR.getColorType(),grayscale,uniformMaxAlpha);
                    if (PC.getIsolated()) {
                        isolatedIndexes[channelIndex].Add(PC.getIsolatedIndex());
                    } else {
                        chunks[channelIndex].add(PC);
                    }

                    remainingData.andNot(chunkReturn.visited());
                    index = remainingData.nextSetBit(index);
                }
            }
        }
        return new GetChunksReturn(isolatedIndexes, chunks);
    }

    private FindChunkReturn FindChunk(ColorRepresentative CR, BitSet visited, Queue<Integer> BFSQueue) {
        BitSet CRData = CR.getPixelIndexes();
        PixelChunk dataBlock = new PixelChunk(CR.getColorType(),CR.getColorValue());
        int smallestIndex = ImageDataHolder.flatsize;

        while (!BFSQueue.isEmpty()) {
            int currentIndex = BFSQueue.poll();
            smallestIndex = Math.min(currentIndex,smallestIndex);

            // left, right, down, up
            int[] toSearch = new int[]{currentIndex - 1, currentIndex + 1, currentIndex + ImageDataHolder.width, currentIndex - ImageDataHolder.width};
            int flagmask = 0;
            // {go left one, go right one, go down one, go up one} *wraps*

            // maybe unwrap this loop; it migh be faster that way (less GC cleanup)
            for (int directionalIndex = 0; directionalIndex < toSearch.length; directionalIndex++) {
                int directionalColor = toSearch[directionalIndex];

                boolean outOfBounds  = !IndexInImage(directionalColor);
                boolean inSameRow = IndexsInSameRow(directionalColor,currentIndex);

                if (outOfBounds || (!inSameRow && directionalIndex<2)) {
                    flagmask |= 1 << directionalIndex;
                    continue;
                }

                if (CRData.get(directionalColor)) { // if its there add to queue and move on
                    if (!visited.get(directionalColor)) {
                        visited.flip(directionalColor);
                        BFSQueue.add(directionalColor);
                    }
                } else {
                    flagmask |= 1 << directionalIndex;
                }
            }

            if (flagmask==15) {
                dataBlock.setIsolatedIndex(currentIndex);
                break;
            } else if (flagmask!=8 && flagmask!=12 && flagmask>4) {
                dataBlock.cornerPoints.add(new CornerPoint(currentIndex,flagmask));
            }

            dataBlock.StartingIndex = smallestIndex;
        }
        return new FindChunkReturn(dataBlock, visited);
    }

    private ColorRepresentative[] IsolateSeperateColors(ChannelColorTypes colorType) {
        int[] pixels = DeltaEncodedImage.getOneColorChannel(colorType).getChannelData();

        HashMap<Integer,BitSet> splitColors = new HashMap<>();
        for (int colorIndex=0; colorIndex<pixels.length; colorIndex++) {
            int colorValue = pixels[colorIndex];

            if (!splitColors.containsKey(colorValue)) splitColors.put(colorValue,new BitSet(ImageDataHolder.flatsize));

            splitColors.get(colorValue).flip(colorIndex);
        }

        ColorRepresentative[] result = new ColorRepresentative[splitColors.size()];

        int i=0;
        for (Map.Entry<Integer,BitSet> map : splitColors.entrySet()) {
            int colorValue = map.getKey();
            BitSet colorLocations = map.getValue();

            ColorRepresentative rep = new ColorRepresentative(colorType,colorValue);

            rep.firstIndex = colorLocations.nextSetBit(0);
            rep.setPixelIndexes(colorLocations);
            result[i++] = rep;
        }

        return result;
    }

    public ChunkedImageRepresentative chunkImage(ImageRepresentative IR) {
        ColorChannel[] currentChannels = IR.getColorChannels();
        ChunkedColorChannel[] returnData = new ChunkedColorChannel[currentChannels.length];

        for (int i=0;i<currentChannels.length;i++) {
            ColorChannel CR = currentChannels[i];
            ShapeGrouperReturn SGR = ConvertToPixelChunks(NeighborScan(CR), CR);

            IntArrayList isoIndexes = SGR.IsolatedIndexes();
            PixelChunk[] PCs = SGR.Chunks();
            ChunkedColorChannel CCC = new ChunkedColorChannel(CR.getChannelType(),PCs.length,isoIndexes.size());

            CCC.setIsolatedIndexes(isoIndexes.toIntArray());
            CCC.setChannelChunks(PCs);
            returnData[i] = CCC;
        }

        ChunkedImageRepresentative CIR = new ChunkedImageRepresentative(ImageDataHolder.isGrayscale,ImageDataHolder.UniformMaxAlpha, IR.isDeltaEncoded());
        CIR.setChunkedChannels(returnData);
        CIR.SetImageCompressionInformation();

        return CIR;
    }

    private NeighborScanReturn NeighborScan(ColorChannel CR) {
        int[] pixelData = CR.getChannelData();
        int width = ImageDataHolder.width;
        int[] pixelFlags = new int[ImageDataHolder.flatsize];
        ShapeUnionFind shapeOrganizer = new ShapeUnionFind(ImageDataHolder.flatsize);

        for (int yValue=0; yValue<ImageDataHolder.height;yValue++) {
            for (int xValue=0; xValue<width;xValue++) {
                int currentFlatCoordinate = PixelHelper.IndexCalculator(width,xValue,yValue);
                int currentColor = pixelData[currentFlatCoordinate];

                int flagBitMask = 0;
                // first bit is left
                // second bit is right
                // third bit is down
                // fourth bit is up

                int topCoordinate = currentFlatCoordinate - width;
                int leftCoordinate = currentFlatCoordinate - 1;
                int bottomCoordinate = currentFlatCoordinate + width;
                int rightCoordinate = currentFlatCoordinate + 1;

                if (IndexInImage(topCoordinate)) {
                    if (pixelData[topCoordinate]==currentColor) {
                        shapeOrganizer.Union(currentFlatCoordinate, topCoordinate); // if the same color, add to the shape
                    } else  {
                        flagBitMask |= 1<<3; // if not the same color, mark as an edge
                    }
                } else {
                    flagBitMask |= 1<<3;
                }
                if (IndexInImage(leftCoordinate) && IndexsInSameRow(currentFlatCoordinate,leftCoordinate)) {
                    if (pixelData[leftCoordinate]==currentColor) {
                        shapeOrganizer.Union(currentFlatCoordinate, leftCoordinate);
                    } else  {
                        flagBitMask |= 1;
                    }
                } else {
                    flagBitMask |= 1;
                }
                if (!IndexInImage(bottomCoordinate) || pixelData[bottomCoordinate]!=currentColor) {
                    flagBitMask |= 1<<2;
                }
                if (!IndexInImage(rightCoordinate) || !IndexsInSameRow(currentFlatCoordinate,rightCoordinate) || pixelData[rightCoordinate]!=currentColor) {
                    flagBitMask |= 1<<1;
                }


                pixelFlags[currentFlatCoordinate]=flagBitMask;
            }
        }

        shapeOrganizer.Finalize(); // MIGHT NOT NEED

        return new NeighborScanReturn(shapeOrganizer,pixelFlags);
    }

    private ShapeGrouperReturn ConvertToPixelChunks(NeighborScanReturn data, ColorChannel CR) {
        ShapeUnionFind shapeOrganizer = data.Shapes();
        int[] shapeHolder = shapeOrganizer.getRawData();
        int[] flagHolder = data.Flags();
        int[] rawPixels = CR.getChannelData();


        IntArrayList IsolatedPixels = new IntArrayList(2048);
        PixelChunk[] returnHolder = new PixelChunk[shapeOrganizer.getTotalShapeCount()];
        int[] keyMapper = new int[ImageDataHolder.flatsize];
        int currentNextIndex = 0;
        Arrays.fill(keyMapper,-1);

        for (int pixelIndex=0; pixelIndex<shapeHolder.length; pixelIndex++) {
            int shapeID = shapeHolder[pixelIndex];
            int flag = flagHolder[pixelIndex];
            int dataKey = keyMapper[shapeID];

            if (dataKey==-1) {
                if (flag==15) {
                    IsolatedPixels.Add(pixelIndex);
                    continue;
                }

                PixelChunk PC = new PixelChunk(CR.getChannelType(),rawPixels[pixelIndex]);
                PC.StartingIndex = shapeID;

                if (flag!=8 && flag!=12 && flag>4) {
                    PC.cornerPoints.add(new CornerPoint(pixelIndex,flag));
                }

                returnHolder[currentNextIndex] = PC;
                keyMapper[shapeID] = currentNextIndex++;
            } else {
                // first bit is left
                // second bit is right
                // third bit is down
                // fourth bit is up
                if (flag!=8 && flag!=12 && flag>4) {
                    returnHolder[dataKey].cornerPoints.add(new CornerPoint(pixelIndex,flag));
                }
            }
        }

        int actualCount = 0;
        for (PixelChunk pc : returnHolder) {
            if (pc != null) actualCount++;
        }

        PixelChunk[] noNullChunks = new PixelChunk[actualCount];
        int index = 0;
        for (PixelChunk pc : returnHolder) {
            if (pc != null) {
                noNullChunks[index++] = pc;
            }
        }

        return new ShapeGrouperReturn(noNullChunks,IsolatedPixels);
    }

    public ChunkedImageRepresentative FlattenChunks(GetChunksReturn data, boolean grayscale, boolean uniformMaxAlpha, boolean usesPaeth) {
        int size = (grayscale ? 2 : 4) - (uniformMaxAlpha ? 1 : 0);
        ChunkedColorChannel[] returnData = new ChunkedColorChannel[size];

        for (int i=0; i<size;i++) {
            IntArrayList IC = data.IsolatedIndexes()[i];
            PixelChunk[] PC = data.pixelChunks()[i].toArray(new PixelChunk[0]);

            returnData[i] = new ChunkedColorChannel(PC[0].getColorType(),PC.length,IC.size());
            returnData[i].setChannelChunks(PC);
            returnData[i].setIsolatedIndexes(IC.toIntArray());
        }

        ChunkedImageRepresentative CIR = new ChunkedImageRepresentative(grayscale,uniformMaxAlpha, usesPaeth);
        CIR.setChunkedChannels(returnData);
        CIR.SetImageCompressionInformation();

        return CIR;
    }
}