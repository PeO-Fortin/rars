package rars.corrector;

import java.io.File;
import java.util.ArrayList;

public class Options {

    public static String HELP = "TODO";

    private int maxExecutions = 50;
    private ArrayList<File> userTests;

    public Options(){}

    public int getMaxExecutions() {
        return maxExecutions;
    }

    public File[] getUserTests() {
        if (userTests == null) return null;
        return userTests.toArray(new File[0]);
    }

    public String[] checkOptions(String[] args){
        ArrayList<String> cleanArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++){
            switch(args[i]) {
                case "--max":
                    ++i;
                    maxExecutions = Integer.parseInt(args[i]);
                    break;
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
            }
        }

        return cleanArgs.toArray(new String[0]);
    }

}
