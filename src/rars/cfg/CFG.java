package rars.cfg;

import minic.lib.CFGraphviz;
import minic.lib.CFGraphvizDag;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// The control flow graph of a program.

public class CFG {
    public BasicBlock entryBlock;
    public BasicBlock exitBlock;
    public List<BasicBlock> blocks = new ArrayList<>();
    public List<Register> registers = new ArrayList<>();
    public List<Register> parameters = new ArrayList<>();
    public HashMap<String, Integer> registerNameCount = new HashMap<>();

    public CFG(Function f) {
        function = f;
    }

    private int blockCount = 0;

    public BasicBlock newBlock() {
        return new BasicBlock(this, blockCount++);
    }

    public Register newRegister(String name) {
        while (!name.isEmpty() && Character.isDigit((name.charAt(name.length() - 1)))) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            name = "tmp";
        }
        Integer count = registerNameCount.get(name);
        if (count == null) {
            if (name.equals("tmp")) {
                registerNameCount.put(name, 0);
                name = "tmp0";
            } else {
                registerNameCount.put(name, 1);
            }
        } else {
            count++;
            registerNameCount.put(name, count);
            name = name + count;
        }
        Register result = new Register(registers.size(), name);
        registers.add(result);
        return result;
    }

    /** All the uses of a register. Not flow sensitive. */
    public List<IR> getUses(Register reg) {
        List<IR> uses = new ArrayList<>();
        for (BasicBlock block : blocks)
            for (IR ir : block.instructions)
                for (Operand op : ir.src)
                    if (op == reg)
                        uses.add(ir);
        return uses;
    }

    /** All the definitions of a register. Not flow sensitive. */
    public List<IR> getDefs(Register reg) {
        List<IR> defs = new ArrayList<>();
        for (BasicBlock block : blocks)
            for (IR ir : block.instructions)
                if (ir.dst == reg)
                    defs.add(ir);
        return defs;
    }

    /** Get the single definitions of a register, or null (if none or many).
     * Not flow sensitive. */
    public IR getSingleDef(Register reg) {
        List<IR> defs = getDefs(reg);
        if (defs.size() == 1) return defs.get(0);
        return null;
    }

    public BasicBlock blockOf(IR instruction) {
        for (BasicBlock block : blocks)
            for (IR ir: block.instructions)
                if (ir == instruction)
                    return block;
        return null;
    }

    void addBefore(IR oldIR, IR newIR) {
        BasicBlock block = blockOf(oldIR);
        int index = block.instructions.indexOf(oldIR);
        block.instructions.add(index, newIR);
    }

    public void addAfter(IR oldIR, IR newIR) {
        BasicBlock block = blockOf(oldIR);
        int index = block.instructions.indexOf(oldIR);
        block.instructions.add(index+1, newIR);
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
            fw.write(function.name + ":\n");
            for (BasicBlock block : blocks) {
                fw.write(block + ":\n");
                for (IR instruction : block.instructions) {
                    fw.write("\t" + instruction + "\n");
                }
            }
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void dumpDot(String filename) {
        CFGraphviz viz = new CFGraphviz(this);
        viz.dumpDot(filename);
    }

    public void dumpDag(String filename) {
        CFGraphviz viz = new CFGraphvizDag(this);
        viz.dumpDot(filename);
    }
}