package com.maxgamertyper1.datatypes;

public enum ChannelColorTypes {
    Red(0),
    Green(1),
    Blue(2),
    Gray(3),
    Alpha(4);

    private final int value;

    ChannelColorTypes(int value) {
        this.value = value;
    }

    public int getOrderValue() {
        return value;
    }
}
