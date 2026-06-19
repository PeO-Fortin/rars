package rars.concolic;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Corrector {

    public static final int MAX_EXEC = 50;

    public static void concExecFile(String filename, List<String> arguments) {
        try {
            List<String> completeArgs = new ArrayList<>();
            completeArgs.add(filename);
            completeArgs.addAll(arguments);
            ConcolicInterpreter.main(completeArgs.toArray(new String[0]));
        } catch (Exception e) {
            System.err.println("Error while executing " + filename + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        List<String> arguments = new ArrayList<>();

        arguments.add("--max-exec");
        arguments.add(String.valueOf(MAX_EXEC));
        concExecFile(args[0], arguments);

        File inputFolder = new File(ConcolicInterpreter.INPUT_FOLDER_NAME);

        List<File> masterInputs = new ArrayList<>(Arrays.asList(inputFolder.listFiles()));

        arguments.add("--user-entries");

        for(File f : masterInputs){
            arguments.add(inputFolder.getName() + f.getName());
        }

    }
}
