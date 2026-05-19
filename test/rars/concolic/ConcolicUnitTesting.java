package rars.concolic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConcreteMemoryUnitTesting {

    ConcreteMemory memory;
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream()

//******************************************************
//   SETUP
//******************************************************
    @BeforeEach
    void setup() {
        memory = new ConcreteMemory();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

//******************************************************
//   TESTS BINARY VALUE
//******************************************************
    @Test
    public void testBVRawValuePositiveInt() throws Exception{
        int storedValue = 123456;
        long rawValue;

        BinaryValue value = new BinaryValue(storedValue,MemoryValueTypes.WORD,true);
        rawValue = value.getRawValue();

        assertEquals(storedValue,rawValue);
    }

    @Test
    public void testBVRawValueNegativeInt() throws Exception{
        int storedValue = -123456;
        long rawValue;

        BinaryValue value = new BinaryValue(storedValue,MemoryValueTypes.WORD,false);
        rawValue = value.getRawValue();

        assertEquals(storedValue,rawValue);
    }

    @Test
    public void testBVRawValueOverflowInt() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;
        long rawValue;

        BinaryValue value = new BinaryValue(storedValue,MemoryValueTypes.WORD,false);
        rawValue = value.getRawValue();

        assertTrue(rawValue > 0);
    }

    @Test
    public void testBVValueOverflowIntSigned() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;
        long realValue;

        BinaryValue value = new BinaryValue(storedValue,MemoryValueTypes.WORD,false);

        assertEquals(Integer.MIN_VALUE, value.getValue());
    }

    @Test
    public void testBVValueOverflowIntUnsigned() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;

        BinaryValue value = new BinaryValue(storedValue,MemoryValueTypes.WORD,true);


        assertTrue(value.getValue() > Integer.MAX_VALUE);
    }

    @Test
    public void testBVGetType() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        BinaryValue bValue = new BinaryValue(value,storedType,unsigned);

        assertEquals(storedType,bValue.getType());
    }

    @Test
    public void testBVGetSize() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        BinaryValue bValue = new BinaryValue(value,storedType,unsigned);

        assertEquals(MemoryValueTypes.WORD.getSize(),bValue.getSize());
    }

    @Test
    public void testBVIsUnsigned() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        BinaryValue bValue = new BinaryValue(value,storedType,unsigned);

        assertEquals(unsigned,bValue.isUnsigned());
    }

    @Test
    public void testBVGetType() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        BinaryValue bValue = new BinaryValue(value,storedType,unsigned);

        assertEquals(storedType,bValue.getType());
    }

    @Test
    public void testBVSetValue() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        long newValue = 21;

        BinaryValue bValue = new BinaryValue(originalValue,storedType,unsigned);
        bValue.setValue(newValue);

        assertEquals(newValue,bValue.getValue());
    }

    @Test
    public void testBVSetType() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        MemoryValueTypes newType = MemoryValueTypes.HALFWORD;

        BinaryValue bValue = new BinaryValue(originalValue,storedType,unsigned);

        bValue.setType(newType);
        assertEquals(newType,bValue.getType());
    }

    @Test
    public void testBVSetSize() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        int newSize = 8;

        BinaryValue bValue = new BinaryValue(originalValue,storedType,unsigned);
        bValue.setSize(newSize);
        assertEquals(newSize,bValue.getSize());
    }

    @Test
    public void testBVSetWrongSize() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        int newSize = 5;

        BinaryValue bValue = new BinaryValue(originalValue,storedType,unsigned);
        bValue.setSize(newSize);

        assertEquals("Error: Invalid value size",outputStreamCaptor.toString().trim());
    }

    @Test
    public void testBVSetUnisgned() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        boolean newValue = false;

        BinaryValue bValue = new BinaryValue(originalValue,storedType,unsigned);
        bValue.setUnsigned(newValue);

        assertEquals(newValue,bValue.isUnsigned());
    }

//******************************************************
//   TESTS MEMORY STORAGE
//******************************************************
    @Test
    public void testStoreMemoryDataByte () throws Exception {
        BinaryValue value = new BinaryValue(0xAB, MemoryValueTypes.BYTE, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[ConcreteMemory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xAB;

        Field dataBlockTableAttribute = ConcreteMemory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryHalfword () throws Exception {
        BinaryValue value = new BinaryValue(0xABCD, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[ConcreteMemory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xCD;
        referenceDataBlockTable[65545] = 0xAB;

        Field dataBlockTableAttribute = ConcreteMemory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryWord () throws Exception {
        BinaryValue value = new BinaryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[ConcreteMemory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xEF;
        referenceDataBlockTable[65545] = 0xCD;
        referenceDataBlockTable[65546] = 0xAB;
        referenceDataBlockTable[65547] = 0x89;

        Field dataBlockTableAttribute = ConcreteMemory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryDoubleword () throws Exception {
        BinaryValue value = new BinaryValue(0x0123456789ABCDEF, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[ConcreteMemory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xEF;
        referenceDataBlockTable[65545] = 0xCD;
        referenceDataBlockTable[65546] = 0xAB;
        referenceDataBlockTable[65547] = 0x89;
        referenceDataBlockTable[65548] = 0x67;
        referenceDataBlockTable[65549] = 0x45;
        referenceDataBlockTable[65550] = 0x23;
        referenceDataBlockTable[65551] = 0x01;

        Field dataBlockTableAttribute = ConcreteMemory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryHalfword () throws Exception {
        BinaryValue value = new BinaryValue(0x0123456789ABCDEF, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[ConcreteMemory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xEF;
        referenceDataBlockTable[65545] = 0xCD;
        referenceDataBlockTable[65546] = 0xAB;
        referenceDataBlockTable[65547] = 0x89;
        referenceDataBlockTable[65548] = 0x67;
        referenceDataBlockTable[65549] = 0x45;
        referenceDataBlockTable[65550] = 0x23;
        referenceDataBlockTable[65551] = 0x01;

        Field dataBlockTableAttribute = ConcreteMemory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

//******************************************************
//   TESTS MEMORY ACCESS
//******************************************************
    @Test
    public void testAccessMemoryByte () throws Exception {
        BinaryValue storeValue = new BinaryValue(0xAB, MemoryValueTypes.BYTE, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        BinaryValue accessValue = new BinaryValue(MemoryValueTypes.BYTE, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storeValue.getValue(), accessValue.getValue());
    }

    @Test
    public void testAccessMemoryHalfword () throws Exception {
        BinaryValue storeValue = new BinaryValue(0xAB, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        BinaryValue accessValue = new BinaryValue(MemoryValueTypes.HALFWORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storeValue.getValue(), accessValue.getValue());
    }

    @Test
    public void testAccessMemoryWord () throws Exception {
        BinaryValue storeValue = new BinaryValue(0xAB, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        BinaryValue accessValue = new BinaryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storeValue.getValue(), accessValue.getValue());
    }

    @Test
    public void testAccessMemoryDoubleword () throws Exception {
        BinaryValue storeValue = new BinaryValue(0xAB, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        BinaryValue accessValue = new BinaryValue(MemoryValueTypes.DOUBLEWORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storeValue.getValue(), accessValue.getValue());
    }

//******************************************************
//   TEAR DOWN
//******************************************************

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }
}
