package rars.concolic;

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

        for (int i = 1; i < args.length; ++i) {
            arguments.add("--user-entries");
            arguments.add(inputs);
            concExecFile(args[i], arguments);
        }
    }
}
