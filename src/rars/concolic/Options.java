package rars.concolic;

import rars.ProgramStatement;
import rars.cfg.BasicBlock;
import rars.cfg.CFG;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Options {
    private final String HELP = "\nAvailable options:\n" +
            "--help :\t\tDisplay available options\n" +
            "--iterative-deepening :\tActivate iterative deepening\n" +
            "--distance-exit :\tActivate distance to exit exploration\n" +
            "--dfs :\tActive Depth-first search exploration\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions\n" +
            "--user-entries [number of files] [files] :\tUse entries from files to start the symbolic execution";


    boolean iterativeDeepening = false;
    boolean dfs = false;
    boolean userEntries = false;
    boolean distanceToExit = false;

    //Iterative deepening variables
    private int loopLimit = -1;

    //User entries variable
    private BufferedReader readerInput;
    private String[] files;
    private int fileNumber;

    private int maxExecutions = 300;
    private int maxInstructions = 500;

    Options(String[] args){
        if (args.length > 0) {
            checkOptions(args);
        }
    }

    public void newExecution() {
        if (iterativeDeepening) {
            ++loopLimit;
        }
        if (userEntries) {
            try {
                if (fileNumber < files.length) {
                    readerInput = new BufferedReader(new FileReader(files[fileNumber]));
                    ++fileNumber;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Input file not found: " + files[fileNumber]);
                System.exit(1);
            }
        }
    }

    public int getMaxExecutions(){
        return maxExecutions;
    }

    public int getMaxInstructions(){
        return maxInstructions;
    }

    //TODO
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

        Character ch = readCharFromFile(readerInput);

        return ch == null ? null : new ConcolicValues.V(ch, new SymbolicLong(ch));

    }

    private Character readCharFromFile(BufferedReader reader) {
        char ch;

        try {
            if(reader.ready()){
                ch = (char) reader.read();
            } else {
                reader.close();
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        return ch;
    }

    public ConcolicValues.V readIntFromFile() {
        if(!userEntries){return null;}

        Integer value = readIntFromFile(readerInput);

        return value==null ? null : new ConcolicValues.V(value, new SymbolicLong(value));
    }

    private Integer readIntFromFile(BufferedReader reader){
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

        } catch (IOException e) {
            return null;
        }

        return value;
    }

    public String readStringFromFile(long length) {
        if(!userEntries || length <= 0){return null;}
        String value = "";

        try {
            for(int i = 0; i < length - 1; ++i) {
                if(!readerInput.ready()){
                    readerInput.close();
                    return null;
                }
                char ch = (char) readerInput.read();
                if (ch == '\0') break;
                value += ch;
            }
        }
        catch (IOException e) {
            return null;
        }
        return value;
    }

    Map<BasicBlock, Integer> calculateDistanceToExit(CFG cfg) {

        if(!distanceToExit) return null;

        Map<BasicBlock, Integer> distances = new HashMap<>();
        Queue<BasicBlock> worklist = new LinkedList<>();

        for (BasicBlock block : cfg.blocks) {
            for (ProgramStatement ps : block.instructions) {
                int[] operands = ps.getOperands();
                if (ps.getInstruction().getName().equals("addi") &&
                        operands[0] == 17 && operands[1] == 0 && operands[2] == 10) {
                    distances.put(block, 0);
                    worklist.add(block);
                }
            }
        }

        if (worklist.isEmpty()) return null;

        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.remove();
            int currentDistance = distances.get(block);
            for (BasicBlock predecessor : block.getIn()) {
                if (!distances.containsKey(predecessor)) {
                    distances.put(predecessor, currentDistance + 1);
                    worklist.add(predecessor);
                }
            }
        }

        return distances;
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
                        files = new String[Integer.parseInt(args[++i])];
                        for(int j = 0; j < files.length; ++j){
                            files[j] = args[++i];
                        }
                        fileNumber = 0;
                    } catch (NumberFormatException e) {
                        System.out.println("Wrong parameter for option --user-entries: " + args[i]);
                        System.out.println("Try option '--help'");
                        System.exit(1);
                    }
                    break;
                case "--distance-exit":
                    distanceToExit = true;
                    break;
                default:
                    System.out.println("Unknown option: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
            }
        }
    }
}
