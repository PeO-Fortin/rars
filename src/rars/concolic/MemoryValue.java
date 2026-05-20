package rars.concolic;

public class MemoryValue {
    private Long concreteValue;
    private MemoryValueTypes type;
    private boolean unsigned;
    private SymbolicValue symbolicValue;

    public MemoryValue(long value) {
        this.concreteValue = value;
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public MemoryValue(MemoryValueTypes type, boolean unsigned) {
        this.type = type;
        this.unsigned = unsigned;
    }

    public MemoryValue(long value, MemoryValueTypes type, boolean unsigned) {
        this.concreteValue = value;
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getConcreteValue() {
        long realValue = Utils.maskedValue(concreteValue, type);

        if (!unsigned) {
            realValue = Utils.signedValue(realValue, type);
        }

        return realValue;
    }

    public SymbolicValue getSymbolicValue() {
        return symbolicValue;
    }

    public Long getRawValue() {
        return concreteValue;
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
        this.concreteValue = value;
    }

    public void setSymbolicValue(SymbolicValue symbolicValue) {
        this.symbolicValue = symbolicValue;
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
