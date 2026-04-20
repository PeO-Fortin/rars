package rars.concolic;

import rars.ProgramStatement;
import rars.RISCVprogram;
import rars.assembler.Assembler;
import rars.cfg.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GenericInterpreter<V> {
    InterpreterValues<V> values;

    public GenericInterpreter(InterpreterValues<V> values) {
        this.values = values;
    }

    public void runOn(String filename) throws Exception {
        prepare(filename);
        runMain();
    }

    CFG cfg;
    public void prepare(String filename) throws Exception {
        RISCVprogram program = new RISCVprogram();
        ArrayList<String> filenames = new ArrayList<>();
        filenames.add(filename);

        ArrayList<RISCVprogram> programs = program.prepareFilesForAssembly(filenames, filename, null);
        Assembler assembler = new Assembler();
        assembler.assemble(programs, true, false, program);

        cfg = new CFG(program);
        CFGBuilder builder = new CFGBuilder(cfg);
        builder.build();
    }

    V[] registers = (V[]) new Object[32];
    protected BasicBlock currentBlock;
    void run() {
        registers[0] = values.inject(0);
        while (currentBlock != null) {
            for (ProgramStatement ps : currentBlock.instructions) {
                String name = ps.getInstruction().getName();
                int[] operands = ps.getOperands();

                switch (name) {
                    case "add": add(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "addi": add(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sub": sub(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "mul": mul(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "div": div(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "jal": jmp(currentBlock.takenSuccessor); break;
                    case "bge": ifgeq(registers[operands[0]], registers[operands[1]],
                            currentBlock.takenSuccessor, currentBlock.fallthroughSuccessor); break;
                    case "blt": iflt(registers[operands[0]], registers[operands[1]],
                            currentBlock.takenSuccessor, currentBlock.fallthroughSuccessor); break;
                    case "beq": ifeq(registers[operands[0]], registers[operands[1]],
                            currentBlock.takenSuccessor, currentBlock.fallthroughSuccessor); break;
                    case "bne": ifneq(registers[operands[0]], registers[operands[1]],
                            currentBlock.takenSuccessor, currentBlock.fallthroughSuccessor); break;
                    case "ecall": ecall(values.asInt(registers[17])); break; // Register a7
                    case "xor": xor(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "xori": xor(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "and": and(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "andi": and(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "or": or(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "ori": or(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sll": sll(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "slli": sll(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "srl": srl(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "srli": srl(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sra": sra(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "srai": sra(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                }
            }
        }
    }

    public void runMain() {
        currentBlock = cfg.entryBlock;
        run();
    }

    protected void setCurrentBlock(BasicBlock target) {
        currentBlock = target;
    }

    protected void setCurrentBlockCond(BasicBlock target) {
        setCurrentBlock(target);
    }

    void add(V left, V right, int dst) {registers[dst] = values.add(left, right);}

    void sub(V left, V right, int dst) {registers[dst] = values.sub(left, right);}

    void mul(V left, V right, int dst) {registers[dst] = values.mul(left, right);}

    void div(V left, V right, int dst) {registers[dst] = values.mul(left, right);}

    void jmp(BasicBlock target) {
        setCurrentBlock(target);
    }

    void ifgeq(V left, V right, BasicBlock then, BasicBlock else_) {
        if_(values.geq(left, right), then, else_);
    }

    void iflt(V left, V right, BasicBlock then, BasicBlock else_) {
        if_(values.lt(left, right), then, else_);
    }

    void ifeq(V left, V right, BasicBlock then, BasicBlock else_) { if_(values.eq(left, right), then, else_); }

    void ifneq(V left, V right, BasicBlock then, BasicBlock else_) { if_(values.neq(left, right), then, else_); }


    void xor(V left, V right, int dst) { registers[dst] = values.xor(left, right); }

    void and(V left, V right, int dst) { registers[dst] = values.and(left, right); }

    void or(V left, V right, int dst) { registers[dst] = values.or(left, right); }

    void sll(V left, V right, int dst) { registers[dst] = values.sll(left, right); }

    void srl(V left, V right, int dst) { registers[dst] = values.srl(left, right); }

    void sra(V left, V right, int dst) { registers[dst] = values.sra(left, right); }

    protected void if_(V cond, BasicBlock then, BasicBlock else_) {
        if (values.isTruthy(cond)) {
            setCurrentBlockCond(then);
        } else {
            setCurrentBlockCond(else_);
        }
    }

    protected abstract V readChar();
    protected abstract V readInt();
    public void ecall(int syscall) {
        switch (syscall) {
            // Register 10 = a0
            case 5:  // ReadInt
                registers[10] = readInt();
                return;
            case 12: // ReadChar
                registers[10] = readChar();
                return;
            case 1:  // PrintInt
                System.out.print(values.asInt(registers[10]));
                return;
            case 11: // PrintChar
                System.out.print(values.asChar(registers[10]));
                return;
            case 34: // PrintIntHex
                System.out.printf("%x", values.asInt(registers[10]));
                return;
            case 35: // PrintIntBinary
                System.out.print(Integer.toBinaryString(values.asInt(registers[10])));
        }
    }
}
