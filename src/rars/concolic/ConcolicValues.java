package rars.concolic;

public class ConcolicValues extends InterpreterValues<ConcolicValues.V> {
    static class V {
        int concrete;
        SymbolicValue symbolic;
        public V(int concrete, SymbolicValue symbolic) {
            this.concrete = concrete;
            this.symbolic = symbolic;
        }
    }

    public V inject(int i) {
            return new V(i, new SymbolicInteger(i));
    }

    public static V variable(int value, String name) {
        return new V(value, new SymbolicVariable(name));
    }
    interface Op {
        public int apply(int[] args);
        public SymbolicOperator operator();
    }

    V op(Op op, V[] args) {
        boolean remainsSymbolic = false;
        for (V arg : args) {
            if (!(arg.symbolic instanceof SymbolicInteger)) {
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
            int[] operands = new int[args.length];
            for (int i = 0; i < args.length; i++) {
                operands[i] = ((SymbolicInteger) args[i].symbolic).value;
            }
            symbolic = new SymbolicInteger(op.apply(operands));
        }
        int[] concreteOperands = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            concreteOperands[i] = args[i].concrete;
        }
        return new V(op.apply(concreteOperands), symbolic);
    }

    @Override
    public V add(V v1, V v2) {
        return op(new Op() {
                public int apply(int[] args) { return args[0] + args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Add; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V sub(V v1, V v2) {
        return op(new Op() {
                public int apply(int[] args) { return args[0] - args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Sub; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V mul(V v1, V v2) {
        return op(new Op() {
                public int apply(int[] args) { return args[0] * args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Mul; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V div(V v1, V v2) {
        return op(new Op() {
                public int apply(int[] args) { return args[0] / args[1]; }
                public SymbolicOperator operator() { return SymbolicOperator.Div; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V eq(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] == args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Eq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V neq(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] != args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Neq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V geq(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] >= args[1] ? 1 : 0; }
            public SymbolicOperator operator() { return SymbolicOperator.Geq; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V lt(V v1, V v2) {
        return op(new Op() {
                public int apply(int[] args) { return args[0] < args[1] ? 1 : 0; }
                public SymbolicOperator operator() { return SymbolicOperator.Lt; }
            }, new V[]{ v1, v2 });
    }

    @Override
    public V xor(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] ^ args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Xor; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V and(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] & args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.And; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V or(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] | args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Or; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V sll(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] << args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Sll; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V srl(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] >>> args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Srl; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public V sra(V v1, V v2) {
        return op(new Op() {
            public int apply(int[] args) { return args[0] >> args[1]; }
            public SymbolicOperator operator() { return SymbolicOperator.Sra; }
        }, new V[]{ v1, v2 });
    }

    @Override
    public boolean isTruthy(V v) {
        return v.concrete != 0;
    }

    @Override
    public int asInt(V v) {
        return v.concrete;
    }

    @Override
    public char asChar(V v) {
        return (char) v.concrete;
    }

}
