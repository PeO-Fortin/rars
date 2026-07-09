package rars.concolic;

import rars.cfg.BasicBlock;

import java.util.*;

public class ExecutionTree {
    private Map<NodeKey, ExecutionTreeNode> nodes;
    private static int id = 0;


    public ExecutionTree() {
        nodes = new HashMap<>();
    }

    ExecutionTreeNode getOrCreate(NodeKey key) {
        return nodes.computeIfAbsent(key, k->new ExecutionTreeNode(++id));
    }
}

class NodeKey {
    public BasicBlock block;
    public Deque<BasicBlock> context;

    public NodeKey(BasicBlock block, Deque<BasicBlock> context) {
        this.block = block;
        this.context = new ArrayDeque<>(List.copyOf(context)); //Immutable Deque
    }

    @Override
    public boolean equals(Object obj) {
        return block.equals(((NodeKey) obj).block) && context.equals(((NodeKey) obj).context);
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
