package com.maxgamertyper1.CodecIO.Decoding;

import com.maxgamertyper1.CodecIO.Encoding.DataType;
import com.maxgamertyper1.datatypes.IntArrayList;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Disassembler {
    private final FormattedReader reader;
    private final String inFileName;
    private final String outFileName;
    private final BufferedWriter fileStream;
    private String[] channelNames;
    private int[] unaryLengths;
    private DataType currentType;
    private int currentChannelLength;
    private int trailingBits;
    private boolean previousWasTypeEnd;
    private int dataLength = 1;
    String OPCode;
    private int[] colors;

    public Disassembler(String inFileName, String outFileName) throws IOException {
        this.outFileName = outFileName;
        this.inFileName = inFileName;
        reader = new FormattedReader(inFileName);
        fileStream = new BufferedWriter(new FileWriter(outFileName));
    }

    public void Dissassemble() throws IOException {
        boolean isQDCFile = reader.correctType();

        if (!isQDCFile) {
            System.out.println("Error: not QDC file");
        }

        DissassembleMetadata();
        for (int i=0; i < channelNames.length; i++) {
            processChannelMeta(i);
            processChannelData();
            channelTail();
            fileStream.newLine();
            fileStream.newLine();
            fileStream.newLine();
        }
        processTail();
    }

    private void DissassembleMetadata() throws IOException {
        writeWithNewLine("; =====================================================================================");
        writeWithNewLine("; FILE: " + inFileName + "  |  SIZE: " + reader.getFileSize());

        int majorVersion = reader.readBytes(1);
        int minorVersion = reader.readBytes(1);
        int canvasWidth = reader.readBytes(2);
        int canvasHeight = reader.readBytes(2);
        boolean isGrayscale = reader.readBits(1) == 1;
        boolean isMaximumAlpha = reader.readBits(1) == 1;
        int originalType = reader.readBits(2);


        int totalChannels = isGrayscale ? 2 : 4;
        totalChannels -= isMaximumAlpha ? 1 : 0;
        channelNames = new String[totalChannels];

        if (isGrayscale) {
            channelNames[0] = ".channel_gray";
        } else {
            channelNames[0] = ".channel_red";
            channelNames[1] = ".channel_green";
            channelNames[2] = ".channel_blue";
        }
        if (!isMaximumAlpha) {
            channelNames[totalChannels-1] = ".channel_alpha";
        }
        colors = new int[totalChannels];

        for (int i=0; i < totalChannels; i++) {
            colors[i] = reader.readBytes(1);
        }

        writeWithNewLine("; HEADER: Version " + majorVersion + "." + minorVersion + "  |  Canvas: " + canvasWidth + "x" + canvasHeight);
        writeWithNewLine(
                "; FLAGS: Grayscale: " + (isGrayscale ? "TRUE" : "FALSE") +
                "  |  Max Alpha: " + (isMaximumAlpha ? "TRUE" : "FALSE") +
                "  |  Original File Flag: " + originalType
        );
        reader.readBits(4);
        writeWithNewLine("; INITIAL COLORS: " + Arrays.toString(colors));
        writeWithNewLine("; HEADER HASH: "+ formatHex(reader.readBytes(4)));
        writeWithNewLine("; =====================================================================================");
        fileStream.newLine();
        fileStream.flush();
    }

    private void processChannelMeta(int index) throws IOException {
        writeWithNewLine("SECTION " + channelNames[index] + "  LOCATION  " + formatHex(reader.getTotalBytesRead()));
        writeWithNewLine("SECTION " + channelNames[index] + "   INITIAL   " + colors[index]);
        writeHeader();

        currentChannelLength = reader.readBytes(4);
        writeWithLocation("[SIZE]         Payload Length: " + currentChannelLength,32);
        trailingBits = reader.readBytes(1);
        writeWithLocation("[TRAILING]     Bit Trailing Count: " + trailingBits,8);
        reader.resetTracker();

        int entries = reader.readBits(4);
        unaryLengths = new int[entries];
        writeWithLocation("[UNARY_TABLE]  Table Count: " + entries,4);
        for (int i=0; i < entries; i++) {
            int value = reader.readBits(4);
            writeWithLocation("[UNARY_TABLE]  Entry #" + i + ": " + value,4);
            unaryLengths[i] = value;
        }

        int startingCode = reader.readBits(2);
        currentType = startingCode == 0 ? DataType.Raw : startingCode == 2 ? DataType.Rectangle : DataType.Corner;
        writeOPCodeSwitch();

        fileStream.flush();
    }

    private void writeWithLocation(String text, int bitsRead) throws IOException {
        writeLocationHead(bitsRead);
        fileStream.write("  " + text);
        fileStream.newLine();
    }

    private void writeHeader() throws IOException {
        fileStream.newLine();
        fileStream.write(" BYTE_S  |  BIT_S  |  V_LENGTH    OPERATION          VALUE");
        fileStream.newLine();
    }

    private void writeLocationHead(int bitsRead) throws IOException {
        long byteEnd = reader.getTotalBytesRead();
        int bitEnd = reader.getTotalBitsRead();

        long byteStart = byteEnd - bitsRead / 8;
        int bitStart = bitEnd - (bitsRead & 7);

        if (bitStart<0) {
            byteStart--;
            bitStart+=8;
        }

        fileStream.write(
                formatHex(byteStart) + " | Bit: " + formatInt(bitStart) +
                " | Length: " + formatInt(bitsRead)
        );
    }

    private String formatInt(int number) {
        return String.format("%02d", number);
    }

    private void writeOPCodeSwitch() throws IOException {
        switch (currentType) {
            case Raw -> {
                OPCode = "[OP_RAW]       ";
                dataLength = 1;
            }
            case Rectangle -> {
                OPCode = "[OP_RECT]      ";
                dataLength = 3;
            }
            case Corner -> {
                OPCode = "[OP_CORNER]    ";
                dataLength = -1;
            }
            default -> OPCode = "[OP_UNKNOWN]  ";
        }
        writeWithLocation("[SWITCH_OP]    " + OPCode,2);
    }

    private void writeWithNewLine(String text) throws IOException {
        fileStream.write(text);
        fileStream.newLine();
    }

    private String formatHex(long value) {
        return String.format("0x%06X", value);
    }

    private void processChannelData() throws IOException {
        previousWasTypeEnd = true;
        int thisPrefix = 0;
        int bitsToRead = 0;
        int value = 0;
        int iDataLength = dataLength;
        int bitsRead = 0;
        IntArrayList values = new IntArrayList(4);

        long targetTotalBits = (currentChannelLength * 8L) - trailingBits;

        while ((reader.getByteTracker() * 8L + reader.getBitsTracker()) < targetTotalBits) {
            thisPrefix = reader.readUnary();

            if (thisPrefix == unaryLengths.length-1) {
                bitsToRead = reader.readBits(8);
            }

            bitsToRead = unaryLengths[thisPrefix];

            if (previousWasTypeEnd) {
                value = reader.readZigZag(bitsToRead);
            } else {
                value = reader.readBits(bitsToRead);
            } // figure out how to do this for corners!

            bitsRead += (thisPrefix + bitsToRead + 1);
            values.Add(value);

            iDataLength--;
            previousWasTypeEnd = false;
            if (iDataLength==0) {
                previousWasTypeEnd = true;
                iDataLength = dataLength;
                writeValueRead(values.toIntArray(),bitsRead);
                values.Clear();
            }
        }
        reader.jumpToByteBoundary(); // eat them

        fileStream.flush();
    }

    private void channelTail() throws IOException {
        writeWithNewLine("; =====================================================================================");
        writeWithNewLine("; Channel Hash: " + formatHex(reader.readBytes(4)));
        writeWithNewLine("; =====================================================================================");
        fileStream.flush();
    }

    private void writeValueRead(int[] values, int bitsRead) throws IOException {
        switch (currentType) {
            case Raw -> {
                writeWithLocation(OPCode + "Paeth Value: " + values[0], bitsRead);
            }
            case Rectangle -> {
                writeWithLocation(OPCode + values[1] + "x" + values[2] + " RECT: " + values[0], bitsRead);
            }
            case Corner -> {
                System.out.println("NOT IMPLEMENTED");
            }
        }

    }

    private void processTail() throws IOException {
        writeWithNewLine("SECTION   .tail   LOCATION  " + formatHex(reader.getTotalBytesRead()));
        fileStream.newLine();

        writeWithNewLine("; A SEEK of 0x00000000 means the channel does not exist");
        fileStream.newLine();

        writeWithLocation("     [RED_SEEK]       " + formatHex(reader.readBytes(4)),32);
        writeWithLocation("    [GREEN_SEEK]      " + formatHex(reader.readBytes(4)),32);
        writeWithLocation("    [BLUE_SEEK]       " + formatHex(reader.readBytes(4)),32);
        writeWithLocation("    [GRAY_SEEK]       " + formatHex(reader.readBytes(4)),32);
        writeWithLocation("    [ALPHA_SEEK]      " + formatHex(reader.readBytes(4)),32);

        writeWithLocation("  [FILE_INTEGRITY]    " + formatHex(reader.readBytes(4)),32);
        writeWithLocation("[FILE_END_SIGNATURE]  " + formatHex(reader.readBytes(4)),32);
        writeWithNewLine("; means MAX! in ASCII");
        fileStream.flush();
    }
}
