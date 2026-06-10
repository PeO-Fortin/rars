package rars.concolic;

import rars.AssemblyException;
import rars.ProgramStatement;
import rars.RISCVprogram;
import rars.assembler.Assembler;
import rars.riscv.hardware.AddressErrorException;
import rars.cfg.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public abstract class GenericInterpreter<V> {
    InterpreterValues<V> values;

    public GenericInterpreter(InterpreterValues<V> values) {
        this.values = values;
    }

    CFG cfg;
    public void runOn(String filename) throws Exception {
        prepare(filename);
        runMain();
    }

    Memory memory;
    public void prepare(String filename) throws Exception {
        try {
            RISCVprogram program = new RISCVprogram();
            ArrayList<String> filenames = new ArrayList<>();
            filenames.add(filename);
            filenames.add("test/rars/concolic/libs.s");

            ArrayList<RISCVprogram> programs = program.prepareFilesForAssembly(filenames, filename, null);
            Assembler assembler = new Assembler();
            assembler.assemble(programs, true, false, program);

            cfg = new CFG(program);
            cfg.build();
            memory = new Memory();

        } catch (AssemblyException e) {
            System.err.println(e.errors().generateErrorReport());
            throw e;
        }
    }

    public String output = "";
    public String input = "";

    int instructionCounter;
    final int MAX_INSTRUCTIONS = 500;

    final int DEFAULT_OFFSET = 4;

    V[] registers;
    BasicBlock currentBlock;

    boolean exit = false;

    void run(BasicBlock entryPoint) {
        currentBlock = entryPoint;
        instructionCounter = 0;
        while (currentBlock!=null && !exit && instructionCounter < MAX_INSTRUCTIONS) {
            BasicBlock executedBlock = currentBlock;
            for (ProgramStatement ps : currentBlock.instructions) {
                int[] operands = ps.getOperands();
                switch (ps.getInstruction().getName()) {
                    case "lui":
                        lui(values.inject(operands[1] << 12), operands[0]); break;
                    case "add":
                        add(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "addw":
                        addw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "addi":
                        add(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "addiw":
                        addw(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sub":
                        sub(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "subw":
                        subw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "mul":
                        mul(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "mulw":
                        mulw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "div":
                        div(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "divw":
                        divw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "auipc":
                        add(values.inject(ps.getAddress()), values.inject(operands[1] << 12), operands[0]); break;
                    case "ecall":
                        ecall(values.asInt(registers[17])); break; // Register a7
                    case "xor":
                        xor(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "xori":
                        xor(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "and":
                        and(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "andi":
                        and(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "or":
                        or(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "ori":
                        or(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sb":
                        sb(registers[operands[0]], values.inject(operands[1]), registers[operands[2]]); break;
                    case "sh":
                        sh(registers[operands[0]], values.inject(operands[1]), registers[operands[2]]); break;
                    case "sw":
                        sw(registers[operands[0]], values.inject(operands[1]), registers[operands[2]]); break;
                    case "sd":
                        sd(registers[operands[0]], values.inject(operands[1]), registers[operands[2]]); break;
                    case "lb":
                        lb(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "lbu":
                        lbu(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "lh":
                        lh(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "lhu":
                        lhu(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "lw":
                        lw(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "lwu":
                        lwu(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "ld":
                        ld(values.inject(operands[1]), registers[operands[2]], operands[0]); break;
                    case "sll":
                        sll(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "slli":
                        sll(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sllw":
                        sllw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "slliw":
                        sllw(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "srl":
                        srl(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "srli":
                        srl(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "srlw":
                        srlw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "srliw":
                        srlw(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sra":
                        sra(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "srai":
                        sra(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "sraw":
                        sraw(registers[operands[1]], registers[operands[2]], operands[0]); break;
                    case "sraiw":
                        sraw(registers[operands[1]], values.inject(operands[2]), operands[0]); break;
                    case "jal":
                        jal(ps.getAddress(), operands[0]); break;
                    case "jalr":
                        jalr(registers[operands[1]], ps.getAddress(), operands[2], operands[0]); break;
                    case "bge":
                        ifgeq(registers[operands[0]], registers[operands[1]]); break;
                    case "blt":
                        iflt(registers[operands[0]], registers[operands[1]]); break;
                    case "beq":
                        ifeq(registers[operands[0]], registers[operands[1]]); break;
                    case "bne":
                        ifneq(registers[operands[0]], registers[operands[1]]); break;
                }
                registers[0] = values.inject(0); // Register zero = 0
                ++instructionCounter;
            }
            if(executedBlock == currentBlock){
                currentBlock = executedBlock.fallthroughSuccessor;
            }
        }
        if(instructionCounter == MAX_INSTRUCTIONS){
            output += "Maximum number of instructions reached |";
        }
    }

    protected void setCurrentBlock(BasicBlock target) {
        currentBlock = target;
    }

    protected void setCurrentBlockCond(BasicBlock target) {
        setCurrentBlock(target);
    }

    public void runMain() {
        registers = (V[]) new Object[32];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = values.inject(0);
        }
        registers[2] = values.inject(Memory.DEFAULT_STACK_POINTER); //sp
        registers[3] = values.inject(Memory.DEFAULT_GLOBAL_POINTER); //gp
        exit = false;
        run(cfg.entryBlock);
    }

    void lui(V value, int dst) { registers [dst] = value; }

    void add(V left, V right, int dst) {registers[dst] = values.add(left, right); }

    void addw(V left, V right, int dst) {registers[dst] = values.addw(left, right); }

    void sub(V left, V right, int dst) {registers[dst] = values.sub(left, right); }

    void subw(V left, V right, int dst) {registers[dst] = values.subw(left, right); }

    void mul(V left, V right, int dst) {registers[dst] = values.mul(left, right); }

    void mulw(V left, V right, int dst)  {registers[dst] = values.mulw(left, right); }

    void div(V left, V right, int dst) {registers[dst] = values.div(left, right); }

    void divw(V left, V right, int dst) {registers[dst] = values.divw(left, right); }

    void ifgeq(V left, V right) {if_(values.geq(left, right)); }

    void iflt(V left, V right) {if_(values.lt(left, right)); }

    void ifeq(V left, V right) { if_(values.eq(left, right)); }

    void ifneq(V left, V right) { if_(values.neq(left, right)); }

    void xor(V left, V right, int dst) { registers[dst] = values.xor(left, right); }

    void and(V left, V right, int dst) { registers[dst] = values.and(left, right); }

    void or(V left, V right, int dst) { registers[dst] = values.or(left, right); }

    protected abstract void sb(V value, V offset, V memAddress);
    protected abstract void sh(V value, V offset, V memAddress);
    protected abstract void sw(V value, V offset, V memAddress);
    protected abstract void sd(V value, V offset, V memAddress);
    protected abstract void lb(V offset, V memAddress, int dst);
    protected abstract void lbu(V offset, V memAddress, int dst);
    protected abstract void lh(V offset, V memAddress, int dst);
    protected abstract void lhu(V offset, V memAddress, int dst);
    protected abstract void lw(V offset, V memAddress, int dst);
    protected abstract void lwu(V offset, V memAddress, int dst);
    protected abstract void ld(V offset, V memAddress, int dst);

    void sll(V left, V right, int dst) { registers[dst] = values.sll(left, right); }

    void sllw(V left, V right, int dst) { registers[dst] = values.sllw(left, right); }

    void srl(V left, V right, int dst) { registers[dst] = values.srl(left, right); }

    void srlw(V left, V right, int dst) { registers[dst] = values.srlw(left, right); }

    void sra(V left, V right, int dst) { registers[dst] = values.sra(left, right); }

    void sraw(V left, V right, int dst) { registers[dst] = values.sraw(left, right); }

    void jal(int currentProgramCounter, int dst) {
        registers[dst] = values.inject(currentProgramCounter + DEFAULT_OFFSET);
        currentBlock = currentBlock.takenSuccessor;
    }

    void jalr(V jd, int currentProgramCounter, int offset, int dst) {
        registers[dst] = values.inject(currentProgramCounter + DEFAULT_OFFSET);
        currentBlock.takenSuccessor = cfg.blockMap.get(values.asInt(jd) + offset);
        currentBlock = currentBlock.takenSuccessor;
    }

    protected void if_(V cond) {
        if (values.isTruthy(cond)) {
            setCurrentBlockCond(currentBlock.takenSuccessor);
        } else {
            setCurrentBlockCond(currentBlock.fallthroughSuccessor);
        }
    }

    protected abstract V readChar();
    protected abstract V readInt();
    protected abstract void readString(V bufAddress, V length);
    public void ecall(int syscall) {
        switch (syscall) {
            // Register 10 = a0
            case 1:     // PrintInt
                output += values.asInt(registers[10]) + " | ";
                return;
            case 4:     // PrintString
                output += printString(values.asLong(registers[10])) + " | ";
                return;
            case 5:     // ReadInt
                registers[10] = readInt();
                input += values.asInt(registers[10]) + " | ";
                return;
            case 8:     // ReadString
                readString(registers[10], registers[11]);
                return;
            case 9:     //Sbrk
                memory.sBrk(values.asInt(registers[10]));
                return;
            case 10:    // Exit
                exit = true;
                return;
            case 11:    // PrintChar
                output += values.asChar(registers[10]) + " | ";
                return;
            case 12:    // ReadChar
                registers[10] = readChar();
                input += values.asChar(registers[10]) + " | ";
                return;
            case 34:    // PrintIntHex
                output += Integer.toHexString(values.asInt(registers[10])) + " | ";
                return;
            case 35:    // PrintIntBinary
                output += Integer.toBinaryString(values.asInt(registers[10])) + " | ";
                return;
        }
    }

    private String printString(long bufAddress) {
        String s = "";
        ByteArrayOutputStream encodedChars = new ByteArrayOutputStream();
        MemoryValue memoryValue = new MemoryValue(MemoryValueTypes.BYTE, true);
        ConcolicValues op = new ConcolicValues();
        byte encodedChar;
        int offset = 0;

        try {
            do {
                memory.accessMemory(memoryValue, op.inject(bufAddress), op.inject(offset));
                encodedChar = memoryValue.getConcreteValue().byteValue();
                encodedChars.write(encodedChar);
                ++offset;
            } while (encodedChar != 0);

            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);

            s = decoder.decode(ByteBuffer.wrap(encodedChars.toByteArray())).toString();

        } catch (AddressErrorException e){
            output += e.getMessage() + " | ";
            exit = true;
        } catch (CharacterCodingException e) {
            output += "Erreur d'encodage de caractère" + " | ";
            exit = true;
        }
        return s;
    }
}
