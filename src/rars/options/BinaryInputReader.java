package rars.options;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BinaryInputReader implements InputReader {
    private InputStream in;

    public BinaryInputReader(String file) {
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + file);
        }
    }

    @Override
    public Long readChar() throws IOException {
        return (long) in.read();
    }

    @Override
    public Long readInt() throws IOException {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b == -1) return null;
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }
}
