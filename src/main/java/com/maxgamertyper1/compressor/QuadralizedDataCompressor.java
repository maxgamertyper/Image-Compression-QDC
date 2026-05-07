package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.*;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.PixelHelper;
import java.util.*;

import static com.maxgamertyper1.imagereading.PixelHelper.*;

public class QuadralizedDataCompressor {
    public ImageRepresentative DeltaEncodedImage;
    public ImageRepresentative QuadralizedCompressedImage;
    private ImageRepresentative UsedImage;

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

    public ChunkedImageRepresentative chunkImage(ImageRepresentative IR) {
        ColorChannel[] currentChannels = IR.getColorChannels();
        ChunkedColorChannel[] returnData = new ChunkedColorChannel[currentChannels.length];
        UsedImage = IR;

        for (int i=0;i<currentChannels.length;i++) {
            ColorChannel CR = currentChannels[i];
            int[] rawValues = CR.getChannelData();
            NeighborScanReturn NSR = NeighborScan(CR);
            ShapeGrouperReturn SGR = ConvertToPixelChunks(NSR.Shapes(), CR, NSR.Flags());

            int[] isoIndexes = SGR.IsolatedIndexes().toIntArray();
            PixelChunk[] PCs = SGR.Chunks();
            ChunkedColorChannel CCC = new ChunkedColorChannel(
                    CR.getChannelType(),
                    PCs.length,
                    isoIndexes.length,
                    NSR.Shapes(),
                    NSR.CardinalMap(),
                    NSR.Flags());


            int[] correspondingIsolatedColors = new int[isoIndexes.length];
            System.arraycopy(rawValues, 0, correspondingIsolatedColors, 0, isoIndexes.length);

            CCC.setIsolatedIndexes(isoIndexes,correspondingIsolatedColors);
            CCC.setChannelChunks(PCs);
            returnData[i] = CCC;
        }

        ChunkedImageRepresentative CIR = new ChunkedImageRepresentative(ImageDataHolder.isGrayscale,ImageDataHolder.UniformMaxAlpha, IR.isDeltaEncoded());
        CIR.setChunkedChannels(returnData);
        CIR.SetImageCompressionInformation();

        return CIR;
    }

    public NeighborScanReturn NeighborScan(ColorChannel CR) {
        int[] pixelData = CR.getChannelData();
        int width = ImageDataHolder.width;
        int[] pixelFlags = new int[ImageDataHolder.flatsize];
        ShapeUnionFind shapeOrganizer = new ShapeUnionFind(ImageDataHolder.flatsize);

        BitSet[] yBasedMap = new BitSet[ImageDataHolder.height];
        BitSet[] xBasedMap = new BitSet[ImageDataHolder.width];

        for (int i=0; i<ImageDataHolder.height;i++) {
            yBasedMap[i] = new BitSet(ImageDataHolder.width); // only bottom edges now
        }
        for (int i=0; i<ImageDataHolder.width;i++) {
            xBasedMap[i] = new BitSet(ImageDataHolder.height); // only right edges now
        }

        for (int yValue=0; yValue<ImageDataHolder.height;yValue++) {
            for (int xValue=0; xValue<width;xValue++) {
                int currentFlatCoordinate = PixelHelper.IndexCalculator(width,xValue,yValue);
                int currentColor = pixelData[currentFlatCoordinate];
                int[] coordinates = ReverseIndexCalculator(ImageDataHolder.width,currentFlatCoordinate);

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
                    xBasedMap[coordinates[0]].set(coordinates[1]);
                }
                if (!IndexInImage(rightCoordinate) || !IndexsInSameRow(currentFlatCoordinate,rightCoordinate) || pixelData[rightCoordinate]!=currentColor) {
                    flagBitMask |= 1<<1;
                    yBasedMap[coordinates[1]].set(coordinates[0]);
                }


                pixelFlags[currentFlatCoordinate]=flagBitMask;
            }
        }

        shapeOrganizer.Finalize();
        shapeOrganizer.calculateShapeAreas();
        shapeOrganizer.calculateShapeContinuity();
        shapeOrganizer.calculatePerimiterAndHoles(pixelFlags);

        int[][] finalXBasedMap = new int[ImageDataHolder.width][];
        int[][] finalYBasedMap = new int[ImageDataHolder.height][];

        for (int i=0; i<yBasedMap.length;i++) {
            BitSet dataHolder = yBasedMap[i];
            int count = dataHolder.cardinality();
            finalYBasedMap[i] = new int[count+1];
            finalYBasedMap[i][0]=-1; // the image wall

            int j =1;
            for (int bit = dataHolder.nextSetBit(0); bit >= 0; bit = dataHolder.nextSetBit(bit + 1)) {
                finalYBasedMap[i][j++] = bit;
            }
        }
        for (int i=0; i<xBasedMap.length;i++) {
            BitSet dataHolder = xBasedMap[i];
            int count = dataHolder.cardinality();
            finalXBasedMap[i] = new int[count+1];
            finalXBasedMap[i][0]=-1; // the image wall

            int j = 1;
            for (int bit = dataHolder.nextSetBit(0); bit >= 0; bit = dataHolder.nextSetBit(bit + 1)) {
                finalXBasedMap[i][j++] = bit;
            }
        }

        return new NeighborScanReturn(shapeOrganizer,pixelFlags,new EdgeMapHolder(finalXBasedMap,finalYBasedMap));
    }

    public ShapeGrouperReturn ConvertToPixelChunks(ShapeUnionFind shapeOrganizer, ColorChannel CR, int[] computedFlags) {
        int[] shapeHolder = shapeOrganizer.getRawData();
        int[] rawPixels = CR.getChannelData();


        IntArrayList IsolatedPixels = new IntArrayList(2048);
        PixelChunk[] returnHolder = new PixelChunk[shapeOrganizer.getTotalShapeCount()];
        int[] keyMapper = new int[ImageDataHolder.flatsize];
        int currentNextIndex = 0;
        Arrays.fill(keyMapper,-1);

        for (int pixelIndex=0; pixelIndex<shapeHolder.length; pixelIndex++) {
            int shapeID = shapeHolder[pixelIndex];
            int flag = computedFlags[pixelIndex];
            int dataKey = keyMapper[shapeID];

            if (dataKey==-1) {
                if (flag==15) {
                    IsolatedPixels.Add(pixelIndex);
                    continue;
                }

                PixelChunk PC = new PixelChunk(CR.getChannelType(),rawPixels[pixelIndex]);
                PC.StartingIndex = shapeID;
                PC.setTotalArea(shapeOrganizer.getShapeArea(shapeID));

                if (flag!=8 && flag!=12 && flag>4) {
                    PC.cornerPoints.add(new CornerPoint(pixelIndex,flag));
                    if (Integer.bitCount(flag)==3) {
                        PC.convexCorners+=2;
                    } else {
                        PC.convexCorners++;
                    }
                }

                returnHolder[currentNextIndex] = PC;
                keyMapper[shapeID] = currentNextIndex++;
            } else {
                // first bit is left
                // second bit is right
                // third bit is down
                // fourth bit is up
                if (flag!=8 && flag!=12 && flag>4) {
                    PixelChunk thisPC = returnHolder[dataKey];
                    thisPC.cornerPoints.add(new CornerPoint(pixelIndex,flag));
                    if (Integer.bitCount(flag)==3) {
                        thisPC.convexCorners+=2;
                    } else {
                        thisPC.convexCorners++;
                    }
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

    public CompressedImageRepresentative QuadralizedDataCompressed(ChunkedImageRepresentative CIR) {
        ChunkedColorChannel[] chunkedColorChannels = CIR.getChunkedChannels();

        CompressedColorChannel[] compressedColorChannels = new CompressedColorChannel[chunkedColorChannels.length];

        for (int i=0; i<chunkedColorChannels.length;i++) {
            ChunkedColorChannel CCC = chunkedColorChannels[i];

            int[] flagHolder = CCC.getComputedFlags();
            ShapeUnionFind shapeOrganizer = CCC.getShapeOrganizer();
            EdgeMapHolder cardinalMaps = CCC.getCardinalMaps();
            int[] currentIsolated = CCC.getChannelIsolatedIndexes();


            ArrayList<RectangleRepresentative> finalRectangleHolder = new ArrayList<>();
            IntArrayList finalIsolatedIndexes = new IntArrayList(currentIsolated.length);
            IntArrayList IsolatedIndexesValues = new IntArrayList(currentIsolated.length);
            ArrayList<CornerDataRepresentative> finalCornerHolder = new ArrayList<>();

            for (int isolatedIndex : currentIsolated) {
                finalIsolatedIndexes.Add(isolatedIndex);
            }
            for (int indexValue : CCC.getChannelIsolatedCorrespondingColor()) {
                IsolatedIndexesValues.Add(indexValue);
            }


            for (PixelChunk pixelChunk : CCC.getChannelChunks()) {
                int startingIndex = pixelChunk.StartingIndex;
                boolean useRectangles = PixelChunkUseRectangles(pixelChunk.convexCorners,shapeOrganizer.getContinuity(startingIndex),shapeOrganizer.getHoles(startingIndex));

                if (useRectangles) {
                    ArrayList<RectangleRepresentative> brokenChunk = RectangleCreator(pixelChunk, flagHolder, shapeOrganizer, cardinalMaps);

                    for (RectangleRepresentative rect : brokenChunk) {
                        if (rect.area() == 1) { // if size 1, its isolated
                            finalIsolatedIndexes.Add(rect.startingIndex());
                            IsolatedIndexesValues.Add(rect.value());
                        } else {
                            finalRectangleHolder.add(rect);
                        }
                    }
                } else {
                    CornerDataRepresentative holder = CalculateCornerData(pixelChunk, CCC.getComputedFlags());
                    finalCornerHolder.add(holder);
                }

            }

            // sequential sorting
            finalRectangleHolder.sort(Comparator.comparingInt(RectangleRepresentative::startingIndex));
            finalCornerHolder.sort(Comparator.comparingInt(CornerDataRepresentative::startingIndex));

            //parallel sort the isolated indexes and their values
            int[] isolatedIndexes = finalIsolatedIndexes.toIntArray();
            int[] indexValues = IsolatedIndexesValues.toIntArray();

            Integer[] arrayIndices = new Integer[isolatedIndexes.length];
            for (int j = 0; j < arrayIndices.length; j++) arrayIndices[j] = j;

            Arrays.sort(arrayIndices, Comparator.comparingInt(idx -> isolatedIndexes[idx]));

            int[] sortedKeys = new int[isolatedIndexes.length];
            int[] sortedValues = new int[indexValues.length];

            for (Integer arrayIndex : arrayIndices) {
                sortedKeys[i] = isolatedIndexes[arrayIndex];
                sortedValues[i] = indexValues[arrayIndex];
            }
            // done

            //store!
            RectangleRepresentative[] sortedRectangles = finalRectangleHolder.toArray(new RectangleRepresentative[0]);
            CornerDataRepresentative[] sortedCorners = finalCornerHolder.toArray(new CornerDataRepresentative[0]);

            CompressedColorChannel compressedColorChannel = new CompressedColorChannel(CCC.getChannelType(),sortedRectangles,sortedCorners);
            compressedColorChannel.setIsolatedIndexes(sortedKeys,sortedValues);
            compressedColorChannels[i] = compressedColorChannel;
        }

        return new CompressedImageRepresentative(CIR.isGrayScale(),CIR.isUniformMaxAlpha(),compressedColorChannels);
    }

    public CornerDataRepresentative CalculateCornerData(PixelChunk pixelChunk, int[] flagHolder) {
        int dataSize = pixelChunk.cornerPoints.size();
        int[] flagMask = new int[dataSize];
        int[] indexDelta = new int[dataSize-1];

        flagMask[0] = pixelChunk.cornerPoints.getFirst().flagmask(); // always 9?

        int prevIndex = pixelChunk.cornerPoints.getFirst().index();
        for (int i=1; i<dataSize; i++) {
            CornerPoint CP = pixelChunk.cornerPoints.get(i);
            int currentIndex = CP.index();

            indexDelta[i-1] = currentIndex-prevIndex;
            flagMask[i] = CP.flagmask();

            prevIndex = currentIndex;
        }

        return new CornerDataRepresentative(indexDelta,pixelChunk.StartingIndex,flagMask, pixelChunk.getColorValue());
    }

    public boolean PixelChunkUseRectangles(int convexCorners, int continuity, int holes) {
        return (continuity/((double) convexCorners * convexCorners))*holes <= 1; // prediction for rectangle efficiency basically
    }

    private ArrayList<RectangleRepresentative> RectangleCreator(PixelChunk pixelChunk, int[] flagHolder, ShapeUnionFind shapeOrganizer, EdgeMapHolder EMH) { // greedy algorith
        int area = pixelChunk.getTotalArea();

        ArrayList<CornerPoint> processingList = new ArrayList<>(pixelChunk.cornerPoints);
        ArrayList<RectangleRepresentative> returnHolder = new ArrayList<RectangleRepresentative>();

        BitSet unproccessedPixels = new BitSet(ImageDataHolder.flatsize);
        int[] raw = shapeOrganizer.getRawData();

        for (int currentPixelIndex=0; currentPixelIndex<raw.length; currentPixelIndex++) {
            int shapeID = raw[currentPixelIndex];
            if (shapeID==pixelChunk.StartingIndex) {
                unproccessedPixels.set(currentPixelIndex);
            }
        }

        while (area>0) {
            // find the optimal rectangle
            RectangleRepresentative currentGlobalBest = new RectangleRepresentative(0,0,1,0);

            for (CornerPoint processingCorner : processingList) {
                int flatAnchorIndex = processingCorner.index();

                int flag = processingCorner.flagmask();

                RectangleRepresentative[] longRectangles = CalculateLongRectangles(EMH, flatAnchorIndex, flag, pixelChunk);

                if (longRectangles.length==1) {
                    currentGlobalBest = longRectangles[0];
                    break;
                }

                RectangleRepresentative localKingRect = longRectangles[1]; // the initial wide rectangle (wide gets priority)

                RectangleRepresentative longRectangle = longRectangles[0];

                // wide or long?
                if ((long)longRectangle.area() * localKingRect.storageCost()>(long) localKingRect.area() * longRectangle.storageCost()) {
                    // this is the same thing as area/storage ? area/storage
                    // its just more efficient because division sucks in terms of CPU cycles
                    //however this heuristic itself mighe be bad and probably needs to be changed

                    localKingRect = longRectangle;
                }

                RectangleRepresentative squareCalculation = CalculateSquare(EMH, flatAnchorIndex, flag, pixelChunk);

                // is square better?
                if ((long)squareCalculation.area() * localKingRect.storageCost()>(long) localKingRect.area() * squareCalculation.storageCost()) {
                    localKingRect = squareCalculation;
                }

                // is this local king better than the other ones?
                if ((long)localKingRect.area() * currentGlobalBest.storageCost() > (long)currentGlobalBest.area() * localKingRect.storageCost()) {
                    currentGlobalBest = localKingRect;
                }
            }

            // process the rectangle

            area-=currentGlobalBest.area();
            processBestRectangle(currentGlobalBest, processingList, unproccessedPixels);
            returnHolder.add(currentGlobalBest);
        }

        return returnHolder;
    }

    private void processBestRectangle(RectangleRepresentative currentGlobalBest, ArrayList<CornerPoint> processingList, BitSet unproccessedPixels) {
        int w = ImageDataHolder.width;

        // 1) update bitset
        for (int y = 0; y < currentGlobalBest.height(); y++) {
            int rowStart = currentGlobalBest.startingIndex() + (y * w);
            unproccessedPixels.clear(rowStart, rowStart + currentGlobalBest.width());
        }
        // 2) remove swallowed corners
        processingList.removeIf(cp -> !unproccessedPixels.get(cp.index()));

        int[] startingCoordinates = ReverseIndexCalculator(w, currentGlobalBest.startingIndex());

        // 3) add new corners
        int right = startingCoordinates[0] + currentGlobalBest.width();
        int left = startingCoordinates[0] - 1;
        int top = startingCoordinates[1] - 1;
        int bottom = startingCoordinates[1] + currentGlobalBest.height();

        // width and height are inclusive so
        //top and bottom boundary check
        for (int x=left; x<right; x++) {
            int topTestingIndex = IndexCalculator(w,x,top);
            int bottomTestingIndex = IndexCalculator(w,x,bottom);


            if (unproccessedPixels.get(topTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(unproccessedPixels,topTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
            if (unproccessedPixels.get(bottomTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(unproccessedPixels,bottomTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
        }

        // left and right boundary check
        for (int y=top; y<bottom; y++) {
            int leftTestingIndex = IndexCalculator(w,left,y);
            int rightTestingIndex = IndexCalculator(w,right,y);


            if (unproccessedPixels.get(leftTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(unproccessedPixels,leftTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
            if (unproccessedPixels.get(rightTestingIndex)) {
                CornerPoint newCP = translateToCornerPoint(unproccessedPixels,rightTestingIndex);
                if (newCP.trueCorner()) {
                    processingList.add(newCP);
                }
            }
        }

    }

    private CornerPoint translateToCornerPoint(BitSet unproccessedPixels, int flatIndex) {
        int w = ImageDataHolder.width;
        int flagmask = 0;
        // first bit is left
        // second bit is right
        // third bit is down
        // fourth bit is up
        int left   = flatIndex - 1;
        int right  = flatIndex + 1;
        int above  = flatIndex - w;
        int below  = flatIndex + w;

        if (flatIndex % w == 0 || !unproccessedPixels.get(left)) flagmask |= 1;
        if ((flatIndex + 1) % w == 0 || !unproccessedPixels.get(right)) flagmask |= 2;
        if (below >= ImageDataHolder.flatsize || !unproccessedPixels.get(below)) flagmask |= 4;
        if (above < 0 || !unproccessedPixels.get(above)) flagmask |= 8;

        return new CornerPoint(flatIndex,flagmask);
    }

    private RectangleRepresentative[] CalculateLongRectangles(EdgeMapHolder EMH ,int flatAnchorIndex, int flag, PixelChunk PC) {
        int directionData = ~flag; // not-ing the flag gives us expansion directions
        int[] expansionLimits = CalculateLimits(directionData, flatAnchorIndex, EMH);
        int longRectangleWidth = 1;
        int wideRectangleHeight = 1;

        RectangleRepresentative[] returnHolder = new RectangleRepresentative[2];

        int testingIndex = flatAnchorIndex;
        int change = (((directionData & 1) != 0) ? -1 : 1); // -1 if im checking left, 1 if im checking right
        while (true) { // the tall rectangle, expands horizontally
            testingIndex+=change;

            if (!IndexsInSameRow(testingIndex,flatAnchorIndex) || !IndexInImage(testingIndex)) break; // DOES IT MATTER IF IT BLEEDS INTO THE NEXT ROW?

            int[] testingExpansionLimits = CalculateLimits(directionData, testingIndex, EMH);

            if (testingExpansionLimits[1]!=expansionLimits[1]) {
                break;
            } else {
                longRectangleWidth++;
            }
        }

        RectangleRepresentative longRect = new RectangleRepresentative(expansionLimits[1],longRectangleWidth,PC.StartingIndex, PC.getColorValue());

        if (longRect.area()==PC.getTotalArea()) {
            return new RectangleRepresentative[]{longRect}; // return the perfect fit
        }
        returnHolder[0] = longRect; // otherwise keep it there


        testingIndex = flatAnchorIndex;
        change = ImageDataHolder.width*(((directionData & 8) != 0) ? -1 : 1); // -height if im checking up, and height if im checking down
        while (true) { // the wide rectnagle, expands verically
            testingIndex+=change;

            if (!IndexInImage(testingIndex)) break;

            int[] testingExpansionLimits = CalculateLimits(directionData, testingIndex, EMH);

            if (testingExpansionLimits[0]!=expansionLimits[0]) {
                break;
            } else {
                wideRectangleHeight++;
            }
        }

        RectangleRepresentative wideRect = new RectangleRepresentative(wideRectangleHeight,expansionLimits[0],PC.StartingIndex, PC.getColorValue());
        //no check for wide to see if it fully fits because the long already should have
        returnHolder[1] = wideRect; // otherwise keep it there

        return returnHolder;
    }

    private RectangleRepresentative CalculateSquare(EdgeMapHolder EMH, int flatAnchorIndex, int flag, PixelChunk PC) {
        int directionData = ~flag;

        if (Integer.bitCount(directionData)<2) return new RectangleRepresentative(1,1,flatAnchorIndex,PC.getColorValue());

        int[] expansionLimits = CalculateLimits(directionData, flatAnchorIndex, EMH);
        int[] flatCoordinates = ReverseIndexCalculator(ImageDataHolder.width,flatAnchorIndex);

        int relativeIndex = flatAnchorIndex;
        int dx = (((directionData & 1) != 0) ? -1 : 1);
        int dy = ImageDataHolder.width*(((directionData & 8) != 0) ? -1 : 1);

        int size=1;
        for (; size<=Math.min(expansionLimits[0],expansionLimits[1]); size++) {
            relativeIndex+=dx+dy;

            int[] relativeExpansionLimits = CalculateLimits(flag, relativeIndex, EMH); // we actually want to calculate in the direction of the flag to check if theres any intersection

            if (!(relativeExpansionLimits[0] >= size) || !(relativeExpansionLimits[1] >= size)) {
                break;
            }
        }

        int startingIndex = flatAnchorIndex;
        if (dx<0) {
            startingIndex-=size;
        }
        if (dy<0) {
            startingIndex-=size*ImageDataHolder.width;
        }

        return new RectangleRepresentative(size,size,startingIndex,PC.getColorValue());
    }

    public int[] CalculateLimits(int calculationDirections, int flatAnchorIndex, EdgeMapHolder EMH) {
        // first bit is left
        // second bit is right
        // third bit is down
        // fourth bit is up
        int limitX = 0;
        int limitY = 0;
        int[] anchorCoordinates = ReverseIndexCalculator(ImageDataHolder.width,flatAnchorIndex);
        int nearestLocation;

        if ((calculationDirections & 1) != 0) { // left edge open
            nearestLocation = nearestCardinalEdge(EMH,false, false, anchorCoordinates[1], anchorCoordinates[0]); // right edge + searching left = dont include
            limitX = anchorCoordinates[0]-nearestLocation-1;
        } else if ((calculationDirections & 2) != 0) { // right edge open
            nearestLocation = nearestCardinalEdge(EMH,false, true, anchorCoordinates[1], anchorCoordinates[0]); // right edge + searching right = include\
            limitX = nearestLocation-anchorCoordinates[0];
        }
        if ((calculationDirections & 4) != 0) { // down open
            nearestLocation = nearestCardinalEdge(EMH,true, true, anchorCoordinates[0], anchorCoordinates[1]); // down edge + searching down = include
            limitY = nearestLocation-anchorCoordinates[1];
        } else if ((calculationDirections & 8) != 0) { // up open
            nearestLocation = nearestCardinalEdge(EMH,true, false, anchorCoordinates[0], anchorCoordinates[1]); // down edge + searching up = dont include
            limitY = anchorCoordinates[1]-nearestLocation-1;
        }

        return new int[]{limitX,limitY};
    }

    private int nearestCardinalEdge(EdgeMapHolder EMH, boolean useXmap, boolean positiveDirection, int absoluteBasis, int relativeCoordinateLocation) {
        int[] dataset = useXmap ? EMH.xBasedMap()[absoluteBasis] : EMH.yBasedMap()[absoluteBasis];

        if (dataset == null || dataset.length == 0) return -1;

        int index = Arrays.binarySearch(dataset, relativeCoordinateLocation);

        if (positiveDirection) {
            if (index>=0) { // if the current point is an edge
                return dataset[index];
            } else {
                int insertionPoint = -(index + 1); // undo java's stupid return
                return (insertionPoint < dataset.length) ? dataset[insertionPoint] : dataset[dataset.length-1];
            }
        } else {
            if (index>=0) { // if found
                return (index > 0) ? dataset[index - 1] : dataset[0];
            } else {
                int insertionPoint = -(index + 1);
                return (insertionPoint > 0) ? dataset[insertionPoint - 1] : dataset[0];
            }
        }
    }
}