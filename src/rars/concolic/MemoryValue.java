package rars.concolic;

public class MemoryValue extends ConcolicValues {
    private V value;
    private MemoryValueTypes type;
    private boolean unsigned;

    public MemoryValue(long value) {
        this.value = new V (value, new SymbolicLong(value));
    }

    public MemoryValue(V value) {
        this.value = value;
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
        this.value = new V (value, new SymbolicLong(value));
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getConcreteValue() {
        return rawToRealValue(value.concrete);
    }

    public SymbolicValue getSymbolicValue() {
        SymbolicValue realSymb = value.symbolic;

        if (realSymb instanceof SymbolicLong) {
            realSymb = new SymbolicLong(rawToRealValue(value.concrete));
        }
        return realSymb;
    }

    public Long getRawValue() {
        return value.concrete;
    }

    public V getConcolicValue() {
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

    public void setConcreteValue(long value) {
        this.value.concrete = value;
    }

    public void setSymbolicValue(SymbolicValue symbolicValue) {
        this.value.symbolic = symbolicValue;
    }

    public void setConcolicValue(V value) {
        this.value = value;
    }

    public void setType (MemoryValueTypes type) {
        this.type = type;
    }

    public void setSize (int size) {
        switch (size) {
            case 1:
                this.type = MemoryValueTypes.BYTE;
                break;
            case 2:
                this.type = MemoryValueTypes.HALFWORD;
                break;
            case 4:
                this.type = MemoryValueTypes.WORD;
                break;
            case 8:
                this.type = MemoryValueTypes.DOUBLEWORD;
                break;
            default:
                System.out.println("Error: Invalid value size");
                break;
        }
    }

    public void setUnsigned(boolean unsigned) {
        this.unsigned = unsigned;
    }

    private long rawToRealValue(long value) {
        value = Utils.maskedValue(value, type);

        if (!unsigned) {
            value = Utils.signedValue(value, type);
        }

        return value;
    }
}