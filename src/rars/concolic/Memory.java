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


    public void initialize() {
        heapAddress = HEAP_BASE_ADDRESS;
        stackPointer = DEFAULT_STACK_POINTER;
        dataBlockTable = new byte[MEMORY_SIZE];
        stackBlockTable = new byte[MEMORY_SIZE];
    }

    


}
