package rars.concolic;

public class ConcolicValues extends InterpreterValues<ConcolicValues.V> {
    static class V {
        Long concrete;
        SymbolicValue symbolic;
        public V(long concrete, SymbolicValue symbolic) {
            this.concrete = concrete;
            this.symbolic = symbolic;
        }
    }

    public V inject(long i) {
            return new V(i, new SymbolicLong(i));
    }
    public V access(MemoryValue v) {
        if(v == null) {
            return inject(0);
        }
        return new V(v.getConcreteValue(), v.getSymbolicValue());
    }

    public static V variable(long value, String name) {
        return new V(value, new SymbolicVariable(name));
    }
    interface Op {
        public long apply(long[] args);
        public SymbolicOperator operator();
    }

    V op(Op op, V[] args) {
        boolean remainsSymbolic = false;
        for (V arg : args) {
            if (!(arg.symbolic instanceof SymbolicLong || arg.symbolic instanceof SymbolicByte)) {
                remainsSymbolic = true;
            }
        }

        SymbolicValue symbolic;
        if (remainsSymbolic) {
            SymbolicValue[] operands = new SymbolicValue[args.length];
            for (int i = 0; i < args.length; i++) {
                operands[i] = args[i].symbolic;
            }
            symbolic = new SymbolicOperation(op.operator(), operands);
        } else {
            long[] operands = new long[args.length];
            for (int i = 0; i < args.length; i++) {
                operands[i] = ((SymbolicLong) args[i].symbolic).value;
            }
            symbolic = new SymbolicLong(op.apply(operands));
        }
        long[] concreteOperands = new long[args.length];
        for (int i = 0; i < args.length; i++) {
            concreteOperands[i] = asLong(args[i]);
        }
        return new V(op.apply(concreteOperands), symbolic);
    }

    @Override
    public V add(V v1, V v2) {
        return op(new Op() {
                public long apply(long[] args) { return args[0] + args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Add; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V addw(V v1, V v2) {
        V v1_correct = Utils.maskedValue(v1, MemoryValueTypes.WORD);
        v1_correct = Utils.signedValue(v1_correct, MemoryValueTypes.WORD);
        V v2_correct = Utils.maskedValue(v2, MemoryValueTypes.WORD);
        v2_correct = Utils.signedValue(v2_correct, MemoryValueTypes.WORD);

        return add(v1_correct, v2_correct);
    }

    @Override
    public V sub(V v1, V v2) {
        return op(new Op() {
                public long apply(long[] args) { return args[0] - args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Sub; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V subw(V v1, V v2) {
        V v1_correct = Utils.maskedValue(v1, MemoryValueTypes.WORD);
        v1_correct = Utils.signedValue(v1_correct, MemoryValueTypes.WORD);
        V v2_correct = Utils.maskedValue(v2, MemoryValueTypes.WORD);
        v2_correct = Utils.signedValue(v2_correct, MemoryValueTypes.WORD);

        return sub(v1_correct, v2_correct);
    }

    @Override
    public V mul(V v1, V v2) {
        return op(new Op() {
                public long apply(long[] args) { return args[0] * args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Mul; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V mulw(V v1, V v2) {
        V v1_correct = Utils.maskedValue(v1, MemoryValueTypes.WORD);
        v1_correct = Utils.signedValue(v1_correct, MemoryValueTypes.WORD);
        V v2_correct = Utils.maskedValue(v2, MemoryValueTypes.WORD);
        v2_correct = Utils.signedValue(v2_correct, MemoryValueTypes.WORD);

        return mul(v1_correct, v2_correct);
    }

    @Override
    public V mulh(V v1, V v2) {
        return mul(sra(v1, inject(32)), sra(v2, inject(32)));
    }

    @Override
    public V mulhsu(V v1, V v2) {
        return mul(sra(v1, inject(32)), srl(v2, inject(32)));
    }

    @Override
    public V mulhu(V v1, V v2) {
        return mul(srl(v1, inject(32)), srl(v2, inject(32)));
    }

    @Override
    public V div(V v1, V v2) {
        return op(new Op() {
                public long apply(long[] args) { return args[0] / args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Div; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V divw(V v1, V v2) {
        V v1_correct = Utils.maskedValue(v1, MemoryValueTypes.WORD);
        v1_correct = Utils.signedValue(v1_correct, MemoryValueTypes.WORD);
        V v2_correct = Utils.maskedValue(v2, MemoryValueTypes.WORD);
        v2_correct = Utils.signedValue(v2_correct, MemoryValueTypes.WORD);

        return div(v1_correct, v2_correct);
    }

    @Override
    public V divu(V v1, V v2) {

    }

    @Override
    public V divuw(V v1, V v2) {

    }

    @Override
    public V eq(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] == args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Eq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V neq(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] != args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Neq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V geq(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] >= args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Geq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V lt(V v1, V v2) {
        return op(new Op() {
                public long apply(long[] args) { return args[0] < args[1] ? 1 : 0; }
                public SymbolicOperator operator() { return SymbolicOperator.Lt; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V xor(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] ^ args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Xor; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V and(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] & args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.And; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V or(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] | args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Or; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V sll(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] << args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Sll; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V sllw(V v1, V v2) {
        return Utils.signedValue(sll(v1, v2), MemoryValueTypes.WORD);
    }

    @Override
    public V srl(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return (int)args[0] >>> (int)args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Srl; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V srlw(V v1, V v2) {
        return Utils.signedValue(srl(v1, v2), MemoryValueTypes.WORD);
    }

    @Override
    public V sra(V v1, V v2) {
        return op(new Op() {
            public long apply(long[] args) { return args[0] >> args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Sra; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V sraw(V v1, V v2) {
        return Utils.signedValue(sra(v1, v2), MemoryValueTypes.WORD);
    }

    @Override
    public boolean isTruthy(V v) {
        return v.concrete != 0;
    }

    @Override
    public byte asByte(V v) {return v.concrete.byteValue(); }

    @Override
    public short asShort(V v) { return v.concrete.shortValue(); }

    @Override
    public int asInt(V v) { return v.concrete.intValue(); }

    @Override
    public long asLong(V v) { return v.concrete; }

    @Override
    public char asChar(V v) { return (char) v.concrete.byteValue(); }

    @Override
    public MemoryValue asMemoryValue(V v) {
        return new MemoryValue(v);
    }
}
