package com.maxgamertyper1.datatypes.records;

import com.maxgamertyper1.datatypes.PixelChunk;

import java.util.BitSet;

public record FindChunkReturn(PixelChunk PixelChunk, BitSet visited) { }