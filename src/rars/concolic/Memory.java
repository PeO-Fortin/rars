package rars.concolic;

import rars.riscv.hardware.MemoryConfigurations;
import rars.riscv.hardware.AddressErrorException;

public class Memory {
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

    private byte[] concreteDataBlockTable;
    private byte[] concreteStackBlockTable;

    public Memory() { initialize(); }

    private void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        concreteDataBlockTable = new byte[DATA_SIZE];
        concreteStackBlockTable = new byte[STACK_SIZE];
        intializeData();
    }

    private void intializeData() {
        rars.riscv.hardware.Memory rarsMemory = rars.riscv.hardware.Memory.getInstance();
        int base = MemoryConfigurations.getDefaultDataBaseAddress();
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        for(int i = 0; i < concreteDataBlockTable.length; i += 4){
            try {
                value.setConcreteValue(rarsMemory.getRawWordOrNull(base + i));
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
     * @param value     the container for the value
     * @param address   the address of the memory block
     * @param offset    the offset to apply to the address
     * @return the value at this address
     */
    public void accessMemory(MemoryValue value, long address, long offset) throws AddressErrorException {
        long tempValue = 0;
        byte [] memoryBlockTable = null;
        int memoryBlockAddress;
        long memoryBlockValue;

        if (address % value.getSize() != 0) {
            throw new AddressErrorException("Load address not aligned", 4, (int)address);
        }

        memoryBlockAddress = getMemoryIndex(address, offset);
        memoryBlockTable = getMemoryBlock(address, offset);

        for (int i = 0; i < value.getSize(); i++) {
            memoryBlockValue = memoryBlockTable[memoryBlockAddress + i];
            memoryBlockValue = memoryBlockValue << (i * 8);
            tempValue += memoryBlockValue;
        }

        value.setConcreteValue(tempValue);
    }

    /**
     * Store a value from a specific size at a specif address in memory.
     * The store is made in little-endian.
     *
     * @param value     the value to store
     * @param address   the address where to store the value
     * @param offset    the offset to apply to the address
     */
    public void storeMemory(MemoryValue value, long address, long offset) throws AddressErrorException {
        int memoryBlockAddress;
        byte[] memoryBlockTable = null;

        if (address % value.getSize() != 0) {
            throw new AddressErrorException("Store address not aligned", 4, (int)address);
        }

        memoryBlockAddress = getMemoryIndex(address, offset);
        memoryBlockTable = getMemoryBlock(address, offset);

        for (int i = 0; i < value.getSize(); i++) {
            memoryBlockTable[memoryBlockAddress + i] = (byte) (value.getConcreteValue() >> (i * 8));
        }
    }

    private int getMemoryIndex(long address, long offset) {
        long realAddress = address + offset;

        if (realAddress >= DATA_BASE_ADDRESS && realAddress <= DATA_LIMIT_ADDRESS) {
            return (int)(realAddress - DATA_BASE_ADDRESS);
        } else if (realAddress <= STACK_BASE_ADDRESS && realAddress >= STACK_LIMIT_ADDRESS) {
            return (int)(STACK_BASE_ADDRESS - realAddress);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private byte[] getMemoryBlock(long address, long offset) {
        long realAddress = address + offset;

        if (realAddress >= DATA_BASE_ADDRESS && realAddress <= DATA_LIMIT_ADDRESS) {
            return concreteDataBlockTable;
        } else if (realAddress <= STACK_BASE_ADDRESS && realAddress >= STACK_LIMIT_ADDRESS) {
            return concreteStackBlockTable;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
