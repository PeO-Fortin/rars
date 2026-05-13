package rars.concolic;

public enum MemoryValueTypes {
    BYTE(1),
    HALFWORD(2),
    WORD(4),
    DOUBLEWORD(8);

    private final int size;

    MemoryValueTypes(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
