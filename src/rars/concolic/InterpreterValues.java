package rars.concolic;

abstract class InterpreterValues<V> {
    public abstract V inject(long i);

    //Arithmetic
    public abstract V add(V v1, V v2);
    public abstract V addw(V v1, V v2);
    public abstract V sub(V v1, V v2);
    public abstract V subw(V v1, V v2);
    public abstract V mul(V v1, V v2);
    public abstract V mulw(V v1, V v2);
    public abstract V div(V v1, V v2);
    public abstract V divw(V v1, V v2);

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
    public abstract V sllw(V v1, V v2);
    public abstract V srl(V v1, V v2);
    public abstract V srlw(V v1, V v2);
    public abstract V sra(V v1, V v2);
    public abstract V sraw(V v1, V v2);

    //Others
    public abstract boolean isTruthy(V v);
    public abstract byte asByte(V v);
    public abstract long asLong(V v);
    public abstract int asInt(V v);
    public abstract char asChar(V v);
}
