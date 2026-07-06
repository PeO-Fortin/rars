package rars.concolic;

import rars.Globals;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import rars.cfg.BasicBlock;
import rars.riscv.InstructionSet;
import rars.riscv.hardware.AddressErrorException;

public class ConcolicInterpreter extends GenericInterpreter<ConcolicValues.V> {

    public static void main(String[] args) throws Exception {
        InterpreterOptions opt = new InterpreterOptions();
        opt.checkOptions(args);
        initRars();

        runInterpreter(args[0], opt);
    }

    public static void initRars() {
        Globals.initialize();
        InstructionSet.rv64 = true;
        Globals.instructionSet.populate();
    }

    public static void runInterpreter(String program, InterpreterOptions options) throws Exception {
        ConcolicInterpreter interpreter = new ConcolicInterpreter();
        interpreter.prepare(program);
        interpreter.options = options;
        interpreter.runConcolic(options.getMaxExecutions());
        System.out.printf("edges covered: %d\n", interpreter.edgesCovered.size());
    }

    public ConcolicInterpreter() {
        super(new ConcolicValues());
    }

    public ExecutionTreeNode executionTreeRoot = new ExecutionTreeNode(0);
    public ExecutionTreeNode currentNode = executionTreeRoot;
    int nextId = 0;
    Map<BasicBlock, Integer> distanceToExit;

    @Override
    protected void if_(ConcolicValues.V cond) {
        currentNode.condition = cond.symbolic;
        currentNode.block = currentBlock;
        if (distanceToExit != null) {
            currentNode.distanceToExit = distanceToExit.getOrDefault(currentBlock, Integer.MAX_VALUE);
        }
        if (!currentNode.hasChildren()) {
            currentNode.trueBranch = new ExecutionTreeNode(++nextId);
            currentNode.trueBranch.parent = currentNode;
            currentNode.falseBranch = new ExecutionTreeNode(++nextId);
            currentNode.falseBranch.parent = currentNode;
        }
        if (values.isTruthy(cond)) {
            currentNode = currentNode.trueBranch;
        } else {
            currentNode = currentNode.falseBranch;
        }
        super.if_(cond);
    }

    int lastReadCharacter = 0;
    @Override
    protected ConcolicValues.V readChar() {
        String symbol = "readChar_" + lastReadCharacter++;
        // The result from readChar is between -1 (included) and 127 (included)
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicLong(-2), new SymbolicVariable(symbol) }));
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicLong(128) }));

        ConcolicValues.V value = options.readFromFile();

        if (value == null) {
            value = getFromModel(symbol, -1);
        }

        return value;
    }

    int lastReadInteger = 0;
    @Override
    protected ConcolicValues.V readInt() {
        String symbol = "readInt_" + lastReadInteger++;

        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicLong(Integer.MIN_VALUE - 1L), new SymbolicVariable(symbol) }));
        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicLong(Integer.MAX_VALUE + 1L) }));

        ConcolicValues.V value = options.readFromFile();

        if (value == null) {
            value = getFromModel(symbol, -1);
        }

        return value;
    }

    int lastReadString = 0;
    @Override
    protected String readString(ConcolicValues.V bufAddress, ConcolicValues.V length) {
        String lenSymbol = "readString_" + lastReadString + "_len";
        String strSymbol = "readString_" + lastReadString++;

        String value = options.readStringFromFile(length.concrete);
        long modelLength;

        if(value != null) {
            modelLength = value.length() + 1;
        } else {
            modelLength = modelizedLength(lenSymbol, length).concrete;
        }

        int i = 0;
        for(; i < modelLength - 1; ++i){
            ConcolicValues.V ch;
            if(value != null) {
                ch = values.inject(value.charAt(i));
            } else {
                String charSymbol = strSymbol + "_char_" + i;
                ch = readCharforString(charSymbol);
            }
            sb(ch, values.inject(i), bufAddress);
        }
        sb(values.inject(0), values.inject(i), bufAddress);
        return value;
    }

    private ConcolicValues.V readCharforString(String symbol) {
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicLong(31), new SymbolicVariable(symbol) }));
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicLong(127) }));

        return getFromModel(symbol, 32);
    }

    private ConcolicValues.V modelizedLength(String lenSymbol, ConcolicValues.V length) {
        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Geq,
                new SymbolicValue[]{ new SymbolicVariable(lenSymbol), new SymbolicLong(0) }));
        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(lenSymbol), length.symbolic }));

        return getFromModel(lenSymbol, 0);
    }

    @Override
    protected void sb(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.BYTE, true);
            memory.storeMemory(bValue, memAddress, offset);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            exit = true;
        }
    }

    @Override
    protected void sh(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue memValue = new MemoryValue(value, MemoryValueTypes.HALFWORD, true);
            memory.storeMemory(memValue, offset, memAddress);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            exit = true;
        }
    }

    @Override
    protected void sw(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.WORD, true);
            memory.storeMemory(bValue, memAddress, offset);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            exit = true;
        }
    }

    @Override
    protected void sd(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.DOUBLEWORD, true);
            memory.storeMemory(bValue, memAddress, offset);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            exit = true;
        }
    }

    @Override
    protected ConcolicValues.V lb(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.BYTE, false);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V lbu(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.BYTE, true);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V lh(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.HALFWORD, false);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V lhu(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.HALFWORD, true);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V lw(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.WORD, false);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V lwu(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.WORD, true);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    @Override
    protected ConcolicValues.V ld(ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.DOUBLEWORD, false);
            memory.accessMemory(bValue, memAddress, offset);
            return bValue.getConcolicValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory|";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + "|";
            return null;
        }
    }

    ConcolicValues.V getFromModel(String symbol, int defaultValue) {
        ConcolicValues.V v = ConcolicValues.variable(model.getOrDefault(symbol, defaultValue), symbol);
        return v;
    }

    Map<String, Integer> model = new HashMap<>();
    public void runConcolic(int maxExecutions) {
        this.distanceToExit = options.calculateDistanceToExit(cfg);
        if (this.distanceToExit != null) {
            executionTreeRoot.distanceToExit = this.distanceToExit.get(
                    cfg.entryBlock
            );
        }
        int execution = 0;
        try {
            do {
                lastReadCharacter = 0;
                lastReadInteger = 0;
                options.newExecution();
                currentNode = executionTreeRoot;
                computeNextModel();
                super.input = "|"; super.output = "|";
                runMain();
                currentNode.explored = true;
                printResults(execution);
                ++execution;
            } while (execution < maxExecutions);
        } catch (ExecutionDone e) {
            System.out.println("Execution completed");
        }
    }

    public static final String INPUT_FOLDER_NAME = "src/rars/concolic/results/inputs/";
    public static final String OUTPUT_FOLDER_NAME = "src/rars/concolic/results/outputs/";
    public static final String INPUT_FILE_NAME = "inputs";
    public static final String OUTPUT_FILE_NAME = "outputs";

    private void printResults(int execution) {
        try {
            String paddedExecution = String.format("%05d", execution);
            PrintWriter pwInputs = new PrintWriter(new FileWriter(INPUT_FOLDER_NAME + INPUT_FILE_NAME + paddedExecution));
            PrintWriter pwOutputs = new PrintWriter(new FileWriter(OUTPUT_FOLDER_NAME + OUTPUT_FILE_NAME + paddedExecution));
            pwInputs.println(super.input); pwOutputs.println(super.output);
            pwInputs.flush(); pwOutputs.flush();
            pwInputs.close(); pwOutputs.close();
        } catch (IOException e) {
            System.out.println("IO Error Print Results" + execution);
        }
    }

    private static class ExecutionDone extends RuntimeException {}
    ConstraintSolver solver = new ConstraintSolver();
    public void computeNextModel() {
        ExecutionTreeNode next = executionTreeRoot.nextUnexplored();
        currentNode = next;
        if (next == null) {
            throw new ExecutionDone();
        }
        model = solver.solve(next.collectConstraints(next));
        if (model == null) {
            // unsat!
            next.unsat = true;
            computeNextModel();
        }
    }

    public Collection<FuzzingEdge> edgesCovered = new HashSet<>();
    @Override
    protected void setCurrentBlockCond(BasicBlock target) {
        edgesCovered.add(new FuzzingEdge(currentBlock, target));
        super.setCurrentBlock(target);
    }
}

class ExecutionTreeNode {
    public ExecutionTreeNode parent;
    public SymbolicValue condition;
    public ExecutionTreeNode trueBranch;
    public ExecutionTreeNode falseBranch;
    public boolean unsat = false;
    public Collection<SymbolicValue> extraConstraints = new HashSet<>();
    public boolean explored = false;
    BasicBlock block;
    public Integer distanceToExit;
    public int id;
    public ExecutionTreeNode(int id) {
        this.id = id;
    }

    public boolean isUnexplored() {
       return !hasChildren() && !explored && !unsat; }

    public boolean hasChildren() {
        return trueBranch != null && falseBranch != null;
    }

    public boolean hasUnexploredNode() {
        if (isUnexplored()) {
            return true;
        } else if (trueBranch != null && falseBranch != null) {
            return trueBranch.hasUnexploredNode() || falseBranch.hasUnexploredNode();
        } else {
            return false;
        }
    }

    public ExecutionTreeNode nextUnexploredDFS() {
        if (isUnexplored()) return this;
        if (trueBranch != null && falseBranch != null) {
            ExecutionTreeNode f = falseBranch.nextUnexplored();
            if (f != null) return f;
            ExecutionTreeNode t = trueBranch.nextUnexplored();
            if (t != null) return t;
        }
        return null;
    }

    public ExecutionTreeNode nextUnexploredBFS() {
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.isUnexplored()) return node;
            if (node.falseBranch != null) worklist.add(node.falseBranch);
            if (node.trueBranch != null) worklist.add(node.trueBranch);
        }
        return null;
    }

    public ExecutionTreeNode nextUnexploredExit() {
        List<ExecutionTreeNode> candidates = new ArrayList<>();
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        ExecutionTreeNode best = null;
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.isUnexplored()) {
                candidates.add(node);
            } else {
                if (node.falseBranch != null) worklist.add(node.falseBranch);
                if (node.trueBranch != null) worklist.add(node.trueBranch);
            }
        }

        int bestScore = Integer.MAX_VALUE;
        for (ExecutionTreeNode candidate : candidates) {
            int score;
            if (candidate.parent == null) {
                score = candidate.distanceToExit != null ? candidate.distanceToExit : Integer.MAX_VALUE;
            } else {
                score = candidate.parent.distanceToExit != null ? candidate.parent.distanceToExit : Integer.MAX_VALUE;
            }
            if (best == null || score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    public ExecutionTreeNode nextUnexploredRandom() {
        ExecutionTreeNode node = this;
        Random rand = new Random();

        while (!node.isUnexplored()) {
            List<ExecutionTreeNode> availableBranches = new ArrayList<>();
            if (node.trueBranch != null) availableBranches.add(node.trueBranch);
            if (node.falseBranch != null) availableBranches.add(node.falseBranch);

            if (availableBranches.isEmpty()) return null;

            node = availableBranches.get(rand.nextInt(availableBranches.size()));
        }

        return node;

    }

    public ExecutionTreeNode nextUnexploredCoverage() {
        List<ExecutionTreeNode> candidates = new ArrayList<>();
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        ExecutionTreeNode best = null;
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.isUnexplored()) {
                candidates.add(node);
            } else {
                if (node.falseBranch != null) worklist.add(node.falseBranch);
                if (node.trueBranch != null) worklist.add(node.trueBranch);
            }
        }

        int minCoverage = 0;
        for (ExecutionTreeNode candidate : candidates) {
            int coverage = ConcolicInterpreter.options.getCoverage(candidate.block);

            if (coverage <= minCoverage) {
                minCoverage = coverage;
                best = candidate;
            }
        }
        return best;
    }

    public ExecutionTreeNode nextUnexplored() {
        ExecutionTreeNode result;
        Heuristics heur = ConcolicInterpreter.options.getHeuristic();
        switch (heur) {
            case BFS:
            default:
                result = nextUnexploredBFS();
                break;
            case DFS:
                result = nextUnexploredDFS();
                break;
            case TO_EXIT:
                result = nextUnexploredExit();
                break;
            case RANDOM:
                result = nextUnexploredRandom();
                break;
            case COVERAGE:
                result = nextUnexploredCoverage();
                break;
        }
        return result;
    }

    public int size() {
        int size = 1;
        if (trueBranch != null) size += trueBranch.size();
        if (falseBranch != null) size += falseBranch.size();
        return size;
    }

    public Collection<SymbolicValue> collectConstraints(ExecutionTreeNode comingFrom) {
        Collection<SymbolicValue> constraints;
        if (parent != null) {
            constraints = parent.collectConstraints(this);
        } else {
            constraints = new HashSet<>();
        }
        constraints.addAll(this.extraConstraints);
        if (condition != null) {
            if (comingFrom == trueBranch) {
                constraints.add(condition);
            } else {
                SymbolicValue[] args = { condition };
                constraints.add(new SymbolicOperation(SymbolicOperator.Not, args));
            }
        }
        return constraints;
    }

    public boolean isBlockInPath(BasicBlock target) {
        ExecutionTreeNode cur = this;
        while (cur != null) {
            if (cur.block == target) return true;
            cur = cur.parent;
        }
        return false;
    }
}
