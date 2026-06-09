package rars.cfg;

import rars.RISCVprogram;
import rars.ProgramStatement;

import java.util.*;

/** The control flow graph of a program.*/

public class CFG {
    public RISCVprogram program;
    public BasicBlock entryBlock;
    public BasicBlock exitBlock;
    public List<BasicBlock> blocks = new ArrayList<>();
    public Map<Integer, BasicBlock> blockMap;

    private int blockCount = 0;

    public CFG(RISCVprogram p) {
        program = p;
    }

    public BasicBlock newBlock() {
        return new BasicBlock(this, blockCount++);
    }

    public BasicBlock blockOf(ProgramStatement instruction) {
        for (BasicBlock block : blocks)
            for (ProgramStatement ir: block.instructions)
                if (ir == instruction)
                    return block;
        return null;
    }

    /** Sort blocks pseudo-topologically (topological but ignore back edges).
     * This also removes unreachable blocks.
     */
    public void reorder() {
        blocks.clear();
        HashSet<BasicBlock> seen = new HashSet<>();
        seen.add(exitBlock);
        reorder(entryBlock, seen);
        blocks.add(exitBlock);
    }

    private void reorder(BasicBlock block, Collection<BasicBlock> seen) {
        if (seen.contains(block)) return;
        seen.add(block);
        List<BasicBlock> outs = new ArrayList<>(block.getOut());
        Collections.reverse(outs);
        for (BasicBlock next : outs)
            reorder(next, seen);
        blocks.add(0, block);
    }

    public void build() {
        List<ProgramStatement> machineList = program.getMachineList();
        if (machineList.isEmpty()) return;

        Set<Integer> blockEntryPoints = findEntryPoints(machineList);
        createBlocks(machineList, blockEntryPoints);
        findSuccessors();

        entryBlock = blocks.get(0);
        exitBlock = blocks.get(blocks.size() - 1);
    }

    private void add(ProgramStatement statement, BasicBlock currentBlock) {
        if (currentBlock == null) return;
        currentBlock.add(statement);
    }

    public void cleanUpCFG() {
        reorder();
        for (int i=0; i<blocks.size(); i++)
            blocks.get(i).id = i;
    }

    private Set<Integer> findEntryPoints(List<ProgramStatement> machineList) {
        Set<Integer> blockEntryPoints =  new HashSet<>();
        blockEntryPoints.add(machineList.get(0).getAddress());

        for (ProgramStatement ps : machineList) {
            String instruction = ps.getInstruction().getName();
            int[] operands = ps.getOperands();

            if (isBranch(instruction)) {
                blockEntryPoints.add(ps.getAddress() + operands[2]);    //block if true
                blockEntryPoints.add(ps.getAddress() + 4);              //block if false
            } else if (instruction.equals("jal")) {
                blockEntryPoints.add(ps.getAddress() + operands[1]);
            }
        }

        return blockEntryPoints;
    }

    private void createBlocks(List<ProgramStatement> machineList, Set<Integer> blockEntryPoints) {
        blockMap = new HashMap<>();

        BasicBlock currentBlock = null;

        for (ProgramStatement ps : machineList) {
            if (blockEntryPoints.contains(ps.getAddress())) {
                currentBlock = newBlock();
                blocks.add(currentBlock);
                blockMap.put(ps.getAddress(), currentBlock);
            }
            currentBlock.add(ps);
        }
    }

    private void findSuccessors() {
        for (BasicBlock block : blocks) {
            ProgramStatement terminator = block.getTerminator();
            String instruction = terminator.getInstruction().getName();
            int[] operands = terminator.getOperands();
            int pc = terminator.getAddress();

            if (isBranch(instruction)) {
                block.takenSuccessor = blockMap.get(pc + operands[2]);
                block.fallthroughSuccessor = blockMap.get(pc + 4);
            } else if (instruction.equals("jal")) {
                block.takenSuccessor = blockMap.get(pc + operands[1]);
            } else if (instruction.equals("jalr")) {
                block.takenSuccessor = null;
            } else {
                block.fallthroughSuccessor = blockMap.get(pc + 4);
            }
        }
    }

    private boolean isBranch(String instruction) {
        return instruction.equals("beq") || instruction.equals("bne") ||
                instruction.equals("blt") || instruction.equals("bltu") ||
                instruction.equals("bge") || instruction.equals("bgeu");
    }
}