package com.maxgamertyper1.compressor;

import com.maxgamertyper1.datatypes.ShapeUnionFind;
import com.maxgamertyper1.datatypes.records.EdgeMapHolder;

public record NeighborScanResult(ShapeUnionFind Shapes, byte[] Flags, EdgeMapHolder CardinalMap) {
}
