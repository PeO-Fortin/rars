package rars.concolic;

import java.io.IOException;
import java.util.Scanner;

class ConcreteValues extends InterpreterValues<Long> {
    @Override public Long inject(long i) { return i; }

    @Override public Long add(Long v1, Long v2) { return v1 + v2; }
    @Override public Long sub(Long v1, Long v2) { return v1 - v2; }
    @Override public Long mul(Long v1, Long v2) { return v1 * v2; }
    @Override public Long div(Long v1, Long v2) { return v1 / v2; }

    @Override public Long eq(Long v1, Long v2) { return (long)(v1 == v2 ? 1 : 0); }
    @Override public Long neq(Long v1, Long v2) { return (long)(v1 != v2 ? 1 : 0); }
    @Override public Long geq(Long v1, Long v2) { return (long)(v1 >= v2 ? 1 : 0); }
    @Override public Long lt(Long v1, Long v2) { return (long)(v1 < v2 ? 1 : 0); }

    @Override public Long xor(Long v1, Long v2) { return v1 ^ v2; }
    @Override public Long and(Long v1, Long v2) { return v1 & v2; }
    @Override public Long or(Long v1, Long v2) { return v1 | v2; }

    @Override public Long sll(Long v1, Long v2) { return v1 << v2; }
    @Override public Long srl(Long v1, Long v2) { return v1 >>> v2; }
    @Override public Long sra(Long v1, Long v2) { return v1 >> v2; }

    @Override public boolean isTruthy(Long v) { return v != 0; }
    @Override public byte asByte(Long v) { return v.byteValue(); }
    @Override public int asInt(Long v) { return v.intValue(); }
    @Override public char asChar(Long v) { return (char) v.byteValue(); }
    @Override public long asLong(Long v) { return v.longValue(); }
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
}
