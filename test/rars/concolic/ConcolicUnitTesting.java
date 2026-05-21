package rars.concolic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConcreteMemoryUnitTesting {

    Memory memory;
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream()

//******************************************************
//   SETUP
//******************************************************
    @BeforeEach
    void setup() {
        memory = new Memory();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

//******************************************************
//   TESTS MEMORY VALUE
//******************************************************
    @Test
    public void testOverflowSigned() throws Exception{
        int storedValue = 128;

        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.BYTE, false);

        assertEquals(Byte.MIN_VALUE, value.getConcreteValue())
    }
    
    @Test
    public void testMVRawValuePositiveByte() throws Exception{
        byte storedValue = 12;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.BYTE,true);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValueNegativeByte() throws Exception{
        byte storedValue = -12;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.BYTE,false);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValuePositiveWord() throws Exception{
        short storedValue = 543;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,true);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValueNegativeWord() throws Exception{
        short storedValue = -543;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,false);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValuePositiveWord() throws Exception{
        int storedValue = 123456;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,true);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValueNegativeWord() throws Exception{
        int storedValue = -123456;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,false);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValuePositiveDoubleword() throws Exception{
        long storedValue = 123456789;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.DOUBLEWORD,true);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValueNegativeDoubleword() throws Exception{
        long storedValue = -123456789;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.DOUBLEWORD,false);

        assertEquals(storedValue,value.getRawValue());
    }

    @Test
    public void testMVRawValueOverflowInt() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,false);

        assertTrue(value.getRawValue() > 0);
    }

    @Test
    public void testMVValueOverflowIntSigned() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,false);

        assertEquals(Integer.MIN_VALUE, value.getConcreteValue());
    }

    @Test
    public void testMVValueOverflowIntUnsigned() throws Exception{
        long storedValue = Integer.MAX_VALUE + 1L;

        MemoryValue value = new MemoryValue(storedValue,MemoryValueTypes.WORD,true);

        assertTrue(value.getConcreteValue() > Integer.MAX_VALUE);
    }

    @Test
    public void testMVGetType() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        MemoryValue MValue = new MemoryValue(value,storedType,unsigned);

        assertEquals(storedType,MValue.getType());
    }

    @Test
    public void testMVGetSize() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        MemoryValue MValue = new MemoryValue(value,storedType,unsigned);

        assertEquals(MemoryValueTypes.WORD.getSize(),MValue.getSize());
    }

    @Test
    public void testMVIsUnsigned() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        MemoryValue MValue = new MemoryValue(value,storedType,unsigned);

        assertEquals(unsigned,MValue.isUnsigned());
    }

    @Test
    public void testMVGetType() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;

        MemoryValue MValue = new MemoryValue(value,storedType,unsigned);

        assertEquals(storedType,MValue.getType());
    }

    @Test
    public void testMVSetValue() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        long newValue = 21;

        MemoryValue MValue = new MemoryValue(originalValue,storedType,unsigned);
        MValue.setConcreteValue(newValue);

        assertEquals(newValue,MValue.getConcreteValue());
    }

    @Test
    public void testMVSetType() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        MemoryValueTypes newType = MemoryValueTypes.HALFWORD;

        MemoryValue MValue = new MemoryValue(originalValue,storedType,unsigned);

        MValue.setType(newType);
        assertEquals(newType,MValue.getType());
    }

    @Test
    public void testMVSetSize() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        int newSize = 8;

        MemoryValue MValue = new MemoryValue(originalValue,storedType,unsigned);
        MValue.setSize(newSize);
        assertEquals(newSize,MValue.getSize());
    }

    @Test
    public void testMVSetWrongSize() throws Exception{
        long originalValue = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        int newSize = 5;

        MemoryValue MValue = new MemoryValue(originalValue,storedType,unsigned);
        MValue.setSize(newSize);

        assertEquals("Error: Invalid value size",outputStreamCaptor.toString().trim());
    }

    @Test
    public void testMVSetUnisgned() throws Exception{
        long value = 42;
        MemoryValueTypes storedType = MemoryValueTypes.WORD;
        boolean unsigned = true;
        boolean newValue = false;

        MemoryValue MValue = new MemoryValue(originalValue,storedType,unsigned);
        MValue.setUnsigned(newValue);

        assertEquals(newValue,MValue.isUnsigned());
    }

    @Test
    public void testMVNoValueBuilder() throws Exception{
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD,true);

        assertEquals(0,value.getConcreteValue());
    }

//******************************************************
//   TESTS MEMORY STORAGE
//******************************************************
    @Test
    public void testStoreMemoryDataByte () throws Exception {
        MemoryValue value = new MemoryValue(0xAB, MemoryValueTypes.BYTE, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xAB;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryHalfword () throws Exception {
        MemoryValue value = new MemoryValue(0xABCD, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xCD;
        referenceDataBlockTable[65545] = 0xAB;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryWord () throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xEF;
        referenceDataBlockTable[65545] = 0xCD;
        referenceDataBlockTable[65546] = 0xAB;
        referenceDataBlockTable[65547] = 0x89;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreMemoryDoubleword () throws Exception {
        MemoryValue value = new MemoryValue(0x0123456789ABCDEF, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65544] = 0xEF;
        referenceDataBlockTable[65545] = 0xCD;
        referenceDataBlockTable[65546] = 0xAB;
        referenceDataBlockTable[65547] = 0x89;
        referenceDataBlockTable[65548] = 0x67;
        referenceDataBlockTable[65549] = 0x45;
        referenceDataBlockTable[65550] = 0x23;
        referenceDataBlockTable[65551] = 0x01;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreWithPositiveOffset () throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 3;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65547] = 0xEF;
        referenceDataBlockTable[65548] = 0xCD;
        referenceDataBlockTable[65549] = 0xAB;
        referenceDataBlockTable[65550] = 0x89;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

    @Test
    public void testStoreWithNegativeOffset () throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = -3;

        memory.storeMemory(value, address, offset);

        byte[] referenceDataBlockTable = new byte[Memory.DATA_SIZE];
        referenceDataBlockTable[65541] = 0xEF;
        referenceDataBlockTable[65542] = 0xCD;
        referenceDataBlockTable[65543] = 0xAB;
        referenceDataBlockTable[65544] = 0x89;

        Field dataBlockTableAttribute = Memory.class.getDeclaredField("dataBlockTable");
        dataBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceDataBlockTable, (byte[]) dataBlockTableAttribute.get(memory));
    }

//******************************************************
//   TESTS MEMORY ACCESS
//******************************************************
    @Test
    public void testAccessMemoryByte () throws Exception {
        MemoryValue storedValue = new MemoryValue(0xAB, MemoryValueTypes.BYTE, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.BYTE, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testAccessMemoryHalfword () throws Exception {
        MemoryValue storedValue = new MemoryValue(0xAB, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.HALFWORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testAccessMemoryWord () throws Exception {
        MemoryValue storedValue = new MemoryValue(0xAB, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testAccessMemoryDoubleword () throws Exception {
        MemoryValue storedValue = new MemoryValue(0xAB, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.DOUBLEWORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testStoreAndAccessWithOffset() throws Exception {
        MemoryValue storedValue = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x10010008;
        long offset = 4;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testDefaultMemoryIsZero() throws Exception {
        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, Memory.DATA_BASE_ADDRESS + 1024, 0);

        assertEquals(0, accessValue.getConcreteValue());
    }

//******************************************************
//   TESTS SIGNED VALUES
//******************************************************
    @Test
    public void testAccessMemorySignedByte() throws Exception {
        MemoryValue storedValue = new MemoryValue(0xFF, MemoryValueTypes.BYTE, false);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.BYTE, false);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(-1, accessValue.getConcreteValue());
    }

    @Test
    public void testAccessMemorySignedHalfword() throws Exception {
        MemoryValue storedValue = new MemoryValue(0xFFFF, MemoryValueTypes.HALFWORD, false);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.HALFWORD, false);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(-1, accessValue.getConcreteValue());
    }

    @Test
    public void testAccessMemorySignedWord() throws Exception {
        MemoryValue storedValue = new MemoryValue(0xFFFFFFFFL, MemoryValueTypes.WORD, false);
        long address = 0x10010008;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, false);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(-1, accessValue.getConcreteValue());
    }

//******************************************************
//   TESTS STACK
//******************************************************
    @Test
    public void testStoreStack () throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        byte[] referenceStackBlockTable = new byte[Memory.STACK_SIZE];
        referenceStackBlockTable[0] = 0xEF;
        referenceStackBlockTable[1] = 0xCD;
        referenceStackBlockTable[2] = 0xAB;
        referenceStackBlockTable[3] = 0x89;

        Field stackBlockTableAttribute = Memory.class.getDeclaredField("stackBlockTable");
        stackBlockTableAttribute.setAccessible(true);
        assertArrayEquals(referenceStackBlockTable, (byte[]) stackBlockTableAttribute.get(memory));
    }

    @Test
    public void testAccessMemoryStack() throws Exception {
        MemoryValue storedValue = new MemoryValue(0x89ABCDEF, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        long offset = 0;

        memory.storeMemory(storedValue, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(storedValue.getConcreteValue(), accessValue.getConcreteValue());
    }

//******************************************************
//   TESTS MEMORY LIMITS
//******************************************************
    @Test
    public void testStoreMemoryDataLowerBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42, MemoryValueTypes.WORD, true);
        long address = Memory.DATA_BASE_ADDRESS;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(value.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testStoreMemoryStackUpperBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42, MemoryValueTypes.WORD, true);
        long address = Memory.STACK_BASE_ADDRESS;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(value.getConcreteValue(), accessValue.getConcreteValue());
    }

    @Test
    public void testStoreMemoryStackLowerBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42, MemoryValueTypes.WORD, true);
        long address = Memory.STACK_LIMIT_ADDRESS;
        long offset = 0;

        memory.storeMemory(value, address, offset);

        MemoryValue accessValue = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(accessValue, address, offset);

        assertEquals(value.getConcreteValue(), accessValue.getConcreteValue());
    }

//******************************************************
//   TESTS ERRORS
//******************************************************
    @Test
    public void testStoreAddressNotAligned() {
        MemoryValue value = new MemoryValue(0x42, MemoryValueTypes.WORD, true);
        long address = 0x10010001; // non aligné sur 4 octets
        long offset = 0;

        assertThrows(AddressErrorException.class, () -> {
            memory.storeMemory(value, address, offset);
        });
    }

    @Test
    public void testAccessAddressNotAligned() {
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        long address = 0x10010001; // non aligné sur 4 octets
        long offset = 0;

        assertThrows(AddressErrorException.class, () -> {
            memory.accessMemory(value, address, offset);
        });
    }

    @Test
    public void testStoreOutsideSegments() {
        MemoryValue value = new MemoryValue(0x42, MemoryValueTypes.WORD, true);
        long address = 0x00400000; // segment .text — invalide pour store
        long offset = 0;

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            memory.storeMemory(value, address, offset);
        });
    }

    @Test
    public void testAccessOutsideSegments() {
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        long address = 0x00400000; // segment .text
        long offset = 0;

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            memory.accessMemory(value, address, offset);
        });
    }

//******************************************************
//   TEAR DOWN
//******************************************************

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }
}