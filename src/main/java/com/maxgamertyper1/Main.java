package com.maxgamertyper1;

import com.maxgamertyper1.CodecIO.Assembly.QDCDisassembler;
import com.maxgamertyper1.CodecIO.Decoding.QDCDecoder;
import com.maxgamertyper1.CodecIO.Encoding.QDCEncoder;
import com.maxgamertyper1.compressor.ChunkProcessor;
import com.maxgamertyper1.compressor.CompressionConfig;
import com.maxgamertyper1.compressor.PaethProcessor;
import com.maxgamertyper1.compressor.SpatialCompressor;
import com.maxgamertyper1.datatypes.*;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import com.maxgamertyper1.imagereading.ImageMeta;
import com.maxgamertyper1.imagereading.ImageReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.DataFormatException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) throws IOException, DataFormatException {
        if (args.length == 0) {
            printHelp();
            System.exit(2);
        }
        boolean verbose = false;
        boolean overwrite = false;
        Path in = null, out = null;
        String mode = args[0];

        for (int i = 1; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-verbose" -> Logger.setLevel(Logger.VERBOSE_LEVEL);
                case "-dump" -> Logger.setLevel(Logger.DUMP_LEVEL);
                case "-in"  -> in  = resolvePath(valueAfter(args, i++));
                case "-out" -> out = resolvePath(valueAfter(args, i++));
                case "-overwrite" -> overwrite = true;
                case "-h", "--help" -> {
                    printHelp();
                    return;
                }
                default -> fail("Unknown option: " + args[i]);
            }
        }
        if (in == null) {fail("Missing -in <file>"); return;}

        Logger.info("Using Info output!");
        Logger.verbose("Using Verbose output!");
        Logger.dump("Using Dump output!", () -> "");

        Logger.logTime();
        switch (mode) {
            case "encode"      -> encode(in, out, overwrite);
            case "decode"      -> decode(in, out, overwrite);
            case "disassemble" -> disassemble(in, out, overwrite);
            default            -> fail("Unknown command: " + mode);
        }
    }

    private static void checkConflictingPaths(Path inPath, Path outPath, boolean overwrite) throws IOException {
        if (Files.exists(inPath) && Files.exists(outPath) && Files.isSameFile(inPath, outPath))
            throw new IOException("Output must differ from input: " + outPath);
        if (Files.exists(outPath) && !overwrite)
            throw new IOException("Output already exists (use -overwrite): " + outPath);

        Files.createDirectories(outPath.getParent());
    }

    private static String valueAfter(String[] args, int i) {
        if (i + 1 >= args.length) fail(args[i] + " needs a value");
        return args[i + 1];
    }

    private static void fail(String message) {
        System.err.println(message);
        printHelp();
        System.exit(2);
    }

    private static Path resolvePath(String raw) {
        Path p = Path.of(raw.trim());
        return p.toAbsolutePath().normalize();
    }

    static Path defaultOutput(Path in, String newExtension) {
        String name = in.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return Path.of(base + newExtension).toAbsolutePath().normalize();
    }

    public static void encode(Path inFilePath, Path outFilePath, boolean overwrite) throws IOException {
        if (outFilePath == null) outFilePath = defaultOutput(inFilePath,"qdc");
        checkConflictingPaths(inFilePath, outFilePath, overwrite);

        File imageFile = new File(inFilePath.toUri());
        ImageReader reader = new ImageReader(imageFile);
        ImageRepresentative image = reader.readImageFile();

        ImageMeta metaData = image.getMetaData();

        Logger.verbose(
            "Grayscale: " + metaData.grayscale() + '\n' +
            "Opaque: " + metaData.opaque() + '\n' +
            "Height: " + metaData.height() + '\n' +
            "Width: " + metaData.width() + '\n' +
            "Flatsize: " + metaData.flatsize() + '\n' +
            "originalName: " + metaData.metaImageName() + '\n' +
            "originalType: " + metaData.fileType()
        );

        for (ColorChannel channel : image.getColorChannels()) {
            Logger.dump("Channel: " + channel.getChannelType(),
                    () -> "Recieved Pixels:\n" + Arrays.toString(channel.getChannelData()) + "\n\n"
            );
        }
        Logger.info("Finished reading image file!");
        Logger.logTime();

        //NOTE: will need to adjust the ImageRep instantiation
        ImageRepresentative paethImage = PaethProcessor.paethEncode(image);

        Logger.verbose("Paeth Encoding Output: ");
        for (ColorChannel channel : paethImage.getColorChannels()) {
            Logger.verbose("Channel: " + channel.getChannelType());
            Logger.verbose("Initial Value: " +
                    paethImage.getInitialPixels()[paethImage.getChannelIndex(channel.getChannelType())]);

            Logger.dump("Paeth Values:\n", () -> Arrays.toString(channel.getChannelData()));
            Logger.verbose("\n");
        }
        Logger.info("Finished Paeth Encoding image pixel data!");
        Logger.logTime();
        //NOTE: had to adjust initial pixel length since it needs to be set before, related to above note

        ChunkProcessor chunkProcessor = new ChunkProcessor(paethImage);
        ChunkedImageRepresentative chunkedImage = chunkProcessor.chunkImage();
        chunkedImage.sequentiallyOrder();

        for (ChunkedColorChannel channel : chunkedImage.getChunkedChannels()) {
            Logger.verbose("Channel: " + channel.getChannelType());

            PixelChunk[] chunks = channel.getChannelChunks();
            Logger.verbose("Pixel Chunks: " + chunks.length);
            Logger.dump("Chunk Data:\n", () -> Arrays.toString(chunks));

            int[] isolatedPixels = channel.getChannelIsolatedIndexes();
            Logger.verbose("Isolated Pixels: " + isolatedPixels.length);
            Logger.dump("Pixel Indexes:\n", () -> Arrays.toString(isolatedPixels));

            int[] correspondingColors = channel.getChannelIsolatedCorrespondingColor();
            Logger.verbose("Corresponding Colors: " + correspondingColors.length);
            Logger.dump("Color Data:\n", () -> Arrays.toString(correspondingColors));
            Logger.verbose("\n");
        }
        Logger.info("Finished chunking image file!");
        Logger.logTime();

        //NOTE: compression still needs to be refactored, still bad, just stringed together in a seperate class to use meta and some clarity like EdgeFlags

        SpatialCompressor compressor = new SpatialCompressor(chunkedImage, CompressionConfig.DEFAULT);
        CompressedImageRepresentative compressedImage = compressor.QuadralizedDataCompressed();
        compressedImage.sortChannelOrder();


        for (CompressedColorChannel channel : compressedImage.getCompressedChannels()) {
            Logger.verbose("Channel: " + channel.getChannelColorType());
            Logger.verbose("Initial Pixel: " + channel.getInitialPixel());

            RectangleRepresentative[] rects = channel.getBuiltRectangles();
            Logger.verbose("Rectangles: " + rects.length);
            Logger.dump("Rectangle Data:\n", () -> Arrays.toString(rects));

            int[] isolatedPixels = channel.getIsolatedIndexes();
            Logger.verbose("Isolated Pixels: " + isolatedPixels.length);
            Logger.dump("Pixel Indexes:\n", () -> Arrays.toString(isolatedPixels));

            int[] correspondingColors = channel.getCorrespondingIsolatedValues();
            Logger.verbose("Corresponding Colors: " + correspondingColors.length);
            Logger.dump("Color Data:\n", () -> Arrays.toString(correspondingColors));
            Logger.verbose("\n");
        }
        Logger.info("Finished compressing image file!");
        Logger.logTime();

        Logger.wholeTime();

        QDCEncoder encoder = new QDCEncoder(compressedImage, outFilePath, CompressionConfig.DEFAULT);

        encoder.writeMetadata();
        for (int i = 0; i < compressedImage.getCompressedChannels().length; i++) {
            encoder.processChannel(i);
        }
        encoder.finalizeFile();
    }

    public static void disassemble(Path inFilePath, Path outFilePath, boolean overwrite) throws IOException {
        if (outFilePath == null) outFilePath = defaultOutput(inFilePath,".qdca");
        checkConflictingPaths(inFilePath, outFilePath, overwrite);
        try (QDCDisassembler ds = new QDCDisassembler(inFilePath, outFilePath)) {
            ds.dissassemble();
        }
    }

    public static void decode(Path inFilePath, Path outFilePath, boolean overwrite) throws IOException, DataFormatException {
        if (outFilePath == null) outFilePath = defaultOutput(inFilePath,".png");
        checkConflictingPaths(inFilePath, outFilePath, overwrite);

        Path potentialJPGPath = defaultOutput(inFilePath,".jpg");
        checkConflictingPaths(inFilePath, potentialJPGPath, overwrite);

        try (QDCDecoder dc = new QDCDecoder(inFilePath, outFilePath)) {
            dc.decode();
        }
    }

    public static void printHelp() {
        System.out.println("\n===== Quadralized Data Codec Help =====");
        System.out.println("Usage: java -jar qdc.jar [mode] -in <input-file> [flags]");

        System.out.println("\nModes:");
        System.out.println("  encode            Takes an image file (supports PNG and JPG, but test your luck) and encodes it into a QDC file");
        System.out.println("  decode            Takes a QDC file and outputs the original PNG or JPG (defaults to PNG if not defined)");
        System.out.println("  disassemble       " +
                "Takes a QDC file and outputs a QDCA file (just a text file, open it in notepad to get a detailed breakdown of the image)");
        System.out.println("\n==============================================================================");

        System.out.println("\nRequired Flags:");
        System.out.println("  -in <file>        Path to the input image file (e.g., test.png)");
        System.out.println("\n==============================================================================");

        System.out.println("\nOptional Options:");
        System.out.println("  -out <file>       Path to save the processed output file");
        System.out.println("  -overwrite        Overwrite the output file if it already exists");
        System.out.println("  -verbose          Enable detailed progress and configuration logs");
        System.out.println("  -dump             Enable deep debug logging (includes raw pixel/filter data)");
        System.out.println("  -h, --help        Display this help menu and exit");
        System.out.println("\n==============================================================================");

        System.out.println("\nExample:");
        System.out.println("  java -jar qdc.jar encode -verbose -in test.png -out output.qdc\n");
        System.out.println("\n==============================================================================");
    }
}