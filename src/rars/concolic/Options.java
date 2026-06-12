package rars.concolic;


import rars.cfg.BasicBlock;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private final String HELP = "Available options:\n" +
            "--help :\tDisplay available options\n" +
            "--unrolling :\tActivate unrolling\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions";


    boolean unrolling;
    boolean dfs;

    //Unrolling variables
    private int maxLoopIterations;
    private Map<Integer, Integer> loopIterations;

    private int maxExecutions = 300;
    private int maxInstructions = 500;

    Options(String[] args){
        if (args.length < 2){
            unrolling = false;
        } else {
            checkOptions(args);
        }
    }

    public void newExecution(){
        if (unrolling){
            ++maxLoopIterations;
        }
    }

    public int getMaxExecutions(){
        return maxExecutions;
    }

    public int getMaxInstructions(){
        return maxInstructions;
    }

    public BasicBlock unrollingTarget(ExecutionTreeNode currentNode, BasicBlock target) {
        if (unrolling){
            if (currentNode.isBlockInPath(target)) {
                int address = target.getStartAddress();
                int iterations = loopIterations.getOrDefault(address, 0);
                if (iterations >= maxLoopIterations) {
                    target = currentNode.block.fallthroughSuccessor;
                } else {
                    loopIterations.put(address, iterations + 1);
                }
            }
        }
        return(target);
    }

    private void checkOptions(String[] args){
        for(int i = 2; i < args.length; ++i){
            switch (args[i]){
                case "--help":
                    System.out.println(HELP);
                case "--unrolling":
                    unrolling = true;
                    maxLoopIterations = -1;
                    loopIterations = new HashMap<>();
                    break;
                case "--dfs":
                    dfs = true;
                    break;
                case "--max-exec":
                    maxExecutions = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.out.println("Unknown option: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
            }
        }
    }
}
