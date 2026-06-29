package rars.corrector;

import rars.concolic.ConcolicInterpreter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

public class FilesManager{
    public static final String MASTER_FOLDER = "src/rars/corrector/master_results/";
    public static final FilenameFilter FILTER_INPUTS = (f, name) -> name.startsWith("inputs");
    public static final FilenameFilter FILTER_OUTPUTS = (f, name) -> name.startsWith("outputs");
    public static final String CORRECTION_FILE = "src/rars/corrector/Correction.txt";
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
            studentFiles[i-1] = new File(filesName[i]);
        }
    }

    public int getNumberOfFiles(){
        return studentFiles.length;
    }

    public File[] getStudentFiles(){
        return studentFiles;
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
