package rars.concolic;

import rars.riscv.hardware.MemoryConfigurations;
import rars.riscv.hardware.AddressErrorException;

public class Memory {
    public static final int DEFAULT_STACK_POINTER = MemoryConfigurations.getDefaultStackPointer() - 4; //Align sp for 64 bits architecture
    public static final int DEFAULT_GLOBAL_POINTER = MemoryConfigurations.getDefaultGlobalPointer();
    public static final int DATA_SIZE = 4194304; //4MB
    public static final int STACK_SIZE = 8192; //8KB

    public static final int DATA_BASE_ADDRESS = MemoryConfigurations.getDefaultDataSegmentBaseAddress();
    public static final int DATA_LIMIT_ADDRESS = DATA_BASE_ADDRESS + DATA_SIZE;
    public static final int HEAP_BASE_ADDRESS = MemoryConfigurations.getDefaultHeapBaseAddress();
    public static final long STACK_BASE_ADDRESS = MemoryConfigurations.getDefaultStackBaseAddress();
    public static final long STACK_LIMIT_ADDRESS = STACK_BASE_ADDRESS - STACK_SIZE;

    public final ConcolicValues.V DEFAULT_VALUE = new ConcolicValues.V(0, new SymbolicLong(0));

    private int heapAddress;

    private ConcolicValues.V[] dataBlockTable;
    private ConcolicValues.V[] stackBlockTable;

    public Memory() { initialize(); }

    private void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        dataBlockTable = new ConcolicValues.V[DATA_SIZE];
        stackBlockTable = new ConcolicValues.V[STACK_SIZE];
        intializeData();
    }

    private void intializeData() {
        rars.riscv.hardware.Memory rarsMemory = rars.riscv.hardware.Memory.getInstance();
        int base = MemoryConfigurations.getDefaultDataBaseAddress();
        MemoryValue memValue = new MemoryValue(MemoryValueTypes.WORD, true);
        ConcolicValues.V address;

        for(int i = 0; i < dataBlockTable.length; i += 4){
            try {
                Integer value = rarsMemory.getRawWordOrNull(base + i);
                if (value == null) {
                    break;
                }
                memValue.setValue(new ConcolicValues.V(value.longValue(), new SymbolicLong(value.longValue())));
                address = new ConcolicValues.V(base + i, new SymbolicLong(base + i));
                storeMemory(memValue, address, DEFAULT_VALUE);
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
    public void accessMemory(MemoryValue value, ConcolicValues.V address, ConcolicValues.V offset) throws AddressErrorException {
        if (!(address.symbolic instanceof  SymbolicLong) || !(offset.symbolic instanceof SymbolicLong)) {
            throw new AddressErrorException("Load address is not a constant", 4, -1);
        }

        if (address.concrete % value.getSize() != 0) {
            throw new AddressErrorException("Load address not aligned", 4, address.concrete.intValue());
        }

        int memoryBlockAddress = getMemoryIndex(address.concrete, offset.concrete);
        ConcolicValues.V[] memoryBlockTable = getMemoryBlock(address.concrete, offset.concrete);
        ConcolicValues.V concValue = new ConcolicValues.V(0, new SymbolicLong(0));

        for (int i = 0; i < value.getSize(); i++) {
            ConcolicValues.V tempVal = memoryBlockTable[memoryBlockAddress + i];
            tempVal = tempVal == null ? DEFAULT_VALUE : value.sll(tempVal, value.inject(i * 8));
            concValue = value.or(concValue, tempVal);
        }
        value.setValue(concValue);
    }

    /**
     * Store a value from a specific size at a specif address in memory.
     * The store is made in little-endian.
     *
     * @param value     the value to store
     * @param address   the address where to store the value
     * @param offset    the offset to apply to the address
     */
    public void storeMemory(MemoryValue value, ConcolicValues.V address, ConcolicValues.V offset) throws AddressErrorException {
        if (!(address.symbolic instanceof  SymbolicLong) || !(offset.symbolic instanceof  SymbolicLong)) {
            throw new AddressErrorException("Store address is not a constant", 4, -1);
        }

        if (address.concrete % value.getSize() != 0) {
            throw new AddressErrorException("Store address not aligned", 4, address.concrete.intValue());
        }

        int memoryBlockAddress = getMemoryIndex(address.concrete, offset.concrete);
        ConcolicValues.V[] memoryBlockTable = getMemoryBlock(address.concrete, offset.concrete);

        for (int i = 0; i < value.getSize(); i++) {
            ConcolicValues.V memVal = value.sra(value.getConcolicValue(), value.inject(i * 8));
            memVal = value.and(memVal, value.inject(0xFF));
            memoryBlockTable[memoryBlockAddress + i] = memVal;
        }
    }

    private int getMemoryIndex(long address, long offset) {
        long realAddress = address + offset;

        if (realAddress >= DATA_BASE_ADDRESS && realAddress <= DATA_LIMIT_ADDRESS) {
            return (int)(realAddress - DATA_BASE_ADDRESS);
        } else if (realAddress <= STACK_BASE_ADDRESS && realAddress >= STACK_LIMIT_ADDRESS) {
            return (int)(realAddress - STACK_LIMIT_ADDRESS);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private ConcolicValues.V[] getMemoryBlock(long address, long offset) {
        long realAddress = address + offset;

        if (realAddress >= DATA_BASE_ADDRESS && realAddress <= DATA_LIMIT_ADDRESS) {
            return dataBlockTable;
        } else if (realAddress <= STACK_BASE_ADDRESS && realAddress >= STACK_LIMIT_ADDRESS) {
            return stackBlockTable;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
