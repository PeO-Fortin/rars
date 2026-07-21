package rars.options;

import rars.ProgramStatement;
import rars.cfg.BasicBlock;
import rars.cfg.CFG;
import rars.concolic.GenericInterpreter;
import rars.concolic.Heuristics;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class InterpreterOptions implements OptionsChecker{
    public static final String HELP = "\nAvailable options:\n" +
            "--help :\t\tDisplay available options\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions\n" +
            "--user-entries [number of files] [files] :\tUse entries from files to start the symbolic execution" +
            "--dfs :\tActive Depth-first search exploration\n" +
            "--distance-exit :\tActivate distance to exit exploration\n" +
            "--random :\tActive random paths exploration" +
            "--coverage :\tActive coverage-guided exploration" +
            "--rand-cov :\tActive combination of random and coverage-guided exploration";


    Heuristics heuristic = Heuristics.BFS;
    boolean randCov;

    private Map<BasicBlock, Integer> coverageCounter;

    boolean userEntries = false;
    public boolean eof = false;

    //User entries variables
    private BufferedReader readerInput;
    private String[] filesName;
    private int fileNumber;

    private int maxExecutions = 100;
    private int maxInstructions = 500;

    public InterpreterOptions(){}

    public void newExecution() {
        if (userEntries) {
            try {
                if(readerInput != null) {
                    readerInput.close();
                }
                if (fileNumber < filesName.length) {
                    readerInput = new BufferedReader(new FileReader(filesName[fileNumber]));
                    ++fileNumber;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Input file not found: " + filesName[fileNumber]);
                System.exit(1);
            } catch (IOException e) {
                System.out.println("IO Error new Execution");
                System.exit(1);
            }
        }
    }

    public Heuristics getHeuristic() {
        if(heuristic==Heuristics.RANDOM_COVERAGE) {
            randCov = !randCov;
            return randCov ? Heuristics.COVERAGE : Heuristics.RANDOM;
        }

        return heuristic;
    }

    public int getMaxExecutions(){
        return maxExecutions;
    }

    public int getMaxInstructions(){
        return maxInstructions;
    }

    public Long readFromFile() {
        if(!userEntries){return null;}

        return readIntFromFile(readerInput);
    }

    private Long readIntFromFile(BufferedReader reader){
        long value;
        boolean negative = false;

        try {
            if(reader.ready()){
                String sb = "";
                int c;

                do {
                    reader.mark(1);
                    c = reader.read();
                    if (c == -1) return null;
                } while (c != '|');

                reader.mark(1);
                c = reader.read();

                if (c == '-') {
                    negative = true;
                    reader.mark(1);
                    c = reader.read();
                }

                while (c != -1 && Character.isDigit(c)) {
                    sb += (char) c;

                    reader.mark(1);
                    c = reader.read();
                }

                if (c != -1) {
                    reader.reset();
                }

                if(sb.length() == 0) {
                    return null;
                }

                value = Long.parseLong(sb.toString());
                if (negative) { value = -value; }

            } else {
                reader.close();
                eof = true;
                return -1L;
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

    public Map<BasicBlock, Integer> calculateDistanceToExit(CFG cfg) {

        if(heuristic != Heuristics.TO_EXIT) return null;

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

    public void countCoverage(BasicBlock block) {
        if(heuristic == Heuristics.COVERAGE) {
            coverageCounter.merge(block, 1, Integer::sum);
        }
    }

    public int getCoverage(BasicBlock block) {
        return  coverageCounter.getOrDefault(block, 0);
    }

    public void checkOptions(String args[]) {
        for (int i = 1; i < args.length; ++i) {
            int newIndex = checkOption(args, i);
            if (newIndex == -1) {
                System.out.println("Invalid concolic interpreter option: " + args[i]);
                System.exit(1);
            }
            i = newIndex;
        }
    }

    @Override
    public int checkOption(String[] args, int i){
        switch (args[i]){
            case "--help":
                System.out.println(HELP);
                System.exit(0);
            case "--max-exec":
                maxExecutions = Integer.parseInt(args[++i]);
                break;
            case "--max-inst":
                maxInstructions = Integer.parseInt(args[++i]);
                break;
            case "--user-entries":
                userEntries = true;
                try {
                    filesName = new String[Integer.parseInt(args[++i])];
                    for(int j = 0; j < filesName.length; ++j){
                        filesName[j] = args[++i];
                    }
                    fileNumber = 0;
                } catch (NumberFormatException e) {
                    System.out.println("Wrong parameter for option --user-entries: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
                }
                break;
            case "--dfs":
                if(heuristic == Heuristics.BFS) {
                    heuristic = Heuristics.DFS;
                    break;
                } else {
                    errorHeuristic();
                }
            case "--distance-exit":
                if(heuristic == Heuristics.BFS) {
                    heuristic = Heuristics.TO_EXIT;
                    break;
                } else {
                    errorHeuristic();
                }
            case "--random":
                if(heuristic == Heuristics.BFS) {
                    heuristic = Heuristics.RANDOM;
                    break;
                } else {
                    errorHeuristic();
                }
            case "--coverage":
                if(heuristic == Heuristics.BFS) {
                    heuristic = Heuristics.COVERAGE;
                    coverageCounter = new HashMap<>();
                    break;
                } else {
                    errorHeuristic();
                }
            case "--rand-cov":
                if(heuristic == Heuristics.BFS) {
                    heuristic = Heuristics.RANDOM_COVERAGE;
                    randCov = true;
                    coverageCounter = new HashMap<>();
                    break;
                } else {
                    errorHeuristic();
                }
            default:
                i = -1;
            }

            return i;
    }

    public void errorHeuristic(){
        System.out.println("Please choose no more than one heuristic");
        System.exit(1);
    }
}
