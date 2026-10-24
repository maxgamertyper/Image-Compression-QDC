package com.maxgamertyper1.datatypes;

public class IntArrayDeque {
    private final int[] arrayQueue;
    private final int capacity;
    private int head;
    private int tail;
    private int size = 0;

    public IntArrayDeque(int capacity) {
        this.capacity = capacity;
        this.arrayQueue = new int[capacity];
    }

    public void enqueue(int value) {
        if (size == capacity) throw new IllegalStateException("Queue is full");
        arrayQueue[tail] = value;
        tail++;
        if (tail == capacity) { //  i think this is better than modulo theoretically but i really dk
            tail = 0;
        }
        size++;
    }

    public int dequeue() {
        if (size == 0) throw new IllegalStateException("Queue is empty!");
        int data = arrayQueue[head];
        head++;
        if (head == capacity) { //  i think this is better than modulo theoretically but i really dk
            head = 0;
        }

        size--;
        return data;
    }

    public int getSize() {
        return size;
    }
}
