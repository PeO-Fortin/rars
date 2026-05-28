package rars.concolic;

public class MemoryValue extends ConcolicValues {
    private V value;
    private MemoryValueTypes type;
    private boolean unsigned;

    public MemoryValue(ConcolicValues.V value) {
        this.value = value;
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public MemoryValue(long value) {
        this.value = new V(value, new SymbolicLong(value));
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public MemoryValue(MemoryValueTypes type, boolean unsigned) {
        this.type = type;
        this.unsigned = unsigned;
    }

    public MemoryValue(V value, MemoryValueTypes type, boolean unsigned) {
        this.value = value;
        this.type = type;
        this.unsigned = unsigned;
    }

    public MemoryValue(long value, MemoryValueTypes type, boolean unsigned) {
        this.value = new V(value, new SymbolicLong(value));
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getConcreteValue() {
        return rawToRealValue().concrete;
    }

    public SymbolicValue getSymbolicValue() {;
        return rawToRealValue().symbolic;
    }

    public ConcolicValues.V getConcolicValue() {
        return rawToRealValue();
    }

    public V getRawValue() {
        return value;
    }

    public MemoryValueTypes getType() {
        return type;
    }

    public int getSize() {
        return type.getSize();
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public void setValue(V value) {
        this.value = value;
    }

    private ConcolicValues.V rawToRealValue() {
        ConcolicValues.V realValue = new ConcolicValues.V(value.concrete, value.symbolic);
        realValue = Utils.maskedValue(realValue, type);

        if (!unsigned) {
            realValue = Utils.signedValue(realValue, type);
        }

        return realValue;
    }
}