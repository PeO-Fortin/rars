package rars.options;

import rars.concolic.ConcolicInterpreter;
import rars.concolic.MemoryValueTypes;
import rars.riscv.hardware.MemoryConfigurations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class CorrectorOptions implements OptionsChecker {

    public static String HELP = InterpreterOptions.HELP +
            "--help :\t\tDisplay available options\n" +
            "--sol [file] :\tThe file containing the program of reference\n" +
            "--stud [files] :\tThe files to compare to the program of reference\n" +
            "--tests [files] :\tThe previously prepared tests to include in the comparison\n" +
            "--bitmap [unit width] [unit height] [display width] [display height] [base address code]:\tThe correction will check the bitmap output\n" +
            "\tBase address codes :\n" +
            "\t\t gd (global data) =\t"+ MemoryConfigurations.getDefaultDataSegmentBaseAddress() + "\n" +
            "\t\t gp (global pointer) = \t"+ MemoryConfigurations.getDefaultGlobalPointer() +"\n" +
            "\t\t sd (static data) =\t"+ MemoryConfigurations.getDefaultDataBaseAddress() +"\n" +
            "\t\t hp (heap) =\t"+ MemoryConfigurations.getDefaultHeapBaseAddress() +"\n" +
            "\t\t mm (memory map) =\t"+ MemoryConfigurations.getDefaultMemoryMapBaseAddress() +"\n";

    private ArrayList<File> userTests;
    private InterpreterOptions interpreterOptions;
    private ArrayList<String> cleanArgs;

    public boolean bitmap = false;
    private byte unitWidth;
    private byte unitHeight;
    private short displayWidth;
    private short displayHeight;
    private int bitmapBaseAddress;

    public CorrectorOptions(){}

    public File[] getUserTests() {
        if (userTests == null) return null;
        return userTests.toArray(new File[0]);
    }



    public InterpreterOptions getInterpreterOptions() {
        return interpreterOptions;
    }

    public String[] checkOptions(String[] args) {
        interpreterOptions = new InterpreterOptions();
        cleanArgs = new ArrayList<>();
        for  (int i = 0; i < args.length; i++) {
            int newIndex = checkOption(args, i);
            if (newIndex == -1) {
                System.out.println("Invalid corrector option: " + args[i]);
                System.exit(1);
            }

            i = newIndex;
        }
        return cleanArgs.toArray(new String[0]);
    }

    public int checkOption(String[] args, int i){
        int newIndex = interpreterOptions.checkOption(args, i);
        if (newIndex != -1) {return newIndex;}
        switch(args[i]) {
            case "--sol":
                ++i;
                cleanArgs.add(0, args[i]);
                break;
            case "--stud":
                ++i;
                for(; i < args.length; i++){
                    if(args[i].startsWith("-")) break;
                    cleanArgs.add(args[i]);
                }
                --i;
                break;
            case "--tests":
                ++i;
                userTests = new ArrayList<>();
                for(; i < args.length; i++){
                    if(args[i].startsWith("-")) break;
                    userTests.add(new File(args[i]));
                }
            case "--help":
                System.out.println(HELP);
                break;
            case "--bitmap":
                bitmap = true;

                unitWidth = Byte.parseByte(args[++i]);
                unitHeight = Byte.parseByte(args[++i]);
                displayWidth = Short.parseShort(args[++i]);
                displayHeight = Short.parseShort(args[++i]);

                switch(args[++i]) {
                    case "gd":
                        bitmapBaseAddress = MemoryConfigurations.getDefaultDataSegmentBaseAddress();
                        break;
                    case "gp":
                        bitmapBaseAddress = MemoryConfigurations.getDefaultGlobalPointer();
                        break;
                    case "sd":
                        bitmapBaseAddress = MemoryConfigurations.getDefaultDataBaseAddress();
                        break;
                    case "hp":
                        bitmapBaseAddress = MemoryConfigurations.getDefaultHeapBaseAddress();
                        break;
                    case "mm":
                        bitmapBaseAddress = MemoryConfigurations.getDefaultMemoryMapBaseAddress();
                        break;
                }

                int endingAddress = bitmapBaseAddress + ((displayHeight / unitHeight) * (displayWidth / unitWidth) * MemoryValueTypes.WORD.getSize());

                interpreterOptions.saveMemory = true;
                interpreterOptions.setStartingAddress(bitmapBaseAddress);
                interpreterOptions.setEndingAddress(endingAddress);

                File d = new File(ConcolicInterpreter.MEMORY_SAVE_FOLDER);
                try {
                    Files.createDirectories(d.toPath());
                    for (File f : d.listFiles())
                        if (!f.isDirectory())
                            f.delete();
                } catch (IOException e) {
                    System.out.println("Error creating memory save folder: " + e.getMessage());
                }

                break;
            default:
                i = -1;
        }
        return i;
    }
}
