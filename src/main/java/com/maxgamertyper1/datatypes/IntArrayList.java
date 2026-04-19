package com.maxgamertyper1.datatypes;

import java.util.Arrays;

public class IntArrayList {
    private int[] InternalArray;
    private int size;

    public IntArrayList(int InitialSize) {
        this.InternalArray = new int[InitialSize];
        this.size = 0;
    }

    public void Add(int data) {
        if (size==InternalArray.length) {
            ensureCapacity(size + (size >> 1) + 1);
        }
        InternalArray[size++]=data;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0; // Extremely fast: we just reset the pointer, no GC needed
    }

    public int[] toIntArray() {
        return Arrays.copyOf(InternalArray, size);
    }

    private void ensureCapacity(int size) {
        InternalArray = Arrays.copyOf(InternalArray,size);
    }
}
