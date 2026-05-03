package com.maxgamertyper1.datatypes.records;

import com.maxgamertyper1.datatypes.IntArrayList;
import com.maxgamertyper1.datatypes.PixelChunk;

import java.util.ArrayList;

public record GetChunksReturn(IntArrayList[] IsolatedIndexes, ArrayList<PixelChunk>[] pixelChunks) {
}
