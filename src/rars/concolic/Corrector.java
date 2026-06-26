package rars.concolic;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Corrector {

    public static final int MAX_EXEC = 20;
    public static final String MASTER_FOLDER = "src/rars/concolic/results/master_results/";
    public static final FilenameFilter FILTER_INPUTS = (f, name) -> name.startsWith("inputs");
    public static final FilenameFilter FILTER_OUTPUTS = (f, name) -> name.startsWith("outputs");
    public static final String CORRECTION_FILE = "Correction.txt";
    public static final String STUDENT_HEADER =
            "****************************************\n" +
            "Fichier : %s\n" +
            "****************************************\n";

    public static int topIndex = 0;
    public static String masterFileName;

    public static void concExecFile(String filename, List<String> arguments) {
        try {
            List<String> completeArgs = new ArrayList<>();
            completeArgs.add(filename);
            completeArgs.addAll(arguments);
            ConcolicInterpreter.main(completeArgs.toArray(new String[0]));
        } catch (Exception e) {
            System.err.println("Error while executing " + filename);
            e.printStackTrace();
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

            String paddedIndex = String.format("%05d", index);

            Files.writeString(Paths.get(destinationFolder, "inputs" + paddedIndex), inputContent);
            Files.writeString(Paths.get(destinationFolder, "outputs" + paddedIndex), outputContent);
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
        File[] newInputs;
        if (studentInputs.length - knownInputs.length > 0) {
            newInputs = new File[studentInputs.length - knownInputs.length];
        } else {
            newInputs = new File[0];
        }

        if (studentInputs.length > knownInputs.length) {
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
        masterArgs.add("" + newInputs.length);
        for (File f : newInputs) {
            masterArgs.add(f.getPath());
        }

        concExecFile(masterFileName, masterArgs);

        File[] newOutputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME, FILTER_OUTPUTS);
        Arrays.sort(newOutputs, Comparator.comparing(File::getName));

        for (int i = 0; i < newInputs.length; ++i) {
            saveExecutionResult(newInputs[i],newOutputs[i],MASTER_FOLDER,topIndex);
            topIndex++;
        }
    }

    public static void compareOutputs(File[] studentOutputs) {
        File[] masterInputs = getFiles(MASTER_FOLDER, FILTER_INPUTS);
        File[] studentInputs = getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
        File[] masterOutputs = getFiles(MASTER_FOLDER, FILTER_OUTPUTS);
        Arrays.sort(masterInputs, Comparator.comparing(File::getName));
        Arrays.sort(masterOutputs, Comparator.comparing(File::getName));
        Arrays.sort(studentInputs, Comparator.comparing(File::getName));

        try {
        PrintWriter pw = new PrintWriter(new FileWriter(CORRECTION_FILE, true));
        boolean success = true;
            for (int i = 0; i < masterOutputs.length && i < studentOutputs.length; ++i) {
                String studentOutput = Files.readString(studentOutputs[i].toPath());
                String masterOutput = Files.readString(masterOutputs[i].toPath());

                if (!studentOutput.equals(masterOutput)) {
                    pw.println("FAIL");
                    pw.println(masterInputs[i].getName());
                    pw.println("Teacher Inputs : " + Files.readString(masterInputs[i].toPath()));
                    pw.println(studentInputs[i].getName());
                    pw.println("Student Inputs : " + Files.readString(studentInputs[i].toPath()));
                    pw.println(masterOutputs[i].getName());
                    pw.println("Teacher Outputs : " + Files.readString(masterOutputs[i].toPath()));
                    pw.println(studentOutputs[i].getName());
                    pw.println("Student Outputs : " + Files.readString(studentOutputs[i].toPath()));
                    success = false;
                }
            }

            if (success) {
                pw.println("SUCCESS");
            }
            pw.flush();
            pw.close();
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
            Arrays.sort(knownInputs, Comparator.comparing(File::getName));
            studentArgs.add("" + knownInputs.length);
            for (File f : knownInputs) {
                studentArgs.add(f.getPath());
            }

            concExecFile(studentFile,studentArgs);

            File[] studentInputs = getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
            Arrays.sort(studentInputs, Comparator.comparing(File::getName));
            File[] newInputs = findNewInputs(studentInputs, knownInputs);
            Arrays.sort(newInputs, Comparator.comparing(File::getName));

            if (newInputs.length == 0) {
                stableInputs = true;
            } else {
                updateMasterList(newInputs);
            }
        }

        File[] studentOutputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME, FILTER_OUTPUTS);
        Arrays.sort(studentOutputs, Comparator.comparing(File::getName));
        compareOutputs(studentOutputs);
    }

    private static void cleanFolders() {
        File[] dir = {
                new File(ConcolicInterpreter.OUTPUT_FOLDER_NAME),
                new File(ConcolicInterpreter.INPUT_FOLDER_NAME),
                new File(MASTER_FOLDER),
        };

        try {
            for (File d : dir) {
                for (File file : d.listFiles())
                    if (!file.isDirectory())
                        file.delete();
            }

            File c = new File(CORRECTION_FILE);
            c.delete();
        } catch (NullPointerException e) {
            return;
        }


    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        cleanFolders();

        masterFileName = args[0];
        List<String> arguments = new ArrayList<>();
        addMaxExecArg(arguments);
        concExecFile(masterFileName, arguments);

        File[] inputs = getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
        Arrays.sort(inputs, Comparator.comparing(File::getName));
        File[] outputs = getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME,  FILTER_OUTPUTS);
        Arrays.sort(outputs, Comparator.comparing(File::getName));

        for(int i = 0; i < inputs.length; ++i) {
            saveExecutionResult(inputs[i], outputs[i], MASTER_FOLDER, topIndex);
            ++topIndex;
        }

        try {
            for (int i = 1; i < args.length; ++i) {
                PrintWriter pw = new PrintWriter(new FileWriter(CORRECTION_FILE, true));
                pw.printf(STUDENT_HEADER, args[i]);
                pw.close();
                testStudent(args[i]);
            }
        } catch (IOException e) {
            System.out.println("Error while writing results: " + e.getMessage());
        }

    }
}
