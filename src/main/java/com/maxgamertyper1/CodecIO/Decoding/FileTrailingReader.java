package com.maxgamertyper1.CodecIO.Decoding;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileTrailingReader {

    public static byte[] readTrailingBytes(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();

        if (fileSize < 4) {
            throw new IllegalArgumentException("File size is smaller than 4 bytes!");
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(fileSize - 4);

            byte[] last4Bytes = new byte[4];
            raf.readFully(last4Bytes);

            return last4Bytes;
        }
    }
}
