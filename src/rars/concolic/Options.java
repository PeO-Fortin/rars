package rars.concolic;


import rars.cfg.BasicBlock;

public class Options {
    private final String HELP = "Available options:\n" +
            "--help :\tDisplay available options\n" +
            "--iterative-deepening :\tActivate iterative deepening\n" +
            "--max-exec [value] :\tDefine a maximum number of executions\n" +
            "--max-inst [value] :\tDefine a maximum number of instructions";


    boolean iterativeDeepening = false;
    boolean dfs;

    //Iterative deepening variables
    private int loopLimit = -1;;

    private int maxExecutions = 300;
    private int maxInstructions = 500;

    Options(String[] args){
        if (args.length >= 2){
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

    private void checkOptions(String[] args){
        for(int i = 2; i < args.length; ++i){
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
                default:
                    System.out.println("Unknown option: " + args[i]);
                    System.out.println("Try option '--help'");
                    System.exit(1);
            }
        }
    }
}
