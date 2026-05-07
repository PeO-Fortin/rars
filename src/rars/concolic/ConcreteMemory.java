package rars.concolic;

import rars.riscv.hardware.MemoryConfigurations;

public class ConcreteMemory {
    public static final int DEFAULT_STACK_POINTER = MemoryConfigurations.getDefaultStackPointer();
    public static final int DEFAULT_GLOBAL_POINTER = MemoryConfigurations.getDefaultGlobalPointer();

    public static final int DATA_BASE_ADDRESS = MemoryConfigurations.getDefaultDataSegmentBaseAddress();
    public static final int DATA_LIMIT_ADDRESS = DATA_BASE_ADDRESS + 4194304; //4MB
    public static final int HEAP_BASE_ADDRESS = MemoryConfigurations.getDefaultHeapBaseAddress();
    public static final int STACK_BASE_ADDRESS = MemoryConfigurations.getDefaultStackBaseAddress();
    public static final int STACK_LIMIT_ADDRESS = STACK_BASE_ADDRESS - 4096; //4KB

    public static final int DATA_SIZE = DATA_LIMIT_ADDRESS - DATA_BASE_ADDRESS;
    public static final int STACK_SIZE = STACK_BASE_ADDRESS - STACK_LIMIT_ADDRESS;

    private int heapAddress;

    private byte[] dataBlockTable;
    private byte[] stackBlockTable;

    public ConcreteMemory() {
        initialize();
    }

    public void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        dataBlockTable = new byte[DATA_SIZE];
        stackBlockTable = new byte[STACK_SIZE];
    }

    public int sBrk(int nbrBytesNeeded) {
        int initialHeapAddress = heapAddress;
        int size = MemoryValueSizes.DOUBLEWORD.getSize();
        if (nbrBytesNeeded < 0) {
            throw new IllegalArgumentException("request (" + nbrBytesNeeded + ") is negative heap amount");
        }

        heapAddress += nbrBytesNeeded;

        if (heapAddress % size != 0) {
            heapAddress = heapAddress + size - (heapAddress % size);
        }

        return initialHeapAddress;
    }

    /**
     * Gets the value at a specific address in memory, in a specific format.
     *
     * @param address   the address of the memory block
     * @param offset    the offset to apply to the address
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @param unsigned  true if the value must be unsigned, false if signed
     * @return the value at this address
     */
    public long accessMemory(long address, byte offset, MemoryValueSizes valueSize, boolean unsigned) {
        long value = 0;
        byte [] memoryBlockTable;
        int memoryBlockAddress;
        long memoryBlockValue;

        if (address > DATA_BASE_ADDRESS && address < DATA_LIMIT_ADDRESS) {
            memoryBlockTable = dataBlockTable;
            memoryBlockAddress = (int)(address - DATA_BASE_ADDRESS + offset);
        } else if (address < STACK_BASE_ADDRESS && address > STACK_LIMIT_ADDRESS) {
            memoryBlockTable = stackBlockTable;
            memoryBlockAddress = (int)(STACK_BASE_ADDRESS - address + offset);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }


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
     * Store a value from a specific size at a specif address in memory.
     * The store is made in little-endian.
     *
     * @param value     the value to store
     * @param address   the address where to store the value
     * @param offset    the offset to apply to the address
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     */
    public void storeMemory(long value, long address, byte offset, MemoryValueSizes valueSize) {
        int memoryBlockAddress = (int)(address - DATA_BASE_ADDRESS + offset);
        byte[] memoryBlockTable;
        value = maskedValue(value, valueSize);
        byte memoryBlockValue;
        long mask = 0xFF;

        if (address > DATA_BASE_ADDRESS && address < DATA_LIMIT_ADDRESS) {
            memoryBlockTable = dataBlockTable;
            memoryBlockAddress = (int)(address - DATA_BASE_ADDRESS + offset);
        } else if (address < STACK_BASE_ADDRESS && address > STACK_LIMIT_ADDRESS) {
            memoryBlockTable = stackBlockTable;
            memoryBlockAddress = (int)(STACK_BASE_ADDRESS - address + offset);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }

        for (int i = 0; i < valueSize.getSize(); i++) {
            mask = mask << (i * 8);
            memoryBlockValue = (byte) ((value ^ mask) >> (i * 8));
            memoryBlockTable[memoryBlockAddress + i] = memoryBlockValue;
        }
    }

    /**
     * Apply the right sign to an unsigned value.
     *
     * @param value     the unsigned value
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the right sign
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

    /**
     * Apply the correct mask to a value based on the size required.
     *
     * @param value     the value on which the mask has to be applied
     * @param valueSize the size required (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the correct mask applied
     */

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
