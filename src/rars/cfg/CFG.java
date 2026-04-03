package rars.cfg;

import rars.RISCVprogram;
import rars.ProgramStatement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/** The control flow graph of a program.*/

public class CFG {
    public RISCVprogram program;
    public BasicBlock entryBlock;
    public BasicBlock exitBlock;
    public List<BasicBlock> blocks = new ArrayList<>();

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
     * This also removes unreachable blocks. */
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

    public void dump(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(program.getFilename() + ":\n");
            for (BasicBlock block : blocks) {
                fw.write(block + ":\n");
                for (ProgramStatement instruction : block.instructions) {
                    fw.write("\t" + instruction + "\n");
                }
            }
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}