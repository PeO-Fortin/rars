package rars.concolic;

import rars.ProgramStatement;
import rars.RISCVprogram;
import rars.assembler.Assembler;

import java.util.ArrayList;
import java.util.HashMap;
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

    Map<Integer, ProgramStatement> instructionsMap = new HashMap<>();
    ArrayList<ProgramStatement> machineList;
    static Memory memory;
    public void prepare(String filename) throws Exception {
        RISCVprogram program = new RISCVprogram();
        ArrayList<String> filenames = new ArrayList<>();
        filenames.add(filename);

        ArrayList<RISCVprogram> programs = program.prepareFilesForAssembly(filenames, filename, null);
        Assembler assembler = new Assembler();
        assembler.assemble(programs, true, false, program);

        machineList = program.getMachineList();
        memory = new Memory();

        for (ProgramStatement ps : machineList) {
            instructionsMap.put(ps.getAddress(), ps);
        }
    }

    final int DEFAULT_OFFSET = 4;
    int currentProgramCounter;
    V[] registers;
    boolean exit = false;
    void run(int programCounter) {
        currentProgramCounter = programCounter;
        ProgramStatement ps = instructionsMap.get(programCounter);
        while (ps != null && !exit) {
            String instructionName = ps.getInstruction().getName();
            int[] operands = ps.getOperands();

            switch (instructionName) {
                case "lui": lui(values.inject(operands[1] << 12), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "add": add(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "addi": add(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sub": sub(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "mul": mul(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "div": div(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "auipc": add(values.inject(currentProgramCounter), values.inject(operands[1] << 12), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "ecall": ecall(values.asInt(registers[17])); setCurrentPc(DEFAULT_OFFSET); break; // Register a7
                case "xor": xor(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "xori": xor(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "and": and(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "andi": and(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "or": or(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "ori": or(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sb": sb(registers[operands[0]], registers[operands[1]], registers[operands[2]]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sh": sh(registers[operands[0]], registers[operands[1]], registers[operands[2]]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sw": sw(registers[operands[0]], registers[operands[1]], registers[operands[2]]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sd": sd(registers[operands[0]], registers[operands[1]], registers[operands[2]]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lb": lb(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lbu": lbu(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lh": lh(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lhu": lhu(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lw": lw(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "lwu": lwu(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "ld": ld(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sll": sll(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "slli": sll(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "srl": srl(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "srli": srl(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "sra": sra(registers[operands[1]], registers[operands[2]], operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "srai": sra(registers[operands[1]], values.inject(operands[2]), operands[0]); setCurrentPc(DEFAULT_OFFSET); break;
                case "jal": jal(operands[1],operands[0]); break;
                case "jalr": jalr(registers[operands[1]], operands[2], operands[0]); break;
                case "bge": ifgeq(registers[operands[0]], registers[operands[1]], operands[2]); break;
                case "blt": iflt(registers[operands[0]], registers[operands[1]], operands[2]); break;
                case "beq": ifeq(registers[operands[0]], registers[operands[1]], operands[2]); break;
                case "bne": ifneq(registers[operands[0]], registers[operands[1]], operands[2]); break;
            }
            ps = instructionsMap.get(currentProgramCounter);
        }
    }

    public void runMain() {
        registers = (V[]) new Object[32];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = values.inject(0);
        }
        registers[2] = values.inject(Memory.DEFAULT_STACK_POINTER); //sp
        registers[3] = values.inject(Memory.DEFAULT_GLOBAL_POINTER); //gp
        exit = false;
        run(machineList.get(0).getAddress());
    }

    protected void setCurrentPc(int offset) {
         currentProgramCounter += offset;
    }

    protected void setCurrentPcCond(int offset) {
        setCurrentPc(offset);
    }

    void lui(V value, int dst) { registers [dst] = value; }

    void add(V left, V right, int dst) {registers[dst] = values.add(left, right); }

    void sub(V left, V right, int dst) {registers[dst] = values.sub(left, right); }

    void mul(V left, V right, int dst) {registers[dst] = values.mul(left, right); }

    void div(V left, V right, int dst) {registers[dst] = values.div(left, right); }

    void ifgeq(V left, V right, int condOffset) {if_(values.geq(left, right), condOffset); }

    void iflt(V left, V right, int condOffset) {if_(values.lt(left, right), condOffset); }

    void ifeq(V left, V right, int condOffset) { if_(values.eq(left, right), condOffset); }

    void ifneq(V left, V right, int condOffset) { if_(values.neq(left, right), condOffset); }

    void xor(V left, V right, int dst) { registers[dst] = values.xor(left, right); }

    void and(V left, V right, int dst) { registers[dst] = values.and(left, right); }

    void or(V left, V right, int dst) { registers[dst] = values.or(left, right); }

    void sb(V value, V offset, V MemAddress) {
        try {
            memory.storeMemory(values.asLong(value), values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.BYTE);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void sh(V value, V offset, V MemAddress) {
        try {
            memory.storeMemory(values.asLong(value), values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.HALFWORD);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void sw(V value, V offset, V MemAddress) {
        try {
            memory.storeMemory(values.asLong(value), values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.WORD);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void sd(V value, V offset, V MemAddress) {
        try {
            memory.storeMemory(values.asLong(value), values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.DOUBLEWORD);
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lb(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.BYTE, false));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lbu(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.BYTE, true));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lh(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.HALFWORD, false));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lhu(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.HALFWORD, true));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lw(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.WORD, false));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void lwu(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.WORD, true));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void ld(V offset, V MemAddress, int dst) {
        try {
            registers[dst] = values.inject(memory.accessMemory(values.asLong(MemAddress), values.asByte(offset), MemoryValueSizes.DOUBLEWORD, false));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        }
    }

    void sll(V left, V right, int dst) { registers[dst] = values.sll(left, right); }

    void srl(V left, V right, int dst) { registers[dst] = values.srl(left, right); }

    void sra(V left, V right, int dst) { registers[dst] = values.sra(left, right); }

    void jal(int offset, int dst) {
        registers[dst] = values.inject(currentProgramCounter + DEFAULT_OFFSET);
        setCurrentPc(offset);
    }

    void jalr(V jd, int offset, int dst) {
        registers[dst] = values.inject(currentProgramCounter + DEFAULT_OFFSET);
        setCurrentPc(values.asInt(jd) + offset);
    }

    protected void if_(V cond, int condOffset) {
        if (values.isTruthy(cond)) {
            setCurrentPcCond(condOffset);
        } else {
            setCurrentPcCond(DEFAULT_OFFSET);
        }
    }

    public String output = "";
    public String input = "";
    protected abstract V readChar();
    protected abstract V readInt();
    public void ecall(int syscall) {
        switch (syscall) {
            // Register 10 = a0
            case 1:  // PrintInt
                output += values.asInt(registers[10]) + " | ";
                return;
            case 5:  // ReadInt
                registers[10] = readInt();
                input += values.asInt(registers[10]) + " | ";
                return;
            case 9: //Sbrk
                memory.sBrk(values.asInt(registers[10]));
                return;
            case 10: // Exit
                exit = true;
                return;
            case 11: // PrintChar
                output += values.asChar(registers[10]) + " | ";
                return;
            case 12: // ReadChar
                registers[10] = readChar();
                input += values.asChar(registers[10]) + " | ";
                return;
            case 34: // PrintIntHex
                output += Integer.toHexString(values.asInt(registers[10])) + " | ";
                return;
            case 35: // PrintIntBinary
                output += Integer.toBinaryString(values.asInt(registers[10])) + " | ";
                return;
        }
    }
}
