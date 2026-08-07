package com.maxgamertyper1.CodecIO.Decoding;

import com.maxgamertyper1.CodecIO.Encoding.DataType;
import com.maxgamertyper1.datatypes.IntArrayList;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Disassembler implements AutoCloseable{
    private final FormattedReader reader;
    private final String inFileName;
    private final BufferedWriter fileStream;
    private String[] channelNames;
    private int[] unaryLengths;
    private DataType currentType;
    private int currentChannelLength;
    private int trailingBits;
    private int dataLength = 1;
    String OPCode;
    private int[] colors;
    private boolean override = false;
    private long storedByte = 0;
    private int storedBit = 0;

    public Disassembler(String inFileName, String outFileName) throws IOException {
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

        readLongOPcode();

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

    private void readLongOPcode() throws IOException {
        int startingCode = reader.readBits(2);
        currentType = startingCode == 0 ? DataType.Raw : startingCode == 2 ? DataType.Rectangle : DataType.Corner;
        writeOPCodeSwitch(2);
    }

    private void writeLocationHead(int bitsRead) throws IOException {
        long byteEnd = reader.getTotalBytesRead();
        int bitEnd = reader.getTotalBitsRead();

        long byteStart;
        int bitStart;


        if (bitsRead == -1) {
            bitStart = storedBit;
            byteStart = storedByte;

            bitsRead = (int)((byteEnd - byteStart) * 8) + bitEnd - bitStart;
        } else {
            byteStart = byteEnd - bitsRead / 8;
            bitStart = bitEnd - (bitsRead & 7);

            if (bitStart<0) {
                byteStart--;
                bitStart+=8;
            }
        }

        fileStream.write(
                formatHex(byteStart) + " | Bit: " + formatInt(bitStart) +
                " | Length: " + formatInt(bitsRead)
        );
    }

    private void logCursorLocaiton() {
        storedBit = reader.getTotalBitsRead();
        storedByte = reader.getTotalBytesRead();
    }

    private String formatInt(int number) {
        return String.format("%02d", number);
    }

    private void writeOPCodeSwitch(int bitsRead) throws IOException {
        String ovr = "";
        if (override) {
            ovr = "OVR_";
        }
        switch (currentType) {
            case Raw -> {
                OPCode = "[" + ovr + "OP_RAW]       ";
                dataLength = 1;
            }
            case Rectangle -> {
                OPCode = "[" + ovr + "OP_RECT]      ";
                dataLength = 3;
            }
            case Corner -> {
                OPCode = "[" + ovr + "OP_CORNER]   ";
                dataLength = -1;
            }
            default -> OPCode = "[" + ovr + "OP_UNKNOWN]  ";
        }

        writeWithLocation("[SWITCH_OP]    " + OPCode,bitsRead);
        logCursorLocaiton();
    }

    private void writeWithNewLine(String text) throws IOException {
        fileStream.write(text);
        fileStream.newLine();
    }

    private String formatHex(long value) {
        return String.format("0x%06X", value);
    }

    private void processChannelData() throws IOException {
        boolean previousWasTypeEnd = true;
        int thisPrefix = 0;
        int bitsToRead = 0;
        int value = 0;
        int iDataLength = dataLength;
        IntArrayList values = new IntArrayList(4);
        boolean justLeftOverride = false;
        logCursorLocaiton();

        long targetTotalBits = (currentChannelLength * 8L) - trailingBits;

        while ((reader.getByteTracker() * 8L + reader.getBitsTracker()) < targetTotalBits) {
            thisPrefix = reader.readUnary();

            System.out.println("Prefix: " + ((1 << (thisPrefix + 1)) - 2));

            if (thisPrefix == unaryLengths.length-1) {
                bitsToRead = checkEscape();

                if (bitsToRead == -1) { // escape code

                    if (currentType == DataType.Corner && !override && !justLeftOverride) {
                        int oValue = reader.readBits(1);
                        override = oValue == 1;
                        values.Add(oValue);

                        writeValueRead(values.toIntArray());
                        System.out.println("Writing Corner data");

                        readLongOPcode();
                    } else if (override) {
                        dataLength = -1;
                        currentType = DataType.Corner;
                        override = false;
                        justLeftOverride= true;
                        writeOPCodeSwitch(-1);

                        System.out.println("Left Override");

                    } else {
                        processSwitchOP();
                        System.out.println("ESCAPE");
                        fileStream.flush();
                    }

                    previousWasTypeEnd = true;
                    iDataLength = dataLength + (override ? 2 : 0);
                    values.Clear();
                    continue;
                }

                System.out.println("OVERFLOW");
            } else {
                bitsToRead = unaryLengths[thisPrefix];
            }


            if (previousWasTypeEnd || currentType == DataType.Corner || (override && iDataLength > dataLength-2)) { // read the color
                value = reader.readZigZag(bitsToRead);
            } else {
                value = reader.readBits(bitsToRead);
            } // figure out how to do this for corners!

            System.out.println("Value: " + value);

            values.Add(value);

            iDataLength--;
            previousWasTypeEnd = false;
            justLeftOverride= false;
            if (iDataLength == 0) {
                System.out.println("Writing data");
                previousWasTypeEnd = true;
                iDataLength = dataLength + (override ? 2 : 0);
                writeValueRead(values.toIntArray());
                logCursorLocaiton();
                values.Clear();
            }
        }
        reader.jumpToByteBoundary(); // eat them

        fileStream.flush();
    }

    private void processSwitchOP() throws IOException {
        int OPCode = reader.readBits(1);
        System.out.println("OPCODE: " + OPCode + " Chunk: " + currentType);
        currentType = currentType.getNextType(OPCode);
        writeOPCodeSwitch(-1);
    }

    private int checkEscape() throws IOException {
        int isEscapeFlag = reader.readBits(1);
        if (isEscapeFlag == 1) {
            return -1;
        }
        return reader.readBits(7);
    }

    private void channelTail() throws IOException {
        writeWithNewLine("; =====================================================================================");
        writeWithNewLine("; Channel Hash: " + formatHex(reader.readBytes(4)));
        writeWithNewLine("; =====================================================================================");
        fileStream.flush();
    }

    private void writeValueRead(int[] values) throws IOException {
        String ovr = "";
        int offset = 0;
        if (override) {
            offset = 2;
            ovr = "Corner Delta: (" + values[0] + ", " + values[1] + "); ";
        }
        switch (currentType) {
            case Raw -> {
                writeWithLocation(OPCode + ovr + "Paeth Value: " + values[offset], -1);
            }
            case Rectangle -> {
                writeWithLocation(OPCode + ovr + values[1+offset] + "x" + values[2+offset] + " RECT: " + values[offset], -1);
            }
            case Corner -> {
                writeWithLocation(OPCode + ovr + formatCornerData(values), -1);
            }
        }
        fileStream.flush();
    }

    private String formatCornerData(int[] value) {
        StringBuilder returnHolder = new StringBuilder();
        int offset = 0;
        if (override) {
            offset = 2;
        }


        returnHolder.append("Paeth Value: ").append(value[offset]).append("; Override: ").append((value[value.length - 1] == 1))
                .append("; Corner Deltas: [");

        for (int i=1; i < value.length - 1; i++) {
            int val = value[i];

            if ((i-1 + offset)%2==0) { // -1 from the color being the first
                returnHolder.append("(").append(val).append(", ");
            } else {
                returnHolder.append(val).append(")").append(", ");
            }
        }

        returnHolder.delete(returnHolder.length() - 2, returnHolder.length());
        returnHolder.append("]");

        return returnHolder.toString();
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

    @Override
    public void close() throws IOException {
        fileStream.flush();
    }
}
