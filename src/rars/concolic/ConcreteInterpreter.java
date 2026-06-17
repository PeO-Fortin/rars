package rars.concolic;

import rars.Globals;
import rars.riscv.InstructionSet;
import rars.riscv.hardware.AddressErrorException;

import java.io.IOException;
import java.util.Scanner;

class ConcreteValues extends InterpreterValues<Long> {
    @Override public Long inject(long i) { return i; }
    @Override public Long access(MemoryValue v) { return v.getConcreteValue(); }

    @Override public Long add(Long v1, Long v2) { return v1 + v2; }
    @Override public Long addw(Long v1, Long v2) { return (long)((v1.intValue() + v2.intValue())); }
    @Override public Long sub(Long v1, Long v2) { return v1 - v2; }
    @Override public Long subw(Long v1, Long v2) { return (long)(v1.intValue() - v2.intValue()); }
    @Override public Long mul(Long v1, Long v2) { return v1 * v2; }
    @Override public Long mulw(Long v1, Long v2) { return (long)(v1.intValue() * v2.intValue()); }
    @Override public Long mulh(Long v1, Long v2) { return (v1 >> 32) * (v2 >> 32); }
    @Override public Long mulhsu(Long v1, Long v2) { return (v1 >> 32) * (v2 >>> 32); }
    @Override public Long mulhu(Long v1, Long v2) { return (v1 >>> 32) * (v2 >>> 32); }
    @Override public Long div(Long v1, Long v2) { return v1 / v2; }
    @Override public Long divw(Long v1, Long v2) { return (long)(v1.intValue() / v2.intValue()); }
    @Override public Long divu(Long v1, Long v2) { return Long.divideUnsigned(v1, v2); }
    @Override public Long divuw(Long v1, Long v2) { return Long.divideUnsigned(v1.intValue(), v2.intValue()); }

    @Override public Long eq(Long v1, Long v2) { return v1.equals(v2) ? 1L : 0L; }
    @Override public Long neq(Long v1, Long v2) { return !(v1.equals(v2)) ? 1L : 0L; }
    @Override public Long geq(Long v1, Long v2) { return v1 >= v2 ? 1L : 0L; }
    @Override public Long lt(Long v1, Long v2) { return v1 < v2 ? 1L : 0L; }

    @Override public Long xor(Long v1, Long v2) { return v1 ^ v2; }
    @Override public Long and(Long v1, Long v2) { return v1 & v2; }
    @Override public Long or(Long v1, Long v2) { return v1 | v2; }

    @Override public Long sll(Long v1, Long v2) { return v1 << v2; }
    @Override public Long sllw(Long v1, Long v2) { return (long) (v1.intValue() << v2.intValue()); }
    @Override public Long srl(Long v1, Long v2) { return v1 >>> v2; }
    @Override public Long srlw(Long v1, Long v2) { return (long) (v1.intValue() >>> v2.intValue()); }
    @Override public Long sra(Long v1, Long v2) { return v1 >> v2; }
    @Override public Long sraw(Long v1, Long v2) { return (long) (v1.intValue() >> v2.intValue()); }

    @Override public boolean isTruthy(Long v) { return v != 0L; }
    @Override public byte asByte(Long v) { return v.byteValue(); }
    @Override public int asInt(Long v) { return v.intValue(); }
    @Override public short asShort(Long v) { return v.shortValue(); }
    @Override public long asLong(Long v) { return v.longValue(); }
    @Override public char asChar(Long v) { return (char) v.byteValue(); }
    @Override public MemoryValue asMemoryValue(Long v) { return new MemoryValue(v); }
}

public class ConcreteInterpreter extends GenericInterpreter<Long> {
    public static void main(String[] args) throws Exception {
        Globals.initialize();
        InstructionSet.rv64 = true;
        Globals.instructionSet.populate();
        ConcreteInterpreter interpreter = new ConcreteInterpreter();
        interpreter.runOn(args[0]);
    }
    public ConcreteInterpreter() {
        super(new ConcreteValues());
    }

    protected Long readChar() {
        try {
            long c = System.in.read();
            return c;
        } catch (IOException e) {
            throw new RuntimeException("cannot readChar: " + e);
        }
    }

    protected Long readInt() {
        Scanner s = new Scanner(System.in);
        return (long)(s.nextInt());
    }

    protected String readString(Long bufAddress, Long length) {
        String value = "";
        try {
            if (length > 0) {

                char c;
                int i;
                MemoryValue memValue;
                ConcolicValues op = new ConcolicValues();
                for (i = 0; i < length - 1; ++i) {
                    c = (char) System.in.read();
                    if (c == -1 || c == '\n') {
                        break;
                    }
                    memValue = new MemoryValue(c, MemoryValueTypes.BYTE, true);
                    this.memory.storeMemory(memValue, op.inject(bufAddress), op.inject(i));
                    value += c;
                }
                memValue = new MemoryValue(0, MemoryValueTypes.BYTE, true);
                this.memory.storeMemory(memValue, op.inject(bufAddress), op.inject(i));
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot readChar: " + e);
        } catch (AddressErrorException e) {
            throw new RuntimeException("Address error: " + e.getMessage());
        }
        return value;
    }

    @Override
    protected void sb(Long value, Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.BYTE, true);
            ConcolicValues op = new ConcolicValues();
            memory.storeMemory(bValue, op.inject(memAddress), op.inject(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sh(Long value, Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.HALFWORD, true);
            ConcolicValues op = new ConcolicValues();
            memory.storeMemory(bValue, op.inject(memAddress), op.inject(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sw(Long value, Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.WORD, true);
            ConcolicValues op = new ConcolicValues();
            memory.storeMemory(bValue, op.inject(memAddress), op.inject(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected void sd(Long value, Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(value, MemoryValueTypes.DOUBLEWORD, true);
            ConcolicValues op = new ConcolicValues();
            memory.storeMemory(bValue, op.inject(memAddress), op.inject(offset));
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            exit = true;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            exit = true;
        }
    }

    @Override
    protected Long lb(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.BYTE, false);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long lbu(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.BYTE, true);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long lh(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.HALFWORD, false);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long lhu(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.HALFWORD, true);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long lw(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.WORD, false);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long lwu(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.WORD, true);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }

    @Override
    protected Long ld(Long offset, Long memAddress) {
        try {
            MemoryValue bValue = new MemoryValue(MemoryValueTypes.DOUBLEWORD, false);
            ConcolicValues op = new ConcolicValues();
            memory.accessMemory(bValue, op.inject(memAddress), op.inject(offset));
            return bValue.getConcreteValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            output += "Access outside memory | ";
            return null;
        } catch (AddressErrorException e) {
            output += e.getMessage() + " | ";
            return null;
        }
    }
}
