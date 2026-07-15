package rars.concolic;

import rars.cfg.BasicBlock;

import java.util.*;

public class ExecutionTree {
    private Map<NodeKey, ExecutionTreeNode> nodes;
    ExecutionTreeNode root;
    ExecutionTreeNode previousNode;
    boolean rootDefined = false;
    private static int id = 0;


    public ExecutionTree() {
        nodes = new HashMap<>();
        root = new ExecutionTreeNode(0);
        previousNode = null;
    }

    public boolean contains(NodeKey key){
        return nodes.containsKey(key);
    }

    public ExecutionTreeNode getOrCreate(NodeKey key) {
        return nodes.computeIfAbsent(key, k -> {
            if (!rootDefined) {
                rootDefined = true;
                return root;
            }
            return new ExecutionTreeNode(++id);
        });
    }

    public ExecutionTreeNode addNode(NodeKey key, ExecutionTreeNode node) {
        return nodes.put(key, node);
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
    public int id;
    BasicBlock block;

    public SymbolicValue condition;
    public ArrayList<SymbolicValue> constraints = new ArrayList<>();

    public ExecutionTreeNode trueBranch;
    public ExecutionTreeNode falseBranch;
    public boolean trueBranchUnsat = false;
    public boolean falseBranchUnsat = false;

    public Integer distanceToExit;


    public ExecutionTreeNode(int id) {
        this.id = id;
    }

    public boolean isUnexplored(boolean direction) {
        if(direction == true) {
            return trueBranch == null && !trueBranchUnsat;
        } else {
            return falseBranch == null && !falseBranchUnsat;
        }
    }

    public boolean hasUnexploredEdge() {
        return condition != null && (isUnexplored(true) || isUnexplored(false));
    }

/*    public UnexploredEdge nextUnexploredDFS() {
        if (trueBranch != null && falseBranch != null) {
            ExecutionTreeNode f = falseBranch.nextUnexplored();
            if (f != null) return f;
            ExecutionTreeNode t = trueBranch.nextUnexplored();
            if (t != null) return t;
        }
        return null;
    }*/

    public UnexploredEdge nextUnexploredBFS() {
        Queue<ExecutionTreeNode> worklist = new LinkedList<>();
        Set<ExecutionTreeNode> visited = new HashSet<>();
        worklist.add(this);
        while (!worklist.isEmpty()) {
            ExecutionTreeNode node = worklist.remove();
            if (node.hasUnexploredEdge()) {
                if (node.isUnexplored(false)) return new UnexploredEdge(node, false);
                if (node.isUnexplored(true)) return new UnexploredEdge(node, true);
            }
            if (node.falseBranch != null && !visited.contains(node.falseBranch)) {
                visited.add(node.falseBranch);
                worklist.add(node.falseBranch);
            }
            if (node.trueBranch != null && !visited.contains(node.trueBranch)) {
                visited.add(node.trueBranch);
                worklist.add(node.trueBranch);
            }
        }
        return null;
    }

    /*public UnexploredEdge nextUnexploredExit() {
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

    public UnexploredEdge nextUnexploredRandom() {
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

    public UnexploredEdge nextUnexploredCoverage() {
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
    }*/

    public UnexploredEdge nextUnexplored() {
        UnexploredEdge result;
        Heuristics h = ConcolicInterpreter.options.getHeuristic();
        switch (h) {
            case BFS:
            default:
                result = nextUnexploredBFS();
                break;
            /*case DFS:
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
                break;*/
        }
        return result;
    }

    public int size() {
        int size = 1;
        if (trueBranch != null) size += trueBranch.size();
        if (falseBranch != null) size += falseBranch.size();
        return size;
    }
}

class UnexploredEdge {
    ExecutionTreeNode node;
    boolean direction;

    public UnexploredEdge(ExecutionTreeNode node, boolean direction) {
        this.node = node;
        this.direction = direction;
    }
}
