package rars.concolic;

import rars.riscv.hardware.MemoryConfigurations;

public class Memory {
    public static final int MEMORY_BASE_ADDRESS = MemoryConfigurations.getDefaultDataBaseAddress();
    public static final int DEFAULT_STACK_POINTER = MemoryConfigurations.getDefaultStackPointer();
    public static final int HEAP_BASE_ADDRESS = MemoryConfigurations.getDefaultHeapBaseAddress();
    public static final int MEMORY_SIZE = 4096;

    private int heapAddress;
    private int stackPointer;

    private byte[] dataBlockTable;
    private byte[] stackBlockTable;

    public Memory() {
        initialize();
    }

    public void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        stackPointer = DEFAULT_STACK_POINTER;
        dataBlockTable = new byte[MEMORY_SIZE];
        stackBlockTable = new byte[MEMORY_SIZE];
    }

    public int  getStackPointer() {
        return stackPointer;
    }

    public int getHeapAddress() {
        return heapAddress;
    }

    public int moveStackPointer(int offset) {
        stackPointer += offset;
        return stackPointer;
    }

    public int sBrk(int nbrBytesNeeded) {
        int initialHeapAddress = heapAddress;
        heapAddress += nbrBytesNeeded;
        return initialHeapAddress;
    }

    /**
     *
     * @param address
     * @return
     */
    public long accessStack(int address, MemoryValueTypes valueType, boolean unsigned) {
        long value = 0;
        int memoryBlockAddress;
        int memoryBlockValue;
        try {
            memoryBlockAddress = Math.abs(address - DEFAULT_STACK_POINTER);

            //Check if negative Doubleword
            boolean dwNeg = (valueType == valueType.DOUBLEWORD &&
                    stackBlockTable[memoryBlockAddress + (valueType.getSize() - 1)] < 0);

            if (dwNeg) {
                value = Long.MIN_VALUE;
            }
            for (int i = 0; i < valueType.getSize(); i++) {
                memoryBlockValue = stackBlockTable[memoryBlockAddress + i];
                if (dwNeg && i == valueType.getSize() - 1) {
                    memoryBlockAddress -= 0x80;
                }
                memoryBlockValue = memoryBlockValue << (i * 8);
                value += memoryBlockValue;
            }


            if (!unsigned) {
                value = signedValue(value, valueType);
            }
        } catch (ArrayIndexOutOfBoundsException e) { }
        return value;
    }

    /**
     *
     * @param address
     * @return
     */
    public long accessMemory(int address, MemoryValueTypes valueType, boolean unsigned) {
        long value = 0;
        int memoryBlockAddress;
        int memoryBlockValue;
        try {
            memoryBlockAddress = address - MEMORY_BASE_ADDRESS;

            //Check if negative Doubleword
            boolean dwNeg = (valueType == valueType.DOUBLEWORD &&
                    dataBlockTable[memoryBlockAddress + (valueType.getSize() - 1)] < 0);

            if (dwNeg) {
                value = Long.MIN_VALUE;
            }
            for (int i = 0; i < valueType.getSize(); i++) {
                memoryBlockValue = dataBlockTable[memoryBlockAddress + i];
                if (dwNeg && i == valueType.getSize() - 1) {
                    memoryBlockAddress -= 0x80;
                }
                memoryBlockValue = memoryBlockValue << (i * 8);
                value += memoryBlockValue;
            }

            if (!unsigned) {
                value = signedValue(value, valueType);
            }
        } catch (ArrayIndexOutOfBoundsException e) { }
        return value;
    }

    /**
     *
     * @param value
     * @param valueType
     * @return
     */
    private long signedValue(long value, MemoryValueTypes valueType) {
        switch (valueType) {
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



}
