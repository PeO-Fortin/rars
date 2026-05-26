package rars.concolic;

import rars.Globals;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import rars.Settings;
import rars.riscv.hardware.AddressErrorException;

public class ConcolicInterpreter extends GenericInterpreter<ConcolicValues.V> {
    public static void main(String[] args) throws Exception {
        Globals.initialize();
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, true);
        Globals.instructionSet.populate();
        ConcolicInterpreter interpreter = new ConcolicInterpreter();
        interpreter.prepare(args[0]);
        interpreter.runConcolic(Integer.parseInt(args[1]));
        System.out.printf("edges covered: %d\n", interpreter.edgesCovered.size());
    }

    public ConcolicInterpreter() {
        super(new ConcolicValues());
    }

    public ExecutionTreeNode executionTreeRoot = new ExecutionTreeNode(0);
    public ExecutionTreeNode currentNode = executionTreeRoot;
    int nextId = 0;

    @Override
    protected void if_(ConcolicValues.V cond, int condOffset) {
        currentNode.condition = cond.symbolic;
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
        super.if_(cond, condOffset);
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
        return getFromModel(symbol, -1);
    }

    int lastReadInteger = 0;
    @Override
    protected ConcolicValues.V readInt() {
        String symbol = "readInt_" + lastReadInteger++;

        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicLong(Integer.MIN_VALUE - 1L), new SymbolicVariable(symbol) }));

        currentNode.extraConstraints.add( new SymbolicOperation(SymbolicOperator.Lt,
                new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicLong(Integer.MAX_VALUE + 1L) }));

        return getFromModel(symbol, 0);
    }

    /**
     * TODO
     **/
    int lastReadString = 0;
    @Override
    protected void readString(ConcolicValues.V bufAddress, ConcolicValues.V length) {
        String symbol = "readString_" + lastReadString++;

    }

    @Override
    protected void sb(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.BYTE, true);
            memory.storeMemory(bValue, values.asLong(memAddress), values.asLong(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sh(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.HALFWORD, true);
            memory.storeMemory(bValue, values.asLong(memAddress), values.asLong(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sw(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.WORD, true);
            memory.storeMemory(bValue, values.asLong(memAddress), values.asLong(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sd(ConcolicValues.V value, ConcolicValues.V offset, ConcolicValues.V memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.DOUBLEWORD, true);
            memory.storeMemory(bValue, values.asLong(memAddress), values.asLong(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    ConcolicValues.V getFromModel(String symbol, int defaultValue) {
        ConcolicValues.V v = ConcolicValues.variable(model.getOrDefault(symbol, defaultValue), symbol);
        return v;
    }

    Map<String, Integer> model = new HashMap<>();
    public void runConcolic(int maxIterations) {
        int iteration = 0;
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("Results.txt"));
            do {
                currentProgramCounter = machineList.get(0).getAddress();
                lastReadCharacter = 0;
                lastReadInteger = 0;
                pw.println("***********************");
                pw.println("Iteration: " + iteration);
                currentNode = executionTreeRoot;
                computeNextModel();
                super.output = "| ";
                super.input = "| ";
                runMain();
                currentNode.explored = true;
                pw.println("Input(s): " + super.input);
                pw.println("Output(s): " + super.output);
                pw.println("***********************");
                pw.println();
                pw.flush();
                iteration++;
            } while (iteration < maxIterations);
            pw.close();
        } catch (ExecutionDone e) {
            System.out.println("Execution completed");
        } catch (IOException e) {
            System.out.println("IO Error");
        }
    }

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

    private static class ExecutionDone extends RuntimeException {}

    public Collection<FuzzingEdge> edgesCovered = new HashSet<>();
    @Override
    protected void setCurrentPcCond(int condOffset) {
        edgesCovered.add(new FuzzingEdge(currentProgramCounter, currentProgramCounter + condOffset));
        super.setCurrentPcCond(condOffset);
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
            ExecutionTreeNode t = trueBranch.nextUnexplored();
            if (t != null) return t;
            ExecutionTreeNode f = falseBranch.nextUnexplored();
            if (f != null) return f;
        }
        return null;
    }

    public ExecutionTreeNode nextUnexploredBFS() {
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.isUnexplored()) return node;
            if (node.trueBranch != null) worklist.add(node.trueBranch);
            if (node.falseBranch != null) worklist.add(node.falseBranch);
        }
        return null;
    }

    public ExecutionTreeNode nextUnexplored() {
        ExecutionTreeNode result = nextUnexploredBFS();
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
}
