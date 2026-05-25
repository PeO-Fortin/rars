package rars.concolic;

public class MemoryValue {
    private ConcolicValues.V value;
    private MemoryValueTypes type;
    private boolean unsigned;

    public MemoryValue(long value) {
        this.value = new ConcolicValues.V (value, new SymbolicLong(value));
    }

    public MemoryValue(ConcolicValues.V value) {
        this.value = value;
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public MemoryValue(MemoryValueTypes type, boolean unsigned) {
        this.type = type;
        this.unsigned = unsigned;
    }

    public MemoryValue(ConcolicValues.V value, MemoryValueTypes type, boolean unsigned) {
        this.value = value;
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getConcreteValue() {
        long realValue = Utils.maskedValue(value.concrete, type);

        if (!unsigned) {
            realValue = Utils.signedValue(realValue, type);
        }

        return realValue;
    }

    public SymbolicValue getSymbolicValue() {
        return value.symbolic;
    }

    public Long getRawValue() {
        return value.concrete;
    }

    public ConcolicValues.V getConcolicValue() {
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

    public void setConcolicValue(ConcolicValues.V value) {
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
}