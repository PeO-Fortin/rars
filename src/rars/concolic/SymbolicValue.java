package rars.concolic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

abstract class SymbolicValue {
    public abstract Set<String> variables();
}
class SymbolicInteger extends SymbolicValue {
    int value;
    public SymbolicInteger(int value) {
        this.value = value;
    }
    public Set<String> variables() { return new HashSet<>(); }
    @Override public String toString() { return "" + value; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolicInteger)) return false;
        return value == ((SymbolicInteger) o).value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}

class SymbolicVariable extends SymbolicValue {
    String name;
    public SymbolicVariable(String name) {
        this.name = name;
    }
    public Set<String> variables() {
        Set<String> vars = new HashSet<>();
        vars.add(name);
        return vars;
    }
    @Override public String toString() { return name; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolicVariable)) return false;
        return name.equals(((SymbolicVariable) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

enum SymbolicOperator {
    Add,
    Sub,
    Mul,
    Div,
    Not,
    Eq,
    Neq,
    Geq,
    Lt,
    Xor,
    And,
    Or,
    Sll,
    Srl,
    Sra
}
class SymbolicOperation extends SymbolicValue {
    public SymbolicOperator operator;
    public SymbolicValue[] operands;
    public SymbolicOperation(SymbolicOperator operator, SymbolicValue[] operands) {
        this.operator = operator;
        this.operands = operands;
    }
    public Set<String> variables() {
        Set<String> vars = new HashSet<>();
        for (SymbolicValue v : operands) {
            vars.addAll(v.variables());
        }
        return vars;
    }
    @Override public String toString() {
        switch (operator) {
        case Add: return "(" + operands[0].toString() + "+" + operands[1].toString() + ")";
        case Sub: return "(" + operands[0].toString() + "-" + operands[1].toString() + ")";
        case Mul: return "(" + operands[0].toString() + "*" + operands[1].toString() + ")";
        case Div: return "(" + operands[0].toString() + "/" + operands[1].toString() + ")";
        case Not: return "(!" + operands[0].toString() + ")";
        case Eq: return "(" + operands[0].toString() + "==" + operands[1].toString() + ")";
        case Neq: return "(" + operands[0].toString() + "!=" + operands[1].toString() + ")";
        case Geq: return "(" + operands[0].toString() + ">=" + operands[1].toString() + ")";
        case Lt: return "(" + operands[0].toString() + "<" + operands[1].toString() + ")";
        case Xor: return "(" + operands[0].toString() + "^" + operands[1].toString() + ")";
        case And: return "(" + operands[0].toString() + "&" + operands[1].toString() + ")";
        case Or: return "(" + operands[0].toString() + "|" + operands[1].toString() + ")";
        case Sll: return "(" + operands[0].toString() + "<<" + operands[1].toString() + ")";
        case Srl: return "(" + operands[0].toString() + ">>>" + operands[1].toString() + ")";
        case Sra: return "(" + operands[0].toString() + ">>" + operands[1].toString() + ")";
        }
        return "unknown";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolicOperation)) return false;
        SymbolicOperation other = (SymbolicOperation) o;
        return operator == other.operator &&
            Arrays.equals(operands, other.operands);
    }

    @Override
    public int hashCode() {
        return 31 * operator.hashCode() + Arrays.hashCode(operands);
    }
}
