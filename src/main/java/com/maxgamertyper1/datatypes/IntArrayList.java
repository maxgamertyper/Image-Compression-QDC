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

    public int Size() {
        return size;
    }

    public void Clear() {
        size=0;
    }

    public void Remove(int value) {
        for (int i = 0; i < size; i++) {
            if (InternalArray[i] == value) {

                int numMoved = size - i - 1;
                if (numMoved > 0) {
                    System.arraycopy(InternalArray, i + 1, InternalArray, i, numMoved);
                }

                size--;
            }
        }
    }

    public int atIndex(int index) {
        return InternalArray[index];
    }

    public int[] toIntArray() {
        return Arrays.copyOf(InternalArray, size);
    }

    private void ensureCapacity(int size) {
        InternalArray = Arrays.copyOf(InternalArray,size);
    }

    public void removeDuplicatesUnsorted() {
        if (size <= 1) return;

        Arrays.sort(InternalArray, 0, size); // sort

        int uniqueIndex = 0;
        for (int i = 1; i < size; i++) { // collapse it together
            if (InternalArray[i] != InternalArray[uniqueIndex]) {
                uniqueIndex++;
                InternalArray[uniqueIndex] = InternalArray[i];
            }
        }

        size = uniqueIndex + 1; // change the size
    }
}