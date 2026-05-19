package rars.concolic;

public class BinaryValue {
    private Long value;
    private MemoryValueTypes type;
    private boolean unsigned;

    public BinaryValue(long value) {
        this.value = value;
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public BinaryValue(MemoryValueTypes type, boolean unsigned) {
        this.type = type;
        this.unsigned = unsigned;
    }

    public BinaryValue(long value, MemoryValueTypes type, boolean unsigned) {
        this.value = value;
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getValue() {
        long realValue = Utils.maskedValue(value, type);

        if (!unsigned) {
            realValue = Utils.signedValue(realValue, type);
        }

        return realValue;
    }

    public Long getRawValue() {
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

    public void setValue(long value) {
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
