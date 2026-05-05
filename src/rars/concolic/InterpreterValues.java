package rars.concolic;

abstract class InterpreterValues<V> {
    public abstract V inject(int i);

    //Arithmetic
    public abstract V add(V v1, V v2);
    public abstract V sub(V v1, V v2);
    public abstract V mul(V v1, V v2);
    public abstract V div(V v1, V v2);

    //Branches
    public abstract V eq(V v1, V v2);
    public abstract V neq(V v1, V v2);
    public abstract V geq(V v1, V v2);
    public abstract V lt(V v1, V v2);

    //Logical operations
    public abstract V xor(V v1, V v2);
    public abstract V and(V v1, V v2);
    public abstract V or(V v1, V v2);

    //Shifts
    public abstract V sll(V v1, V v2);
    public abstract V srl(V v1, V v2);
    public abstract V sra(V v1, V v2);

    //Others
    public abstract boolean isTruthy(V v);
    public abstract int asInt(V v);
    public abstract char asChar(V v);
    public abstract double asDouble(V v);
    public abstract float asFloat(V v);
}
