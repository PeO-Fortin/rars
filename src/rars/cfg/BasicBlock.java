package rars.cfg;

import rars.ProgramStatement;

import java.util.ArrayList;
import java.util.List;

/** A basic block of a CFG.
 * Can only enter at the first instruction, and exit at the last one (called terminator).
 */

class BasicBlock {
    public int id;
    public CFG cfg;
    public List<ProgramStatement> instructions = new ArrayList<>();

    public BasicBlock takenSuccessor;
    public BasicBlock fallthroughSuccessor;


    public BasicBlock(CFG cfg, int id) {
        this.cfg = cfg;
        this.id = id;
    }

    public List<BasicBlock> getOut() {
        if (instructions.isEmpty()) return new ArrayList<>();
        ProgramStatement terminator = getTerminator();
        List<BasicBlock> result = new ArrayList<>();
        if (takenSuccessor != null) result.add(takenSuccessor);
        if (fallthroughSuccessor != null) result.add(fallthroughSuccessor);
        return result;
    }

    public List<BasicBlock> getIn() {
        List<BasicBlock> result = new ArrayList<>();
        for (BasicBlock block : cfg.blocks)
            if (block.getOut().contains(this))
                result.add(block);
        return result;
    }

    public ProgramStatement getTerminator() {
        return instructions.get(instructions.size() - 1);
    }

    public BasicBlock nextLinearBlock() {
        int i = cfg.blocks.indexOf(this) + 1;
        if (i < cfg.blocks.size())
            return cfg.blocks.get(i);
        else
            return null;
    }

    public void add(ProgramStatement instruction) {
        instructions.add(instruction);
    }

    public int getStartAddress() {
        return instructions.get(0).getAddress();
    }

    @Override
    public String toString() {
        return "L" + id;
    }
}