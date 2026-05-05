package rars.concolic;

import java.io.IOException;
import java.util.Scanner;

class ConcreteValues extends InterpreterValues<Number> {
    @Override public Integer inject(int i) { return i; }

    @Override public Integer add(Number v1, Number v2) { return (Integer) v1 + (Integer) v2; }
    @Override public Integer sub(Number v1, Number v2) { return (Integer) v1 - (Integer) v2; }
    @Override public Integer mul(Number v1, Number v2) { return (Integer) v1 * (Integer) v2; }
    @Override public Integer div(Number v1, Number v2) { return (Integer) v1 / (Integer) v2; }

    @Override public Integer eq(Number v1, Number v2) { return (Integer) v1 == (Integer) v2 ? 1 : 0; }
    @Override public Integer neq(Number v1, Number v2) { return (Integer) v1 != (Integer) v2 ? 1 : 0; }
    @Override public Integer geq(Number v1, Number v2) { return (Integer) v1 >= (Integer) v2 ? 1 : 0; }
    @Override public Integer lt(Number v1, Number v2) { return (Integer) v1 < (Integer) v2 ? 1 : 0; }

    @Override public Integer xor(Number v1, Number v2) { return (Integer) v1 ^ (Integer) v2; }
    @Override public Integer and(Number v1, Number v2) { return (Integer) v1 & (Integer) v2; }
    @Override public Integer or(Number v1, Number v2) { return (Integer) v1 | (Integer) v2; }

    @Override public Integer sll(Number v1, Number v2) { return (Integer) v1 << (Integer) v2; }
    @Override public Integer srl(Number v1, Number v2) { return (Integer) v1 >>> (Integer) v2; }
    @Override public Integer sra(Number v1, Number v2) { return (Integer) v1 >> (Integer) v2; }

    @Override public boolean isTruthy(Number v) { return (Integer) v != 0; }
    @Override public int asInt(Number v) { return v.intValue(); }
    @Override public char asChar(Number v) { return (char) v.intValue(); }
    @Override public float asFloat(Number v) { return v.floatValue(); }
    @Override public double asDouble(Number v) { return v.doubleValue(); }
}

public class ConcreteInterpreter extends GenericInterpreter<Number> {
    public static void main(String[] args) throws Exception {
        ConcreteInterpreter interpreter = new ConcreteInterpreter();
        interpreter.runOn(args[0]);
    }
    public ConcreteInterpreter() {
        super(new ConcreteValues());
    }

    protected Integer readChar() {
        try {
            int c = System.in.read();
            return c;
        } catch (IOException e) {
            throw new RuntimeException("cannot readChar: " + e);
        }
    }

    protected Integer readInt() {
        Scanner s = new Scanner(System.in);
        return s.nextInt();
    }

    protected Float readFloat() {
        Scanner s = new Scanner(System.in);
        return s.nextFloat();
    }

    protected Double readDouble() {
        Scanner s = new Scanner(System.in);
        return s.nextDouble();
    }
}
