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

        for (ColorChannel channel : IR.getColorChannels()) {
            ColorRepresentative[] IndependentColors = IsolateSeperateColors(channel.getChannelType());

            for (ColorRepresentative CR : IndependentColors) {
                BitSet remainingData = CR.getPixelIndexes();
                BitSet visited = new BitSet(ImageDataHolder.flatsize);

                int index=remainingData.nextSetBit(0);
                while (index!=-1) {
                    FindChunkReturn chunkReturn = FindChunk(CR,visited,index);

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

    private FindChunkReturn FindChunk(ColorRepresentative CR, BitSet visited, int seedIndex) {
        BitSet CRData = CR.getPixelIndexes();
        visited.flip(seedIndex);
        Queue<Integer> BFSQueue = new ArrayDeque<>(){{add(seedIndex);}};

        PixelChunk dataBlock = new PixelChunk(CR.getColorType(),CR.getColorValue());
        int smallestIndex = ImageDataHolder.flatsize;

        while (!BFSQueue.isEmpty()) {
            int currentIndex = BFSQueue.poll();
            smallestIndex = Math.min(currentIndex,smallestIndex);

            boolean[] flags = {false,false,false,false};

            // left, right, down, up
            int[] toSearch = new int[]{currentIndex - 1, currentIndex + 1, currentIndex + ImageDataHolder.width, currentIndex - ImageDataHolder.width};
            // {go left one, go right one, go down one, go up one} *wraps*

            for (int directionalIndex = 0; directionalIndex < toSearch.length; directionalIndex++) {
                int directionalColor = toSearch[directionalIndex];

                boolean outOfBounds  = !IndexInImage(directionalColor);
                boolean inSameRow = IndexsInSameRow(directionalColor,currentIndex);

                if (outOfBounds || (!inSameRow && directionalIndex<2)) {
                    flags[directionalIndex] = true;
                    continue;
                }

                if (CRData.get(directionalColor)) { // if its there add to queue and move on
                    if (!visited.get(directionalColor)) {
                        visited.flip(directionalColor);
                        BFSQueue.add(directionalColor);
                    }
                } else {
                    flags[directionalIndex] = true;
                }
            }

            int flagmask = 0;
            if (flags[0]) flagmask |= 1; // 0001; left
            if (flags[1]) flagmask |= 2; // 0010; right
            if (flags[2]) flagmask |= 4; // 0100; down
            if (flags[3]) flagmask |= 8; // 1000; up

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

    public ChunkedImageRepresentative FlattenChunks(GetChunksReturn data, boolean grayscale, boolean uniformMaxAlpha) {
        int size = (grayscale ? 2 : 4) - (uniformMaxAlpha ? 1 : 0);
        ChunkedColorChannel[] returnData = new ChunkedColorChannel[size];

        for (int i=0; i<size;i++) {
            IntArrayList IC = data.IsolatedIndexes()[i];
            PixelChunk[] PC = data.pixelChunks()[i].toArray(new PixelChunk[0]);

            returnData[i] = new ChunkedColorChannel(PC[0].getColorType(),PC.length,IC.size());
            returnData[i].setChannelChunks(PC);
            returnData[i].setIsolatedIndexes(IC.toIntArray());
        }

        ChunkedImageRepresentative CIR = new ChunkedImageRepresentative(grayscale,uniformMaxAlpha);
        CIR.setChunkedChannels(returnData);
        CIR.SetImageCompressionInformation();

        return CIR;
    }
}