package rars.concolic;

import rars.riscv.hardware.AddressErrorException;

import java.io.IOException;
import java.util.Scanner;

class ConcreteValues extends InterpreterValues<Long> {
    @Override public Long inject(long i) { return i; }

    @Override public Long add(Long v1, Long v2) { return v1 + v2; }
    @Override public Long addw(Long v1, Long v2) { return (long)((v1.intValue() + v2.intValue())); }
    @Override public Long sub(Long v1, Long v2) { return v1 - v2; }
    @Override public Long subw(Long v1, Long v2) { return (long)(v1.intValue() - v2.intValue()); }
    @Override public Long mul(Long v1, Long v2) { return v1 * v2; }
    @Override public Long mulw(Long v1, Long v2) { return (long)(v1.intValue() * v2.intValue()); }
    @Override public Long div(Long v1, Long v2) { return v1 / v2; }
    @Override public Long divw(Long v1, Long v2) { return (long)(v1.intValue() / v2.intValue()); }

    @Override public Long eq(Long v1, Long v2) { return (long)(v1 == v2 ? 1 : 0); }
    @Override public Long neq(Long v1, Long v2) { return (long)(v1 != v2 ? 1 : 0); }
    @Override public Long geq(Long v1, Long v2) { return (long)(v1 >= v2 ? 1 : 0); }
    @Override public Long lt(Long v1, Long v2) { return (long)(v1 < v2 ? 1 : 0); }

    @Override public Long xor(Long v1, Long v2) { return v1 ^ v2; }
    @Override public Long and(Long v1, Long v2) { return v1 & v2; }
    @Override public Long or(Long v1, Long v2) { return v1 | v2; }

    @Override public Long sll(Long v1, Long v2) { return v1 << v2; }
    @Override public Long sllw(Long v1, Long v2) { return (long) (v1.intValue() << v2.intValue()); }
    @Override public Long srl(Long v1, Long v2) { return v1 >>> v2; }
    @Override public Long srlw(Long v1, Long v2) { return (long) (v1.intValue() >>> v2.intValue()); }
    @Override public Long sra(Long v1, Long v2) { return v1 >> v2; }
    @Override public Long sraw(Long v1, Long v2) { return (long) (v1.intValue() >> v2.intValue()); }

    @Override public boolean isTruthy(Long v) { return v != 0; }
    @Override public byte asByte(Long v) { return v.byteValue(); }
    @Override public int asInt(Long v) { return v.intValue(); }
    @Override public short asShort(Long v) { return v.shortValue(); }
    @Override public long asLong(Long v) { return v.longValue(); }
    @Override public char asChar(Long v) { return (char) v.byteValue(); }
    @Override public BinaryValue asBinaryValue(Long v) { return new BinaryValue(v); }
}

public class ConcreteInterpreter extends GenericInterpreter<Long> {
    public static void main(String[] args) throws Exception {
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

    protected void readString(long bufAddress, long length) throws AddressErrorException {
        try {
            if (length > 0) {
                char c;
                int i;
                BinaryValue value;
                for (i = 0; i < length - 1; ++i) {
                    c = (char) System.in.read();
                    if (c == -1 || c == '\n') {
                        break;
                    }
                    value = new BinaryValue(c, MemoryValueTypes.BYTE, true);
                    this.memory.storeMemory(value, bufAddress, i);
                }
                value = new BinaryValue(0, MemoryValueTypes.BYTE, true);
                this.memory.storeMemory(value, bufAddress, i);
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot readChar: " + e);
        }
    }
}
