package com.maxgamertyper1.CodecIO.Decoding;

import com.maxgamertyper1.compressor.PaethProcessor;
import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.OriginalFileType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class QDCDecoder implements AutoCloseable {
    private static final Version DECODER_VERSION = new Version(0,3);
    private final boolean verbose;

    private final String outFileName;
    private File outFile;
    private final Parser QDCParser;

    private int[] initialPixels;
    private int[] imagePixels;
    private ImageMeta metaData;
    private int channelCount;

    public QDCDecoder(String inFileName, String outFileName, boolean verbose) throws IOException {
        this.outFileName = outFileName;
        QDCParser = new Parser(inFileName);
        boolean isQDCFile = QDCParser.checkFile();
        this.verbose = verbose;

        if (!isQDCFile) throw new IOException("File is not a valid QDC file. File may be corrupt or missing");
    }

    public void decode() throws DataFormatException, IOException {
        metaData = getMetaData();

        boolean validMetaData = checkMetaDataHash();
        if (!validMetaData) throw new DataFormatException("File metdata hash did not match calculated hash, abandoning");

        imagePixels = new int[metaData.flatsize()];

        if (metaData.opaque()) {
            Arrays.fill(imagePixels, 0xFF000000);   // fill alpha channel with 255
        }

        for (int i = 0; i < channelCount; i++) {
            ChannelColorTypes channelColor = ChannelColorTypes.getChannelColor(metaData.grayscale(),metaData.opaque(),i);

            ChannelDecoder channelManager = new ChannelDecoder(metaData, QDCParser); // trying to make it easy to thread later
            channelManager.processChannelMeta();
            channelManager.processChannelData();
            channelManager.processChannelTail();

            int[] pixelData = channelManager.getChannelColorData();
            PaethProcessor.paethDecode(pixelData,initialPixels[i], metaData.width()); // in-place

            concatenateChannelData(pixelData, channelColor);
        }
        processTail();
        finalizeImageFile();
    }

    private ImageMeta getMetaData() throws IOException {
        Version compressionVersion = QDCParser.readCompressionVersion();
        if (!DECODER_VERSION.matches(compressionVersion)) throw new IOException(
                "Compression version is not supported; Decoding Version: " +
                        DECODER_VERSION.major() + "." + DECODER_VERSION.minor()
                        + " Received: " + compressionVersion.major() + "." + compressionVersion.minor()
        );

        Dimensions canvas = QDCParser.readCanvasDimensions();
        HeaderFlags flags = QDCParser.readFlag();
        channelCount = QDCParser.getTotalChannels(); // auto calculates it

        OriginalFileType fileType = OriginalFileType.values()[flags.originalType()];
        if (fileType==OriginalFileType.Unknown) fileType = OriginalFileType.PNG; // default to png

        outFile = new File(outFileName+"."+fileType.name()); // concatenate to XYZ.png / .jpg

        initialPixels = QDCParser.readInitialColors();


        return new ImageMeta(
                canvas.height(),
                canvas.width(),
                flags.isGrayscale(),
                flags.opaque(),
                fileType,
                outFileName
        );
    }

    private boolean checkMetaDataHash() throws IOException {
        int metaHash = QDCParser.readHash();

        System.out.println("HASH CHECKING NEEDS IMPLEMENTATION");
        return true;
    }

    private void processTail() throws IOException {
        System.out.println("Tail Processing really should have seeks calculated at the start and then the hash is all");
    }

    private void concatenateChannelData(int[] channelData, ChannelColorTypes channelColor) throws IOException {
        //ARGB
        if (channelColor==ChannelColorTypes.Gray) {
            concatenateGrayscaleData(channelData);
            return;
        }

        int bitShiftCount;

        switch (channelColor) {
            case Alpha -> bitShiftCount = 24;
            case Red -> bitShiftCount = 16;
            case Green -> bitShiftCount = 8;
            case Blue  -> bitShiftCount = 0;
            default -> throw new IllegalArgumentException("erm how did you even get an unknown channel color this far?");
        }

        for (int i=0; i < imagePixels.length ; i++) {
            imagePixels[i] |= (channelData[i] & 0xFF) << bitShiftCount;
        }
    }

    private void concatenateGrayscaleData(int[] grayscaleData) throws IOException {
        for (int i=0; i < imagePixels.length ; i++) {
            imagePixels[i] |= (grayscaleData[i] & 0xFF) * 0x010101; // duplicates the byte over the first 3 aka RGB
        }
    }

    private void finalizeImageFile() throws IOException {
        int width = metaData.width();
        int height = metaData.height();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, imagePixels, 0, width);

        ImageIO.write(image, metaData.fileType().name().toLowerCase(), outFile);
    }

    @Override
    public void close() {
        System.out.println("Erm, an error occured, idk what should go here tbh");
    }
}
