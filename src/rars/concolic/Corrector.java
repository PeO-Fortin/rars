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

        for (File studentFile : files.studentFiles) {

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
        for (int i = 1; i < files.getNumberOfFiles(); ++i) {
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

    static class FilesManager{
        public static final String MASTER_FOLDER = "src/rars/concolic/results/master_results/";
        public static final FilenameFilter FILTER_INPUTS = (f, name) -> name.startsWith("inputs");
        public static final FilenameFilter FILTER_OUTPUTS = (f, name) -> name.startsWith("outputs");
        public static final String CORRECTION_FILE = "Correction.txt";
        public static final String STUDENT_HEADER =
                "****************************************\n" +
                "Fichier : %s\n" +
                "****************************************\n";

        public final File MASTER_FILE;
        private File[] studentFiles;

        private static int topIndex = 0;


        public FilesManager(String [] filesName){
            MASTER_FILE = new File(filesName[0]);
            studentFiles = new File[filesName.length-1];

            cleanFolders();

            for(int i = 1; i < filesName.length; ++i){
                studentFiles[i] = new File(filesName[i]);
            }
        }

        public int getNumberOfFiles(){
            return studentFiles.length;
        }

        public File getStudentFile(int index){
            return studentFiles[index];
        }

        public File[] getMasterInputsFiles() {
            return getFiles(MASTER_FOLDER, FILTER_INPUTS);
        }

        public File[] getMasterOutputsFiles() {
            return getFiles(MASTER_FOLDER, FILTER_OUTPUTS);
        }

        public File[] getExecInputFiles(){
            return getFiles(ConcolicInterpreter.INPUT_FOLDER_NAME, FILTER_INPUTS);
        }

        public File[] getExecOutputFiles(){
            return getFiles(ConcolicInterpreter.OUTPUT_FOLDER_NAME, FILTER_OUTPUTS);
        }

        private File[] getFiles(String folder, FilenameFilter filter) {
            File fileFolder = new File(folder);
            File[] files = fileFolder.listFiles(filter);
            Arrays.sort(files, Comparator.comparing(File::getName));
            return files;
        }

        public void saveExecutionResults() {
            File[] inputs = getExecInputFiles();
            File[] outputs = getExecOutputFiles();

            for(int i = 0; i < inputs.length; ++i) {
                saveExecutionResults(inputs[i], outputs[i]);
                ++topIndex;
            }
        }

        private void saveExecutionResults(File inputFile, File outputFile) {
            try {
                String inputContent = Files.readString(inputFile.toPath());
                String outputContent = Files.readString(outputFile.toPath());

                String paddedIndex = String.format("%05d", topIndex);

                Files.writeString(Paths.get(MASTER_FOLDER, "inputs" + paddedIndex), inputContent);
                Files.writeString(Paths.get(MASTER_FOLDER, "outputs" + paddedIndex), outputContent);
            } catch (IOException e) {
                System.out.println("Error while saving execution result to " + MASTER_FOLDER + ": " + e.getMessage());
                System.exit(1);
            }
        }

        public void printStudentHeader(int index){
            try {
                PrintWriter pw = new PrintWriter(new FileWriter(CORRECTION_FILE, true));
                pw.printf(STUDENT_HEADER, studentFiles[index].getName());
                pw.close();
            } catch (IOException e)  {
                System.out.println("Error while writing header: " + e.getMessage());
            }
        }

        private void cleanFolders() {
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
    }
}
