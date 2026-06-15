package rars.concolic;


import rars.cfg.BasicBlock;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Options {
    private final String HELP = "\nAvailable options:\n" +
            "--help :\t\tDisplay available options\n" +
            "--iterative-deepening :\tActivate iterative deepening\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions\n" +
            "--user-entries [file] :\tUse entries from a file to start the symbolic execution";


    boolean iterativeDeepening = false;
    boolean dfs = false;
    boolean userEntries = false;

    //Iterative deepening variables
    private int loopLimit = -1;

    private BufferedReader reader;

    private int maxExecutions = 300;
    private int maxInstructions = 500;

    Options(String[] args){
        if (args.length > 0) {
            checkOptions(args);
        }
    }

    public void newExecution(){
        if (iterativeDeepening){
            ++loopLimit;
        }
    }

    public int getMaxExecutions(){
        return maxExecutions;
    }

    public int getMaxInstructions(){
        return maxInstructions;
    }

    public boolean iteratesDeeper(ExecutionTreeNode currentNode, BasicBlock target) {
        if (iterativeDeepening){
            int occurrence = 0;
            ExecutionTreeNode node = currentNode;
            while (node != null){
                if(node.block == target){
                    ++occurrence;
                }
                node = node.parent;
            }
            return occurrence < loopLimit;
        }
        return true;
    }

    public ConcolicValues.V readCharFromFile() {
        if(!userEntries){return null;}

        char ch;
        try {
            if(reader.ready()){
                ch = (char) reader.read();
            } else {
                reader.close();
                return null;
            }
            return new ConcolicValues.V(ch, new SymbolicLong(ch));
        } catch (IOException e) {
            return null;
        }
    }

    public ConcolicValues.V readIntFromFile() {
        if(!userEntries){return null;}

        int value;
        try {
            if(reader.ready()){
                StringBuilder sb = new StringBuilder();

                reader.mark(1);
                int c = reader.read();

                while (c != -1 && Character.isDigit(c)) {
                    sb.append((char) c);

                    reader.mark(1);
                    c = reader.read();
                }

                if (c != -1) {
                    reader.reset();
                }

                value = Integer.parseInt(sb.toString());
            } else {
                reader.close();
                return null;
            }
            return new ConcolicValues.V(value, new SymbolicLong(value));
        } catch (IOException e) {
            return null;
        }
    }

    private void checkOptions(String[] args){
        for(int i = 1; i < args.length; ++i){
            switch (args[i]){
                case "--help":
                    System.out.println(HELP);
                    System.exit(0);
                    break;
                case "--iterative-deepening":
                    iterativeDeepening = true;
                    loopLimit = -1;
                    break;
                case "--dfs":
                    dfs = true;
                    break;
                case "--max-exec":
                    maxExecutions = Integer.parseInt(args[++i]);
                    break;
                case "--max-inst":
                    maxInstructions = Integer.parseInt(args[++i]);
                    break;
                case "--user-entries":
                    userEntries = true;
                    try {
                        reader = new BufferedReader(new FileReader(args[++i]));
                    } catch (FileNotFoundException e) {
                        System.out.println("File not found");
                        System.exit(1);
                    }
                    break;
                default:
                    System.out.println("Unknown option: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
            }
        }
    }
}
