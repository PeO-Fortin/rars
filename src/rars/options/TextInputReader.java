package rars.options;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TextInputReader implements InputReader{
    private BufferedReader in;

    public TextInputReader(String file) {
        try {
            in = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + file);
        }
    }

    @Override
    public Long readInt() throws IOException, NumberFormatException {
        String line = in.readLine();
        return line == null ? null : Long.parseLong(line);
    }

    @Override
    public Long readChar() throws IOException {
        return (long) in.read();
    }
}
