package rars.concolic;

import rars.riscv.hardware.MemoryConfigurations;

public class Memory {
    public static final int MEMORY_BASE_ADDRESS = MemoryConfigurations.getDefaultDataBaseAddress();
    public static final int DEFAULT_STACK_POINTER = MemoryConfigurations.getDefaultStackPointer();
    public static final int HEAP_BASE_ADDRESS = MemoryConfigurations.getDefaultHeapBaseAddress();
    public static final int DATA_LIMIT_ADDRESS = MemoryConfigurations.getDefaultUserHighAddress();
    public static final int MEMORY_SIZE = DATA_LIMIT_ADDRESS - MEMORY_BASE_ADDRESS;

    private int heapAddress;

    private byte[] memoryBlockTable;

    public Memory() {
        initialize();
    }

    public void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        memoryBlockTable = new byte[MEMORY_SIZE];
    }

    public int sBrk(int nbrBytesNeeded) {
        int initialHeapAddress = heapAddress;
        heapAddress += nbrBytesNeeded;
        return initialHeapAddress;
    }

    /**
     *
     * @param address   the address of the memory block
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @param unsigned  true if the value must be unsigned, false if signed
     * @return the value at this address (64 bits)
     */
    public long accessMemory(long address, byte offset,MemoryValueSizes valueSize, boolean unsigned) {
        long value = 0;
        int memoryBlockAddress = (int)(address - MEMORY_BASE_ADDRESS + offset);
        long memoryBlockValue;

        //Check if negative Doubleword
        boolean dwNeg = (valueSize == valueSize.DOUBLEWORD &&
                memoryBlockTable[memoryBlockAddress + (valueSize.getSize() - 1)] < 0);

        if (dwNeg) {
            value = Long.MIN_VALUE; //
        }
        for (int i = 0; i < valueSize.getSize(); i++) {
            memoryBlockValue = memoryBlockTable[memoryBlockAddress + i];
            if (dwNeg && i == valueSize.getSize() - 1) {
                memoryBlockAddress -= 0x80; //Remove the MSB
            }
            memoryBlockValue = memoryBlockValue << (i * 8);
            value += memoryBlockValue;
        }

        if (!unsigned) {
            value = signedValue(value, valueSize);
        }

        return value;
    }

    /**
     *
     * @param value
     * @param address
     * @param valueSize
     */
    public void storeMemory(long value, long address, byte offset, MemoryValueSizes valueSize) {
        int memoryBlockAddress = (int)(address - MEMORY_BASE_ADDRESS + offset);
        value = maskedValue(value, valueSize);
        byte memoryBlockValue;
        long mask = 0xFF;

        for (int i = 0; i < valueSize.getSize(); i++) {
            mask = mask << (i * 8);
            memoryBlockValue = (byte) ((value ^ mask) >> (i * 8));
            memoryBlockTable[memoryBlockAddress + i] = memoryBlockValue;
        }
    }

    /**
     *
     * @param value
     * @param valueSize
     * @return
     */
    private long signedValue(long value, MemoryValueSizes valueSize) {
        switch (valueSize) {
            case BYTE:
                if(value > Byte.MAX_VALUE) {
                    value = Byte.MIN_VALUE + (value - Byte.MAX_VALUE);
                }
                break;
            case HALFWORD:
                if(value > Short.MAX_VALUE) {
                    value = Short.MIN_VALUE + (value - Short.MAX_VALUE);
                }
                break;
            case WORD:
                if(value > Integer.MAX_VALUE) {
                    value = Integer.MIN_VALUE + (value - Integer.MAX_VALUE);
                }
                break;
        }
        return value;
    }

    private long maskedValue(long value, MemoryValueSizes valueSize) {
        switch (valueSize) {
            case BYTE:
                value = value ^ 0xFF;
                break;
            case HALFWORD:
                value = value ^ 0xFFFF;
                break;
            case WORD:
                value = value ^ 0xFFFFFF;
                break;
        }
        return value;
    }
}
