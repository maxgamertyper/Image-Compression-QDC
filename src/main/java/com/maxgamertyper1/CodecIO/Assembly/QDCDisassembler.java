package com.maxgamertyper1.CodecIO.Assembly;

import com.maxgamertyper1.CodecIO.Decoding.*;
import com.maxgamertyper1.CodecIO.Encoding.DataType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class QDCDisassembler implements AutoCloseable {
    private final Parser parser;
    private final String inFileName;
    private final BufferedWriter fileStream;
    String OPCode;
    boolean inOverride = false;
    private String[] channelNames;
    private int[] colors;
    private final boolean verbose;

    public QDCDisassembler(String inFileName, String outFileName, boolean verbose) throws IOException {
        this.inFileName = inFileName;
        fileStream = new BufferedWriter(new FileWriter(outFileName));
        parser = new Parser(inFileName);
        this.verbose = verbose;
    }

    public void dissassemble() throws IOException {
        boolean isQDCFile = parser.checkFile();

        if (!isQDCFile) {
            System.out.println("Error: not QDC file");
            return;
        }

        DissassembleMetadata();
        for (int i = 0; i < channelNames.length; i++) {
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
        writeWithNewLine("; FILE: " + inFileName + "  |  SIZE: " + String.format("%,d", parser.getFileSize()) + " Bytes");

        Version compressionVersion = parser.readCompressionVersion();
        Dimensions canvas = parser.readCanvasDimensions();
        HeaderFlags flags = parser.readFlag();
        int totalChannels = parser.getTotalChannels();

        channelNames = new String[totalChannels];

        if (flags.isGrayscale()) {
            channelNames[0] = ".channel_gray";
        } else {
            channelNames[0] = ".channel_red";
            channelNames[1] = ".channel_green";
            channelNames[2] = ".channel_blue";
        }
        if (!flags.opaque()) {
            channelNames[totalChannels - 1] = ".channel_alpha";
        }

        colors = parser.readInitialColors();

        writeWithNewLine(
                "; HEADER: Version " + compressionVersion.major() + "." + compressionVersion.minor() +
                        "  |  Canvas: " + canvas.width() + "x" + canvas.height()
        );
        writeWithNewLine(
                "; FLAGS: Grayscale: " + (flags.isGrayscale() ? "TRUE" : "FALSE") +
                        "  |  Max Alpha: " + (flags.opaque() ? "TRUE" : "FALSE") +
                        "  |  Original File Flag: " + flags.originalType()
        );
        writeWithNewLine("; INITIAL COLORS: " + Arrays.toString(colors));
        writeWithNewLine("; HEADER HASH: " + formatHex(parser.readHash()));
        writeWithNewLine("; =====================================================================================");
        fileStream.newLine();
        fileStream.flush();
    }

    private void processChannelMeta(int index) throws IOException {
        writeWithNewLine("SECTION " + channelNames[index] + "  LOCATION  " + formatHex(parser.cursorLocation().byteLocation()));
        writeWithNewLine("SECTION " + channelNames[index] + "   INITIAL   " + colors[index]);
        writeHeader();
        parser.logCursorLocaiton();

        ChannelSizeInfo channelSizeInfo = parser.readChannelSizeData();
        writeWithLocation(
                "[CHANNEL_META] Payload Length: " + channelSizeInfo.payloadSize() +
                        "; Trailing Bits: " + channelSizeInfo.trailingBits()
        );

        parser.loadUnaryTable();
        int[] unaryTable = parser.getUnaryTable();

        writeWithLocation("[UNARY_TABLE]  Table Count: " + unaryTable.length + "; Table: " + Arrays.toString(unaryTable));

        writeOPCodeSwitch(parser.readExtendedOPCode(), false);

        fileStream.flush();
    }

    private void writeWithLocation(String text) throws IOException {
        StreamLocation loggedDistance = parser.getLoggedDistance();
        StreamLocation loggedCursor = parser.getLoggedPosition();


        fileStream.write(
                formatHex(loggedCursor.byteLocation()) + " | Bit: " + formatInt(loggedCursor.bitLocation()) +
                        " | Length: " + formatInt((int) loggedDistance.totalBits())
        );

        parser.logCursorLocaiton();

        fileStream.write("  " + text);
        fileStream.newLine();
    }

    private void writeHeader() throws IOException {
        fileStream.newLine();
        fileStream.write(" BYTE_S  |  BIT_S  |  V_LENGTH    OPERATION          VALUE");
        fileStream.newLine();
    }

    private String formatInt(int number) {
        return String.format("%02d", number);
    }

    private void writeOPCodeSwitch(DataType currentType, boolean inOverride) throws IOException {
        String ovr = "";
        if (inOverride) {
            ovr = "OVR_";
        }
        switch (currentType) {
            case Raw -> {
                OPCode = "[" + ovr + "OP_RAW]       ";
            }
            case Rectangle -> {
                OPCode = "[" + ovr + "OP_RECT]      ";
            }
            case Corner -> {
                OPCode = "[" + ovr + "OP_CORNER]   ";
            }
            default -> OPCode = "[" + ovr + "OP_UNKNOWN]  ";
        }

        writeWithLocation("[SWITCH_OP]    " + OPCode);
    }

    private void writeWithNewLine(String text) throws IOException {
        fileStream.write(text);
        fileStream.newLine();
    }

    private String formatHex(long value) {
        return String.format("0x%06X", value & 0xFFFFFFFFL);
    }

    private void processChannelData() throws IOException {
        parser.logCursorLocaiton();
        DataType newType;
        int escapeBranch = 0;


        while (parser.hasMoreBits()) { // compare ending location and current, if theres more, continue
            //processes 1 "shape" per loop

            if (parser.prepareNextLength()) { // set the value length and return if its an escape or not
                //ESCAPE code (not overflow, already handled)
                escapeBranch = parser.processEscape(); // NOTE: maybe later
                newType = parser.getCurrentType();
                inOverride = parser.isInOverride();

                writeOPCodeSwitch(newType, inOverride);
            } else {
                int[] shapeValues = parser.processShape();
                writeValueRead(shapeValues, parser.getCurrentType(), parser.isInOverride());

                if (shapeValues[shapeValues.length - 1] == 1 && parser.getCurrentType() == DataType.Corner) { // if override
                    parser.updateOverrideData(true);
                    writeOPCodeSwitch(parser.getCurrentType(), true);
                }
            }
        }
        fileStream.flush();
    }

    private void channelTail() throws IOException {
        writeWithNewLine("; =====================================================================================");
        writeWithNewLine("; Channel Hash: " + formatHex(parser.readHash()));
        writeWithNewLine("; =====================================================================================");
        parser.logCursorLocaiton();
        fileStream.flush();
    }

    private void writeValueRead(int[] values, DataType currentType, boolean override) throws IOException {
        String ovr = "";
        int offset = 0;
        if (override) {
            offset = 2;
            ovr = "Corner Delta: (" + values[0] + ", " + values[1] + "); ";
        }
        switch (currentType) {
            case Raw -> {
                writeWithLocation(OPCode + ovr + "Paeth Value: " + values[offset]);
            }
            case Rectangle -> {
                writeWithLocation(OPCode + ovr + values[1 + offset] + "x" + values[2 + offset] + " RECT: " + values[offset]);
            }
            case Corner -> {
                writeWithLocation(OPCode + ovr + formatCornerData(values));
            }
        }
        fileStream.flush();
    }

    private String formatCornerData(int[] value) {
        StringBuilder returnHolder = new StringBuilder();
        int offset = 0;
        if (inOverride) {
            offset = 2;
        }


        returnHolder.append("Paeth Value: ").append(value[offset]).append("; Override: ").append((value[value.length - 1] == 1))
                .append("; Corner Deltas: [");

        for (int i = 1; i < value.length - 1; i++) {
            int val = value[i];

            if ((i - 1 + offset) % 2 == 0) { // -1 from the color being the first
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
        writeWithNewLine("SECTION   .tail   LOCATION  " + formatHex(parser.cursorLocation().byteLocation()));
        fileStream.newLine();

        writeWithNewLine("; A SEEK of 0x000000 means the channel does not exist");
        fileStream.newLine();

        writeWithLocation("     [RED_SEEK]       " + formatHex(parser.readSeekEntry()));
        writeWithLocation("    [GREEN_SEEK]      " + formatHex(parser.readSeekEntry()));
        writeWithLocation("    [BLUE_SEEK]       " + formatHex(parser.readSeekEntry()));
        writeWithLocation("    [GRAY_SEEK]       " + formatHex(parser.readSeekEntry()));
        writeWithLocation("    [ALPHA_SEEK]      " + formatHex(parser.readSeekEntry()));

        writeWithLocation("  [FILE_INTEGRITY]    " + formatHex(parser.readSeekEntry()));
        writeWithLocation("[FILE_END_SIGNATURE]  " + formatHex(parser.readSeekEntry()));
        writeWithNewLine("; means MAX! in ASCII");
        fileStream.flush();
    }

    @Override
    public void close() throws IOException {
        fileStream.flush();
    }
}
