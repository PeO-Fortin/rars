package rars.options;

import rars.ProgramStatement;
import rars.cfg.BasicBlock;
import rars.cfg.CFG;
import rars.concolic.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class InterpreterOptions implements OptionsChecker{
    public static final String HELP = "\nAvailable options:\n" +
            "--help :\t\tDisplay available options\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions\n" +
            "--text-entries [number of files] [files] :\tUse entries from text files to start the symbolic execution\n" +
            "--binary-entries [number of files] [files]:\tUse entries from binary files to start the symbolic execution\n" +
            "--dfs :\tActive Depth-first search exploration\n" +
            "--distance-exit :\tActivate distance to exit exploration\n" +
            "--random :\tActive random paths exploration\n" +
            "--coverage :\tActive coverage-guided exploration\n" +
            "--rand-cov :\tActive combination of random and coverage-guided exploration\n" +
            "--save-memory [starting address] [ending address] :\t Save the memory state from the starting address to the ending address for each execution";

    Heuristics heuristic = Heuristics.BFS;
    boolean randCov;

    private Map<BasicBlock, Integer> coverageCounter;
    private Set<Integer> exploredBlocks;
    private Set<Integer> exploredInstructions;

    boolean textUserEntries = false;
    boolean binaryUserEntries = false;
    public boolean eof = false;

    public boolean saveMemory = false;
    private int startingAddress;
    private int endingAddress;

    //User entries variables
    private String[] textFilesName;
    private int textFileNumber;
    private String[] binaryFilesName;
    private int binaryFileNumber;
    private InputReader reader;

    private int maxExecutions = 100;
    private int maxInstructions = 1000;

    public InterpreterOptions(){
    }

    public void newExecution() {
        if (textUserEntries) {
            if (textFileNumber < textFilesName.length - 1) {
                reader = new TextInputReader(textFilesName[++textFileNumber]);
            } else {
                textUserEntries = false;
            }
        } else if (binaryUserEntries) {
            if (binaryFileNumber < binaryFilesName.length-1) {
                reader = new BinaryInputReader(textFilesName[++binaryFileNumber]);
            }  else {
                binaryUserEntries = false;
            }
        }
    }

    public void prepareCoverage() {
        exploredBlocks = new HashSet<>();
        exploredInstructions = new HashSet<>();
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

    public Long readIntFromFile() throws NumberFormatException {
        if(!textUserEntries && !binaryUserEntries) {return null;}
        if(eof) {return -1L;}

        Long value;

        try {
            value = reader.readInt();
            if (value == null) {
                eof = true;
                value = -1L;
            }

        } catch (IOException e) {
            System.out.println("Error while reading int from file");
            value = null;
        }

        return value;
    };

    public Long readCharFromFile() {
        if(!textUserEntries && !binaryUserEntries) {return null;}
        if(eof) {return -1L;}

        Long value;

        try {
            value = reader.readChar();
        } catch (IOException e) {
            System.out.println("Error while reading char from file");
            value = null;
        }

        if (value == -1) {
            eof = true;
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

    public void countBlockCoverage(BasicBlock block) {
        if(!block.instructions.get(0).getSourceProgram().getFilename().equals(GenericInterpreter.LIBS_FILENAME))
            exploredBlocks.add(block.id);

        if(heuristic == Heuristics.COVERAGE) {
            coverageCounter.merge(block, 1, Integer::sum); //Add 1 to the counter or create the node and give the value 1
        }
    }

    public void countInstructionCoverage(ProgramStatement ps) {
        if(!ps.getSourceProgram().getFilename().equals(GenericInterpreter.LIBS_FILENAME))
            exploredInstructions.add(ps.getAddress());
    }

    public void printCoverage(CFG cfg) {
        float totalBlocks = 0;
        float totalInstructions = 0;

        for (BasicBlock block : cfg.blocks) {
            if (!block.instructions.get(0).getSourceProgram().getFilename().equals(GenericInterpreter.LIBS_FILENAME)) {
                totalBlocks++;
                totalInstructions += block.instructions.size();
            }
        }

        System.out.printf("Covered blocks: %.2f %%\n", exploredBlocks.size() / totalBlocks * 100);
        System.out.printf("Covered instructions: %.2f %%\n", exploredInstructions.size() / totalInstructions * 100);
    }

    public int getCoverage(BasicBlock block) {
        return  coverageCounter.getOrDefault(block, 0);
    }

    public void setStartingAddress(int startingAddress) {
        this.startingAddress = startingAddress;
    }

    public void setEndingAddress(int endingAddress) {
        this.endingAddress = endingAddress;
    }

    public byte[] getSavedMemory(Memory memory) {
        return memory.getMemory(startingAddress, endingAddress);
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
            case "--text-entries":
                textUserEntries = true;
                try {
                    textFilesName = new String[Integer.parseInt(args[++i])];
                    for(int j = 0; j < textFilesName.length; ++j){
                        textFilesName[j] = args[++i];
                    }
                    textFileNumber = 0;
                } catch (NumberFormatException e) {
                    System.out.println("Wrong parameter for option --text-entries: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
                }
                break;
            case "c":
                binaryUserEntries = true;
                try {
                    binaryFilesName = new String[Integer.parseInt(args[++i])];
                    for(int j = 0; j < binaryFilesName.length; ++j){
                        binaryFilesName[j] = args[++i];
                    }
                    binaryFileNumber = 0;
                } catch (NumberFormatException e) {
                    System.out.println("Wrong parameter for option --binary-entries: " + args[i]);
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
            case "--save-memory":
                saveMemory = true;
                File d = new File(ConcolicInterpreter.MEMORY_SAVE_FOLDER);
                try {
                    Files.createDirectories(d.toPath());
                    for (File f : d.listFiles())
                        if (!f.isDirectory())
                            f.delete();
                } catch (IOException e) {
                    System.out.println("Error creating memory save folder: " + e.getMessage());
                }

                try {
                    startingAddress = Long.decode(args[++i]).intValue();
                    endingAddress = Long.decode(args[++i]).intValue();
                } catch (NumberFormatException e) {
                    System.out.println("Wrong parameter for option --save-memory: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
                }
                break;
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
