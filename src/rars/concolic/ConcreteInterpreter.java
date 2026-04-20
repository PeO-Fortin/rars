package rars.concolic;

import rars.riscv.hardware.RegisterFile;

import java.io.IOException;
import java.util.Scanner;

class ConcreteValues extends InterpreterValues<Integer> {
    @Override public Integer inject(int i) { return i; }

    @Override public Integer add(Integer v1, Integer v2) { return v1 + v2; }
    @Override public Integer sub(Integer v1, Integer v2) { return v1 - v2; }
    @Override public Integer mul(Integer v1, Integer v2) { return v1 * v2; }
    @Override public Integer div(Integer v1, Integer v2) { return v1 / v2; }

    @Override public Integer eq(Integer v1, Integer v2) { return v1 == v2 ? 1 : 0; }
    @Override public Integer neq(Integer v1, Integer v2) { return v1 != v2 ? 1 : 0; }
    @Override public Integer geq(Integer v1, Integer v2) { return v1 >= v2 ? 1 : 0; }
    @Override public Integer lt(Integer v1, Integer v2) { return v1 < v2 ? 1 : 0; }

    @Override public Integer xor(Integer v1, Integer v2) { return v1 ^ v2; }
    @Override public Integer and(Integer v1, Integer v2) { return v1 & v2; }
    @Override public Integer or(Integer v1, Integer v2) { return v1 | v2; }

    @Override public Integer sll(Integer v1, Integer v2) { return v1 << v2; }
    @Override public Integer srl(Integer v1, Integer v2) { return v1 >>> v2; }
    @Override public Integer sra(Integer v1, Integer v2) { return v1 >> v2; }

    @Override public boolean isTruthy(Integer v) { return v != 0; }
    @Override public int asInt(Integer v) { return v.intValue(); }
    @Override public char asChar(Integer v) { return (char) v.intValue(); }
}

public class ConcreteInterpreter extends GenericInterpreter<Integer> {
    public static void main(String[] args) throws Exception {
        ConcreteInterpreter interpreter = new ConcreteInterpreter();
        interpreter.runOn(args[0]);
    }
    public ConcreteInterpreter() {
        super(new ConcreteValues());
    }

    protected void ecallManager(int ecallNumber) {
        Scanner scanner = new Scanner(System.in);

        switch (ecallNumber) {
            case 5:  // ReadInt
                registers[10] = values.inject(scanner.nextInt());
                break;
            case 12: // ReadChar
                registers[10] = values.inject(scanner.next().charAt(0));
                break;
            case 1:  // PrintInt
                System.out.print(values.asInt(registers[(RegisterFile.getRegister("a0").getNumber())]));
                break;
            case 11: // PrintChar
                System.out.print(values.asChar(registers[(RegisterFile.getRegister("a0").getNumber())]));
                break;
            case 34: // PrintIntHex
                System.out.printf("%x", values.asInt(registers[(RegisterFile.getRegister("a0").getNumber())]));
                break;
            case 35: // PrintIntBinary
                System.out.print(Integer.toBinaryString(values.asInt(registers[(RegisterFile.getRegister("a0").getNumber())])));
                break;
            default:
                break;
        }
    }

    protected Integer getchar() {
        try {
            int c = System.in.read();
            return c;
        } catch (IOException e) {
            throw new RuntimeException("cannot getchar: " + e);
        }
    }
}
