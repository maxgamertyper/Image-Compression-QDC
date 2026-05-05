package com.maxgamertyper1.datatypes.records;

import com.maxgamertyper1.datatypes.IntArrayList;
import com.maxgamertyper1.datatypes.PixelChunk;
import com.maxgamertyper1.datatypes.ShapeUnionFind;

public record ShapeGrouperReturn(PixelChunk[] Chunks, IntArrayList IsolatedIndexes) {
}
