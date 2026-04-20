package rars.concolic;

import minic.front.*;
import minic.fuzzing.Edge;
import minic.ir.*;
import minic.lib.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ConcolicInterpreter extends GenericInterpreter<ConcolicValues.V> {
    public static void main(String[] args) throws Exception {
        ConcolicInterpreter interpreter = new ConcolicInterpreter();
        interpreter.prepare(args[0]);
        interpreter.runConcolic(Integer.parseInt(args[1]));
        System.out.printf("edges covered: %d\n", interpreter.edgesCovered.size());
    }

    Random random = new Random();
    public ConcolicInterpreter() {
        super(new ConcolicValues());
    }

    public ExecutionTreeNode executionTreeRoot = new ExecutionTreeNode(0);
    public ExecutionTreeNode currentNode = executionTreeRoot;
    int nextId = 0;
    Map<BasicBlock, Integer> distanceToError;

    @Override
    protected void if_(ConcolicValues.V cond, BasicBlock then, BasicBlock else_) {
        currentNode.condition = cond.symbolic;
        currentNode.block = currentBlock;
        if (distanceToError != null) {
            currentNode.distanceToError = distanceToError.getOrDefault(currentBlock, 10000);
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
        super.if_(cond, then, else_);
    }

    int lastReadCharacter = 0;
    @Override
    protected ConcolicValues.V getchar() {
        String symbol = "getchar_" + lastReadCharacter++;
        // The result from getchar is between -1 (included) and 255 (included)
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                                                               new SymbolicValue[]{ new SymbolicInteger(-2), new SymbolicVariable(symbol) }));
        currentNode.extraConstraints.add(new SymbolicOperation(SymbolicOperator.Lt,
                                                               new SymbolicValue[]{ new SymbolicVariable(symbol), new SymbolicInteger(256) }));
        // To better deal with program reading from stdin until EOF, we default to EOF as the value for getchar
        return getFromModel(symbol, -1);
    }

    ConcolicValues.V getFromModel(String symbol, int defaultValue) {
        ConcolicValues.V v = ConcolicValues.variable(model.getOrDefault(symbol, defaultValue), symbol);
        return v;
    }

    Map<String, Integer> model = new HashMap<>();
    public void runConcolic(int maxIterations) {
        this.distanceToError = calculateDistanceToError();
        if (this.distanceToError != null) {
            executionTreeRoot.distanceToError = this.distanceToError.get(
                    functions.get("main").cfg.entryBlock
            );
        }
        int iteration = 0;
        try {
            do {
                currentNode = executionTreeRoot;
                lastReadCharacter = 0;
                computeNextModel();
                ConcolicValues.V result = runMain(getArguments());
                currentNode.result = result.concrete;
                dumpDot("concolic." + iteration + ".dot");
                iteration++;
                if (currentNode.result == 1) {
                    System.out.println("Program return 1");
                    printUsedStdIn();
                    break;
                }
            } while (iteration < maxIterations);
        } catch (ExecutionDone e) {
            dumpDot("concolic.final.dot");
        }
    }

    Map<BasicBlock, Integer> calculateDistanceToError() {
        Map<BasicBlock, Integer> distances = new HashMap<>();
        Queue<BasicBlock> worklist = new LinkedList<>();

        for (Function function : functions.values()) {
            for (BasicBlock block : function.cfg.blocks) {
                for (IR ir : block.instructions) {
                    if (ir.instr == Instr.Call && "error".equals(ir.symbol)) {
                        distances.put(block, 0);
                        worklist.add(block);
                    }
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

    public void printUsedStdIn() {
        System.out.println("Standard input:");
        for (int i = 0; i < lastReadCharacter; i++) {
            int input = getFromModel("getchar_" + i, -1).concrete;
            if (input == -1) break;
            System.out.print(input + " ");
        }
        System.out.println();
    }

     List<ConcolicValues.V> getArguments() {
        List<ConcolicValues.V> arguments = new ArrayList<ConcolicValues.V>();
        Function main = functions.get("main");
        for (Variable arg : main.parameters) {
            arguments.add(getFromModel(arg.name, random.nextInt()));
        }
        return arguments;
    }

    ConstraintSolver solver = new ConstraintSolver();
    public void computeNextModel() {
        ExecutionTreeNode next = executionTreeRoot.nextUnexplored();
        if (next == null) {
            throw new ExecutionDone();
        }
        model = solver.solve(next.collectConstraints(null));
        if (model == null) {
            // unsat!
            next.unsat = true;
            computeNextModel();
        }
    }


    public void dumpDot(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write("digraph {\n");
            executionTreeRoot.dumpDot(fw);
            fw.write("}\n");
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ExecutionDone extends RuntimeException {}

    public Collection<Edge> edgesCovered = new HashSet<>();
    @Override
    protected void setCurrentBlockCond(BasicBlock target) {
        edgesCovered.add(new Edge(currentBlock, target));
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
    public Integer result;
    public BasicBlock block;
    public Integer distanceToError;
    public int id;
    public ExecutionTreeNode(int id) {
        this.id = id;
    }

    public void dumpDot(FileWriter fw) throws IOException {
        String label = "";
        if (condition != null) {
            label += condition.toString();
        } else if (result != null)  {
            label += result.toString();
        } else if (unsat) {
            label += "unsat";
        } else {
            label += "unexplored";
        }
        fw.write("node_" + id + "[shape=plaintext,label=<" + id + ":" + CFGraphviz.escape(label) + ">];\n");
        if (trueBranch != null && falseBranch != null) {
            fw.write("node_" + id + " -> " + "node_" + trueBranch.id + "[label=T];\n");
            trueBranch.dumpDot(fw);
            fw.write("node_" + id + " -> " + "node_" + falseBranch.id + "[label=F];\n");
            falseBranch.dumpDot(fw);
        }
    }

    public boolean isUnexplored() {
        return result == null && condition == null && !unsat;
    }

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

    public ExecutionTreeNode nextUnexploredError() {
        List<ExecutionTreeNode> candidates = new ArrayList<>();
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        ExecutionTreeNode best = null;
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.isUnexplored()) {
                candidates.add(node);
            } else {
                if (node.trueBranch != null) worklist.add(node.trueBranch);
                if (node.falseBranch != null) worklist.add(node.falseBranch);
            }
        }

        int bestScore = 10000;
            for (ExecutionTreeNode candidate : candidates) {
                int score;
                if (candidate.parent == null) {
                    score = candidate.distanceToError != null ? candidate.distanceToError : 10000;
                } else {
                    score = candidate.parent.distanceToError != null ? candidate.parent.distanceToError : 10000;
                }
                if (best == null || score < bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        return best;
    }

    public ExecutionTreeNode nextUnexplored() {
        ExecutionTreeNode result;
        if (distanceToError != null) {
            result = nextUnexploredError();
        } else {
            result = nextUnexploredBFS();
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

}
