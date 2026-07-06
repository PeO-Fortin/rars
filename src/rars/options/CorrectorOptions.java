package rars.options;

import java.io.File;
import java.util.ArrayList;

public class CorrectorOptions implements OptionsChecker {

    public static String HELP = InterpreterOptions.HELP + "TODO";

    private ArrayList<File> userTests;
    private InterpreterOptions interpreterOptions;
    private ArrayList<String> cleanArgs;


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
        for  (int i = 1; i < args.length; i++) {
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
            default:
                i = -1;
            }
            return i;
        }
}
