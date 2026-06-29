package rars.corrector;

import rars.concolic.ConcolicInterpreter;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Corrector {

    public static final int MAX_EXEC = 20;


    public static void concExecFile(File filename, List<String> arguments) {
        try {
            List<String> completeArgs = new ArrayList<>();
            completeArgs.add(filename.getPath());
            completeArgs.addAll(arguments);
            ConcolicInterpreter.main(completeArgs.toArray(new String[0]));
        } catch (Exception e) {
            System.err.println("Error while executing " + filename);
            e.printStackTrace();
        }
    }

    public static void execStudentFile(File studentFile, File[] knownInputs) {
        List<String> studentArgs = new ArrayList<>();
        addMaxExecArg(studentArgs);
        studentArgs.add("--user-entries");

        studentArgs.add("" + knownInputs.length);
        for (File f : knownInputs) {
            studentArgs.add(f.getPath());
        }

        concExecFile(studentFile,studentArgs);
    }

    public static void addMaxExecArg(List<String> args) {
        args.add("--max-exec");
        args.add(String.valueOf(MAX_EXEC));
    }

    public static void generateTestFiles(FilesManager files) {

        List<String> arguments = new ArrayList<>();
        addMaxExecArg(arguments);

        concExecFile(files.MASTER_FILE, arguments);
        files.saveExecutionResults();

        for (File studentFile : files.getStudentFiles()) {

            boolean stableInputs = false;

            while (!stableInputs) {
                File[] knownInputs = files.getMasterInputsFiles();
                execStudentFile(studentFile, knownInputs);

                File[] studentInputs = files.getExecInputFiles();
                File[] newInputs = findNewInputs(studentInputs, knownInputs);

                if (newInputs.length == 0) {
                    stableInputs = true;
                } else {
                    updateMasterList(newInputs, files);
                }
            }
        }
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

    public static void updateMasterList(File[] newInputs, FilesManager files) {
        List<String> masterArgs = new ArrayList<>();
        addMaxExecArg(masterArgs);
        masterArgs.add("--user-entries");
        masterArgs.add("" + newInputs.length);
        for (File f : newInputs) {
            masterArgs.add(f.getPath());
        }

        concExecFile(files.MASTER_FILE, masterArgs);

        files.saveExecutionResults();
    }

    public static void compareOutputs(FilesManager files) {
        File[] masterInputs = files.getMasterInputsFiles();
        File[] studentInputs = files.getExecInputFiles();
        File[] masterOutputs = files.getMasterOutputsFiles();
        File[] studentOutputs = files.getExecOutputFiles();

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(files.CORRECTION_FILE, true));
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

    public static void testStudents(FilesManager files) {
        File[] knownInputs = files.getMasterInputsFiles();
        for (int i = 0; i < files.getNumberOfFiles(); ++i) {
            System.out.println("Testing student " + i);
            files.printStudentHeader(i);
            execStudentFile(files.getStudentFile(i), knownInputs);
            compareOutputs(files);
        }
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        FilesManager files = new FilesManager(args);

        generateTestFiles(files);

        testStudents(files);
    }
}
