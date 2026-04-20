package rars.concolic;

import language_minic.*;
import minic.*;
import minic.front.*;
import minic.ir.*;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GenericInterpreter<V> {
    InterpreterValues<V> values;
    public GenericInterpreter(InterpreterValues<V> values) {
        this.values = values;
    }

    Map<String, Function> functions;
    public void runOn(String filename) throws Exception {
        prepare(filename);
        runMain(new ArrayList<V>());
    }

    public void prepare(String filename) throws Exception {
        MiniCC2 compiler = new MiniCC2();
        Reader r = new FileReader(filename);
        Parser parser = new Parser(r);
        Node syntaxTree = parser.parse();
        syntaxTree.apply(compiler.litteralAnalysis);
        syntaxTree.apply(compiler.scopeAnalysis);
        syntaxTree.apply(compiler.typeAnalysis);
        syntaxTree.apply(compiler);

        for (Function function : compiler.scopeAnalysis.functions.values()) {
            CFG cfg = function.cfg;
            function.file = filename;
            compiler.passManager.processPasses(cfg);
        }
        functions = compiler.scopeAnalysis.functions;
    }

    Map<String, V> memory = new HashMap<>();
    void write(Operand r, V v) {
        memory.put(r.toString(), v);
    }

    V read(Operand r) {
        return memory.get(r.toString());
    }

    protected BasicBlock currentBlock;
    void run() {
        while (currentBlock != null) {
            for (IR ir : currentBlock.instructions) {
                switch (ir.instr) {
                case Add: add(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Sub: sub(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Mul: mul(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Lt: lt(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Mv: mv(toValue(ir.src[0]), ir.dst); break;
                case Jmp: jmp(ir.labels[0]); break;
                case Iflt: iflt(toValue(ir.src[0]), toValue(ir.src[1]), ir.labels[0], ir.labels[1]); break;
                case If: if_(toValue(ir.src[0]), ir.labels[0], ir.labels[1]); break;
                case Ifneq: ifneq(toValue(ir.src[0]), toValue(ir.src[1]), ir.labels[0], ir.labels[1]); break;
                case Ret: ret(toValue(ir.src[0])); return;
                case Call: call(ir.symbol, toValues(ir.src), ir.dst); break;
                case Not: not(toValue(ir.src[0]), ir.dst); break;
                case Neq: neq(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Bxor: bxor(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Band: band(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Bor: bor(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Bshl: bshl(toValue(ir.src[0]), toValue(ir.src[1]), ir.dst); break;
                case Phi: throw new RuntimeException("should not have phis");
                }
            }
        }
    }

    public V runMain(List<V> arguments) {
        Register resultRegister = new Register(0, "ret");
        call("main", arguments, resultRegister);
        return read(resultRegister);
    }

    protected void setCurrentBlock(BasicBlock target) {
        currentBlock = target;
    }

    protected void setCurrentBlockCond(BasicBlock target) {
        setCurrentBlock(target);
    }

    void add(V left, V right, Register dst) {
        write(dst, values.add(left, right));
    }

    void sub(V left, V right, Register dst) {
        write(dst, values.sub(left, right));
    }

    void mul(V left, V right, Register dst) {
        write(dst, values.mul(left, right));
    }

    void lt(V left, V right, Register dst) {
        write(dst, values.lt(left, right));
    }

    void mv(V v, Register dst) {
        write(dst, v);
    }

    void not(V v, Register dst) {
        write(dst, values.not(v));
    }

    void jmp(BasicBlock target) {
        setCurrentBlock(target);
    }

    void iflt(V left, V right, BasicBlock then, BasicBlock else_) {
        if_(values.lt(left, right), then, else_);
    }

    void ifneq(V left, V right, BasicBlock then, BasicBlock else_) { if_(values.neq(left, right), then, else_); }

    void neq(V left, V right, Register dst) { write(dst, values.neq(left, right)); }

    void bxor(V left, V right, Register dst) { write(dst, values.bxor(left, right)); }

    void band(V left, V right, Register dst) { write(dst, values.band(left, right)); }

    void bor(V left, V right, Register dst) { write(dst, values.bor(left, right)); }

    void bshl(V left, V right, Register dst) { write(dst, values.bshl(left, right)); }

    protected void if_(V cond, BasicBlock then, BasicBlock else_) {
        if (values.isTruthy(cond)) {
            setCurrentBlockCond(then);
        } else {
            setCurrentBlockCond(else_);
        }
    }

    Register returnRegister;
    public void call(String function, List<V> args, Register dst) {
        switch (function) {
        case "printint":
            System.out.println(values.asInt(args.get(0)));
            return;
        case "printbool":
            System.out.println(!values.isTruthy(args.get(0)) ? "false" : "true");
            return;
        case "println":
            System.out.println("");
            return;
        case "putchar":
            System.out.print(values.asChar(args.get(0)));
            return;
        case "getchar":
            V c = getchar();
            if (dst != null) {
                write(dst, c);
            }
            return;
        }
        int i = 0;
        for (V arg : args) {
            Parameter param = new Parameter(i);
            write(param, arg);
            i++;
        }
        Function f = functions.get(function);
        if (f == null) { throw new RuntimeException("undefined function: " + function); }
        BasicBlock returnBlock = currentBlock;
        Register previousReturnRegister = returnRegister;
        returnRegister = dst;
        setCurrentBlock(f.cfg.entryBlock);
        run();
        currentBlock = returnBlock;
        returnRegister = previousReturnRegister;
    }

    protected abstract V getchar();

    void ret(V v) {
        write(returnRegister, v);
    }

    V toValue(Operand operand) {
        if (operand instanceof Constant) {
            return values.inject((Constant ) operand);
        } else if (operand instanceof Register) {
            return read(operand);
        } else if (operand instanceof Parameter) {
            return read(operand);
        } else {
            throw new RuntimeException("invalid case");
        }
    }

    List<V> toValues(Operand[] operands) {
        List<V> values = new ArrayList<>();
        for (Operand operand : operands) {
            values.add(toValue(operand));
        }
        return values;
    }
}
