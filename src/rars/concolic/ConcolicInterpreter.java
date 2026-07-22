package rars.concolic;

import rars.Globals;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import rars.cfg.BasicBlock;
import rars.options.InterpreterOptions;
import rars.riscv.InstructionSet;
import rars.riscv.hardware.AddressErrorException;

public class ConcolicInterpreter extends GenericInterpreter<ConcolicValues.V> {

    public static void main(String[] args) throws Exception {
        InterpreterOptions opt = new InterpreterOptions();
        opt.checkOptions(args);
        runInterpreter(args[0], opt);
    }

    public static void initRars() {
        Globals.initialize();
        InstructionSet.rv64 = true;
        Globals.instructionSet.populate();
    }

    public static void runInterpreter(String program, InterpreterOptions options) throws Exception {
        initRars();
        ConcolicInterpreter interpreter = new ConcolicInterpreter();
        interpreter.prepare(program);
        interpreter.options = options;
        createResultsFolders();
        interpreter.runConcolic(options.getMaxExecutions());
        System.out.printf("edges covered: %d\n", interpreter.edgesCovered.size());
    }

    public static final String INPUT_READABLE_FOLDER_NAME = "src/rars/concolic/results/inputs/readable";
    public static final String INPUT_BINARY_FOLDER_NAME = "src/rars/concolic/results/inputs/binaries";
    public static final String OUTPUT_FOLDER_NAME = "src/rars/concolic/results/outputs/";
    public static final String INPUT_FILE_NAME = "inputs";
    public static final String OUTPUT_FILE_NAME = "outputs";

    private static void createResultsFolders() {
        String[] folders = {
                INPUT_READABLE_FOLDER_NAME,
                INPUT_BINARY_FOLDER_NAME,
                OUTPUT_FOLDER_NAME
        };

        try {
            for (String folder : folders) {
                Path path = Paths.get(folder);

                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.out.println("Error creating results folders: " + e.getMessage());
        }
    }

    public ConcolicInterpreter() {
        super(new ConcolicValues());
    }

    Map<BasicBlock, Integer> distanceToExit;
    boolean lastTruthDecision = true;

    @Override
    protected void if_(ConcolicValues.V cond) {
        NodeKey key = new NodeKey(currentBlock, callStack);
        ExecutionTreeNode node = executionTree.getOrCreate(key);

        if(node.condition == null) {
            node.constraints = new ArrayList<>(constraints);
            node.condition = cond.symbolic;
        }
        node.block = currentBlock;

        if (distanceToExit != null) {
            node.distanceToExit = distanceToExit.getOrDefault(currentBlock, Integer.MAX_VALUE);
        }

        if(node != executionTree.root) {
            if (lastTruthDecision == true) {
                executionTree.previousNode.trueBranch = node;
            } else {
                executionTree.previousNode.falseBranch = node;
            }
        }

        boolean truthDecision = values.isTruthy(cond);

        if(truthDecision) {
            constraints.add(cond.symbolic);
        } else {
            SymbolicValue[] args = {cond.symbolic};
            constraints.add(new SymbolicOperation(SymbolicOperator.Not, args));
        }

        lastTruthDecision = truthDecision;
        executionTree.previousNode = node;

        super.if_(cond);
    }

    int lastReadCharacter = 0;
    @Override
    protected ConcolicValues.V readChar() {
        String symbol = "readChar_" + lastReadCharacter++;
        ConcolicValues.V concValue;

        if(execution > options.getMaxExecutions() * 0.9) {
            //All possible ASCII
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicLong(-2), new SymbolicVariable(symbol)}));
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicVariable(symbol), new SymbolicLong(128)}));
        } else {
            //Only printable ASCII
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicLong(31), new SymbolicVariable(symbol)}));
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicVariable(symbol), new SymbolicLong(127)}));
        }

        Long value = options.readCharFromFile();

        if (value != null) {
            concValue = new ConcolicValues.V(value, new SymbolicLong(value));
        } else {
            concValue = getFromModel(symbol, 0);
        }

        return concValue;
    }

    int lastReadInteger = 0;
    @Override
    protected ConcolicValues.V readInt() {
        String symbol = "readInt_" + lastReadInteger++;
        ConcolicValues.V concValue;

        if(execution > options.getMaxExecutions() * 0.9) {
            //All values
            // NOTE: Even if the syscall is called readInt, in 64bits, rars reads a Long
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicLong(Long.MIN_VALUE), new SymbolicVariable(symbol)}));
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicVariable(symbol), new SymbolicLong(Long.MAX_VALUE)}));
        } else {
            //Limited range
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicLong(-101), new SymbolicVariable(symbol)}));
            constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                    new SymbolicValue[]{new SymbolicVariable(symbol), new SymbolicLong(101)}));
        }

        Long value = null;

        try {
            value = options.readIntFromFile();
        } catch (NumberFormatException e) {
            output += "Runtime exception: invalid input integer";
            exit = true;
        }

        if (value != null) {
            concValue = new ConcolicValues.V(value, new SymbolicLong(value));
        } else {
            concValue = getFromModel(symbol, 0);
        }

        return concValue;
    }

    int lastReadString = 0;
    @Override
    protected String readString(ConcolicValues.V bufAddress, ConcolicValues.V length) {
        String lenSymbol = "readString_" + lastReadString + "_len";
        String strSymbol = "readString_" + lastReadString++;

        String value = "";
        long modelLength;

        modelLength = modelizedLength(lenSymbol, length).concrete;

        int i = 0;
        for(; i < modelLength - 1; ++i){
            ConcolicValues.V ch;
            String charSymbol = strSymbol + "_char_" + i;
            ch = readCharforString(charSymbol);
            value += values.asChar(ch);

            sb(ch, values.inject(i), bufAddress);
        }
        sb(values.inject(0), values.inject(i), bufAddress);
        return value;
    }

    private ConcolicValues.V readCharforString(String symbol) {
        constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicLong(31), new SymbolicVariable(symbol) }));
        constraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicLong(127) }));

        return getFromModel(symbol, 32);
    }

    private ConcolicValues.V modelizedLength(String lenSymbol, ConcolicValues.V length) {
        constraints.add( new SymbolicOperation(SymbolicOperator.Geq,
                new SymbolicValue[]{ new SymbolicVariable(lenSymbol), new SymbolicLong(0) }));
        constraints.add( new SymbolicOperation(SymbolicOperator.Lt,
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

    int execution = 0;
    Map<String, Integer> model = new HashMap<>();
    Collection<SymbolicValue> constraints = new HashSet<>();
    ExecutionTree executionTree = new ExecutionTree();
    public void runConcolic(int maxExecutions) {
        this.distanceToExit = options.calculateDistanceToExit(cfg);
        if (this.distanceToExit != null) {
            executionTree.root.distanceToExit = this.distanceToExit.get(
                    cfg.entryBlock
            );
        }
        try {
            do {
                lastReadCharacter = 0;
                lastReadInteger = 0;
                constraints.clear();
                callStack.clear();
                options.newExecution();
                computeNextModel();
                super.inputReadable = "|"; super.output = "|";
                runMain();
                printResults(execution);
                ++execution;
            } while (execution < maxExecutions);
        } catch (ExecutionDone e) {
            System.out.println("Execution completed");
        }
    }

    private void printResults(int execution) {
        try {
            String paddedExecution = String.format("%05d", execution);
            PrintWriter pwReadableInputs = new PrintWriter(new FileWriter(INPUT_READABLE_FOLDER_NAME + INPUT_FILE_NAME + paddedExecution));
            PrintWriter pwReadableOutputs = new PrintWriter(new FileWriter(OUTPUT_FOLDER_NAME + OUTPUT_FILE_NAME + paddedExecution));
            byte[] bytesResults = inputBytesArray.toByteArray();

            pwReadableInputs.println(super.inputReadable); pwReadableOutputs.println(super.output);
            pwReadableInputs.flush(); pwReadableOutputs.flush();
            pwReadableInputs.close(); pwReadableOutputs.close();

            FileOutputStream fos = new FileOutputStream(INPUT_BINARY_FOLDER_NAME + INPUT_FILE_NAME + paddedExecution);
            fos.write(bytesResults);
        } catch (IOException e) {
            System.out.println("IO Error Print Results" + execution);
        }
    }

    private static class ExecutionDone extends RuntimeException {}
    ConstraintSolver solver = new ConstraintSolver();
    public void computeNextModel() {
        UnexploredEdge next = executionTree.root.nextUnexplored();
        List<SymbolicValue> constraints;

        if (!executionTree.rootDefined) { //first execution
            constraints = new ArrayList<>();
        } else if (next == null) {
            throw new ExecutionDone();
        } else {
            constraints = next.node.constraints;
            if(next.direction == true) {
                constraints.add(next.node.condition);
            } else {
                SymbolicValue[] args = {next.node.condition};
                constraints.add(new SymbolicOperation(SymbolicOperator.Not, args));
            }
        }

        model = solver.solve(constraints);

        if (model == null) {
            //unsat
            if(next.direction == true) next.node.trueBranchUnsat = true;
            else next.node.falseBranchUnsat = true;
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
