package com.maxgamertyper1.CodecIO;

public enum DataType {
    Raw(0), //00
    Rectangle(2), // 10
    Corner(3); // 11

    private final int opCode;

    // Enum constructor (implicitly private)
    DataType(int opCode) {
        this.opCode = opCode;
    }

    public int getOpCode() {
        return opCode;
    }

    public int getSwitchBit(DataType next) {
        return switch (this) {
            case Raw -> (next == Rectangle) ? 1 : 0;
            case Rectangle -> (next == Corner) ? 1 : 0;
            case Corner -> (next == Raw) ? 1 : 0;
            default -> -1;
        };
    }
}