package rars.concolic;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Corrector {

    public static void concExecFile(String filename, List<String> arguments) {
        try {
            ConcolicInterpreter.main(arguments.toArray(new String[arguments.size()]));
        } catch (Exception e) {
            System.err.println("Error while executing " + filename + ": " + e.getMessage());
        }
    }

    public static String getInputs() {
        String inputFile = "inputs.txt";

        try {
            BufferedReader br = new BufferedReader(new FileReader("Results.txt"));
            PrintWriter pw = new PrintWriter(inputFile);


        } catch (IOException e) {
            System.out.println("IO exception: " + e.getMessage());
        }

        return inputFile;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        List<String> arguments = new ArrayList<>();
        String inputs = "";

        arguments.add("--max-exec");
        arguments.add("50");
        concExecFile(args[0], arguments);

        inputs = getInputs();

        arguments.add("--user-entries");
        arguments.add(inputs);

        for (int i = 1; i < args.length; ++i) {
            concExecFile(args[i], arguments);
        }
    }
}
