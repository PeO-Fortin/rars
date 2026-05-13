package rars.concolic;

import rars.riscv.hardware.Memory;
import rars.riscv.hardware.MemoryConfigurations;
import rars.riscv.hardware.AddressErrorException;

public class ConcreteMemory {
    public static final int DEFAULT_STACK_POINTER = MemoryConfigurations.getDefaultStackPointer();
    public static final int DEFAULT_GLOBAL_POINTER = MemoryConfigurations.getDefaultGlobalPointer();
    public static final int DATA_SIZE = 4194304; //4MB
    public static final int STACK_SIZE = 8192; //8KB

    public static final int DATA_BASE_ADDRESS = MemoryConfigurations.getDefaultDataSegmentBaseAddress();
    public static final int DATA_LIMIT_ADDRESS = DATA_BASE_ADDRESS + DATA_SIZE;
    public static final int HEAP_BASE_ADDRESS = MemoryConfigurations.getDefaultHeapBaseAddress();
    public static final long STACK_BASE_ADDRESS = MemoryConfigurations.getDefaultStackBaseAddress();
    public static final long STACK_LIMIT_ADDRESS = STACK_BASE_ADDRESS - STACK_SIZE;

    private int heapAddress;

    private byte[] dataBlockTable;
    private byte[] stackBlockTable;

    public ConcreteMemory() {
        initialize();
    }

    private void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        dataBlockTable = new byte[DATA_SIZE];
        stackBlockTable = new byte[STACK_SIZE];
        intializeData();
    }

    private void intializeData() {
        Memory rarsMemory = Memory.getInstance();
        int base = MemoryConfigurations.getDefaultDataBaseAddress();
        BinaryValue value = new BinaryValue(MemoryValueTypes.WORD, true);
        for(int i = 0; i < dataBlockTable.length; i += 4){
            try {
                value.setValue(rarsMemory.getRawWordOrNull(base + i));
                if (value.getRawValue() == null) {
                    break;
                }
                storeMemory(value, (base + i), 0);
            } catch (AddressErrorException e) {
                System.out.println("Data initialization failed: ");
            }
        }
    }

    public int sBrk(int nbrBytesNeeded) {
        int initialHeapAddress = heapAddress;
        int size = MemoryValueTypes.DOUBLEWORD.getSize();
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
     * @param value     the container for the value
     * @return the value at this address
     */
    public void accessMemory(long address, long offset, BinaryValue value) throws AddressErrorException {
        long tempValue = 0;
        byte [] memoryBlockTable = null;
        int memoryBlockAddress;
        long memoryBlockValue;

        if (address % value.getSize() != 0) {
            throw new AddressErrorException("Load address not aligned", 4, (int)address);
        }

        memoryBlockAddress = getMemorySetup(memoryBlockTable, address, offset);

        //Check if negative Doubleword
        boolean dwNeg = (value.getType() == MemoryValueTypes.DOUBLEWORD &&
                memoryBlockTable[memoryBlockAddress + (value.getSize() - 1)] < 0);

        if (dwNeg) {
            tempValue = Long.MIN_VALUE; //Prevents overflow
        }
        for (int i = 0; i < value.getSize(); i++) {
            memoryBlockValue = memoryBlockTable[memoryBlockAddress + i];
            if (dwNeg && i == value.getSize() - 1) {
                memoryBlockAddress -= 0x80; //Remove the MSB
            }
            memoryBlockValue = memoryBlockValue << (i * 8);
            tempValue += memoryBlockValue;
        }

        value.setValue(tempValue);
    }

    /**
     * Store a value from a specific size at a specif address in memory.
     * The store is made in little-endian.
     *
     * @param value     the value to store
     * @param address   the address where to store the value
     * @param offset    the offset to apply to the address
     */
    public void storeMemory(BinaryValue value, long address, long offset) throws AddressErrorException {
        int memoryBlockAddress;
        byte[] memoryBlockTable = null;

        if (address % value.getSize() != 0) {
            throw new AddressErrorException("Store address not aligned", 4, (int)address);
        }

        memoryBlockAddress = getMemorySetup(memoryBlockTable, address, offset);

        for (int i = 0; i < value.getSize(); i++) {
            memoryBlockTable[memoryBlockAddress + i] = (byte) (value.getValue() >> (i * 8));
        }
    }

    private int getMemorySetup(byte[] memoryBlockTable, long address, long offset) {
        int memoryBlockIndex;

        if (address >= DATA_BASE_ADDRESS && address <= DATA_LIMIT_ADDRESS) {
            memoryBlockTable = dataBlockTable;
            memoryBlockIndex = (int)(address - DATA_BASE_ADDRESS + offset);
        } else if (address <= STACK_BASE_ADDRESS && address >= STACK_LIMIT_ADDRESS) {
            memoryBlockTable = stackBlockTable;
            memoryBlockIndex = (int)(STACK_BASE_ADDRESS - address + offset);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }

        return memoryBlockIndex;
    }
}
