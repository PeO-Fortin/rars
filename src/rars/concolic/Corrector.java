package rars.concolic;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Corrector {

    public static final int MAX_EXEC = 50;
    public static final String MASTER_FOLDER = "src/rars/concolic/results/master_results/";
    public static final FilenameFilter FILTER_INPUTS = (f, name) -> name.startsWith("input");
    public static final FilenameFilter FILTER_OUTPUTS = (f, name) -> name.startsWith("output");

    public static int topIndex = 1;
    public static String masterFileName;

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

    public static void addMaxExecArg(List<String> args) {
        args.add("--max-exec");
        args.add(String.valueOf(MAX_EXEC));
    }

    public static void saveExecutionResult(File inputFile, File outputFile, String destinationFolder, int index) {
        try {
            String inputContent = Files.readString(inputFile.toPath());
            String outputContent = Files.readString(outputFile.toPath());

            Files.writeString(Paths.get(destinationFolder, "input" + index), inputContent);
            Files.writeString(Paths.get(destinationFolder, "output" + index), outputContent);
        } catch (IOException e) {
            System.out.println("Error while saving execution result to " + destinationFolder + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public static File[] getFiles(String folder, FilenameFilter filter) {
        File fileFolder = new File(folder);
        return fileFolder.listFiles(filter);
    }

    public static File[] findNewInputs(File[] studentInputs, File[] knownInputs) {
        File[] newInputs = new File[studentInputs.length - knownInputs.length];
        if (studentInputs.length != knownInputs.length) {
            int i = 0;
            int j = knownInputs.length;
            while (j < studentInputs.length) {
                newInputs[i] = studentInputs[j];
                ++i; ++j;
            }
        }
        return newInputs;
    }

    public static void updateMasterList(File[] newInputs) {
        List<String> masterArgs = new ArrayList<>();
        addMaxExecArg(masterArgs);
        masterArgs.add("--user-entries");
        for (File f : newInputs) {
            masterArgs.add(f.getPath());
        }

        concExecFile(masterFileName, masterArgs);

        File[] newOutputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME, FILTER_OUTPUTS);

        for (int i = 0; i < newInputs.length; ++i) {
            saveExecutionResult(newInputs[i],newOutputs[i],MASTER_FOLDER,topIndex);
            topIndex++;
        }
    }

    public static void compareOutputs(File[] studentOutputs) {
        File[] masterOutputs = getFiles(MASTER_FOLDER, FILTER_OUTPUTS);
        try {
            for (int i = 0; i < masterOutputs.length; ++i) {
                String studentOutput = Files.readString(studentOutputs[i].toPath());
                String masterOutput = Files.readString(masterOutputs[i].toPath());

                if (!studentOutput.equals(masterOutput)) {
                    System.out.println("FAIL");
                }
            }
        } catch (IOException e) {
            System.out.println("Error while reading output: " + e.getMessage());
        }
    }

    public static void testStudent(String studentFile) {
        boolean stableInputs = false;

        while (!stableInputs) {
            List<String> studentArgs = new ArrayList<>();
            addMaxExecArg(studentArgs);
            studentArgs.add("--user-entries");
            File[] knownInputs = getFiles(MASTER_FOLDER, FILTER_INPUTS);
            for (File f : knownInputs) {
                studentArgs.add(f.getPath());
            }

            concExecFile(studentFile,studentArgs);

            File[] studentInputs = getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
            File[] newInputs = findNewInputs(studentInputs, knownInputs);

            if (newInputs.length == 0) {
                stableInputs = true;
            } else {
                updateMasterList(newInputs);
            }
        }

        File[] studentOutputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME, FILTER_OUTPUTS);
        compareOutputs(studentOutputs);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        masterFileName = args[0];
        List<String> arguments = new ArrayList<>();
        addMaxExecArg(arguments);
        concExecFile(masterFileName, arguments);

        File[] inputs = getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
        File[] outputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME,  FILTER_OUTPUTS);

        int i = 0;
        while (i < inputs.length) {
            saveExecutionResult(inputs[i], outputs[i], MASTER_FOLDER, topIndex);
            ++topIndex;
        }

        for(int j = 1; j < args.length; ++j){
            testStudent(args[j]);
        }

    }
}
