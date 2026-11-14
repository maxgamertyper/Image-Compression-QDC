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

    public static ChannelColorTypes getChannelColor(boolean grayscale, boolean opaque, int index) {
        int colorChannels = grayscale ? 1 : 3;

        if (index == colorChannels) {
            return ChannelColorTypes.Alpha;
        }
        if (grayscale) {
            return ChannelColorTypes.Gray;
        }
        return switch (index) {
            case 0 -> ChannelColorTypes.Red;
            case 1 -> ChannelColorTypes.Green;
            default -> ChannelColorTypes.Blue;
        };
    }
}
