package com.maxgamertyper1;

import com.maxgamertyper1.CodecIO.Encoding.EncodingManager;
import com.maxgamertyper1.CodecIO.Encoding.FormattedWriter;
import com.maxgamertyper1.datatypes.ChannelColorTypes;
import com.maxgamertyper1.datatypes.CompressedColorChannel;
import com.maxgamertyper1.datatypes.CompressedImageRepresentative;
import com.maxgamertyper1.datatypes.ShapeUnionFind;
import com.maxgamertyper1.datatypes.records.CornerDataRepresentative;
import com.maxgamertyper1.datatypes.records.RectangleRepresentative;
import com.maxgamertyper1.imagereading.ImageDataHolder;
import com.maxgamertyper1.imagereading.OriginalFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.Adler32;

import static com.maxgamertyper1.EncodingTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EncodingTests {
    @Test
    @Order(1)
    @DisplayName("RAW encoding test")
    void testRawEncoding() throws IOException {
        //takes a "pre-calculated" (just numbers i wrote) paeth array and encodes it

        CompressedColorChannel[] colorChannels = new CompressedColorChannel[1];
        colorChannels[0] = new CompressedColorChannel(ChannelColorTypes.Gray, new RectangleRepresentative[]{}, new CornerDataRepresentative[]{}, 0);
        colorChannels[0].setIsolatedIndexes(sequentialData,sequentialData);

        ShapeUnionFind shapeUnionFind = new ShapeUnionFind(sequentialData);
        ImageDataHolder.flatsize = 25;
        shapeUnionFind.calculateShapeRoots();
        shapeUnionFind.calculatePerimiterAndHoles(straightIsolatedFlags);


        colorChannels[0].setShapeOrganizer(shapeUnionFind);

        CompressedImageRepresentative preBuiltImage = new CompressedImageRepresentative(true,true,colorChannels,true);
        preBuiltImage.setMetaData((short) 5, (short) 5, OriginalFileType.Unknown);


        EncodingManager tempManager = new EncodingManager(preBuiltImage,"RAWTest.qdc");
        preBuiltImage.sortChannelOrder();

        tempManager.writeMetadata();
        for (int i=0; i < preBuiltImage.getCompressedChannels().length; i++) {
            tempManager.processChannel(i);
        }
        tempManager.finalizeFile();

        Path filePath = Paths.get("RAWTest.qdc");
        byte[] fileBytes = new byte[0];

        try {
            fileBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            fail("> Critical: Could not read resulting file in RAW byte test");
        }

        byte[] versionCorrectedAnswer = expectedRAWTestBytes.clone();
        versionCorrectedAnswer[4] = FormattedWriter.compressionVersionMajor;
        versionCorrectedAnswer[5] = FormattedWriter.compressionVersionMinor;

        Adler32 correctionAdler = new Adler32();
        correctionAdler.update(versionCorrectedAnswer, 0, 12);

        //quickly update the metadata hash
        long metaAdler = correctionAdler.getValue();
        versionCorrectedAnswer[12] = (byte) (metaAdler >> 24);
        versionCorrectedAnswer[13] = (byte) (metaAdler >> 16);
        versionCorrectedAnswer[14] = (byte) (metaAdler >> 8);
        versionCorrectedAnswer[15] = (byte) (metaAdler);


        correctionAdler.reset();
        correctionAdler.update(versionCorrectedAnswer,21,24);
        long channelAdler = correctionAdler.getValue();
        versionCorrectedAnswer[45] = (byte) (channelAdler >> 24);
        versionCorrectedAnswer[46] = (byte) (channelAdler >> 16);
        versionCorrectedAnswer[47] = (byte) (channelAdler >> 8);
        versionCorrectedAnswer[48] = (byte) (channelAdler);

        //update the final hash
        correctionAdler.reset();
        correctionAdler.update(versionCorrectedAnswer, 0, 69);
        long finalAdler = correctionAdler.getValue();
        versionCorrectedAnswer[69] = (byte) (finalAdler >> 24);
        versionCorrectedAnswer[70] = (byte) (finalAdler >> 16);
        versionCorrectedAnswer[71] = (byte) (finalAdler >> 8);
        versionCorrectedAnswer[72] = (byte) (finalAdler);


        assertArrayEquals(
                versionCorrectedAnswer,
                fileBytes,
                "RAW byte test failed, TestBack data did not match outputed file.\nExpected: " +
                        Arrays.toString(versionCorrectedAnswer) +"\n Recieved: "+Arrays.toString(fileBytes)
        );

    }
}
