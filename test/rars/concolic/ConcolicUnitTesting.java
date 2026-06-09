package rars.concolic;

import rars.riscv.hardware.AddressErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConcolicUnitTesting {

    Memory memory;
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

//******************************************************
//   SETUP
//******************************************************
    @BeforeEach
    void setup() {
        memory = new Memory();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

//******************************************************
//   TESTS MEMORY VALUE - Builders
//******************************************************
    @Test
    public void testMVRawValuePositiveByte() {
        long storedValue = 12L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.BYTE, true);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValueNegativeByte() {
        long storedValue = -12L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.BYTE, false);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValuePositiveHalfword() {
        long storedValue = 543L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.HALFWORD, true);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValueNegativeHalfword() {
        long storedValue = -543L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.HALFWORD, false);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValuePositiveWord() {
        long storedValue = 123456L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.WORD, true);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValueNegativeWord() {
        long storedValue = -123456L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.WORD, false);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValuePositiveDoubleword() {
        long storedValue = 123456789L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.DOUBLEWORD, true);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVRawValueNegativeDoubleword() {
        long storedValue = -123456789L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.DOUBLEWORD, false);
        assertEquals(storedValue, value.getRawValue().concrete);
    }

    @Test
    public void testMVConstructorFromV() {
        ConcolicValues.V v = new ConcolicValues.V(42L, new SymbolicLong(42L));
        MemoryValue value = new MemoryValue(v);
        assertEquals(v, value.getRawValue());
    }

    @Test
    public void testMVNoValueBuilder() {
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        assertDoesNotThrow(value::getConcreteValue);
    }

//******************************************************
//   TESTS MEMORY VALUE - Concrete Values
//******************************************************
    @Test
    public void testOverflowSigned() {
        MemoryValue value = new MemoryValue(128L, MemoryValueTypes.BYTE, false);
        assertEquals((long) Byte.MIN_VALUE, value.getConcreteValue());
    }

    @Test
    public void testMVValueOverflowIntUnsigned() {
        long storedValue = Integer.MAX_VALUE + 1L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.WORD, true);
        assertTrue(value.getConcreteValue() > Integer.MAX_VALUE);
    }

    @Test
    public void testMVValueOverflowIntSigned() {
        long storedValue = Integer.MAX_VALUE + 1L;
        MemoryValue value = new MemoryValue(storedValue, MemoryValueTypes.WORD, false);
        assertEquals((long) Integer.MIN_VALUE, value.getConcreteValue());
    }

    @Test
    public void testMVUnsignedByteMax() {
        MemoryValue value = new MemoryValue(0xFFL, MemoryValueTypes.BYTE, true);
        assertEquals(255L, value.getConcreteValue());
    }

    @Test
    public void testMVSignedByteMax() {
        MemoryValue value = new MemoryValue(0xFFL, MemoryValueTypes.BYTE, false);
        assertEquals(-1L, value.getConcreteValue());
    }

//******************************************************
//   TESTS MEMORY VALUE - Getters - Other parameters
//******************************************************
    @Test
    public void testMVGetType() {
        MemoryValue mv = new MemoryValue(42L, MemoryValueTypes.WORD, true);
        assertEquals(MemoryValueTypes.WORD, mv.getType());
    }

    @Test
    public void testMVGetSize() {
        MemoryValue mv = new MemoryValue(42L, MemoryValueTypes.WORD, true);
        assertEquals(MemoryValueTypes.WORD.getSize(), mv.getSize());
    }

    @Test
    public void testMVIsUnsignedTrue() {
        MemoryValue mv = new MemoryValue(42L, MemoryValueTypes.WORD, true);
        assertTrue(mv.isUnsigned());
    }

    @Test
    public void testMVIsUnsignedFalse() {
        MemoryValue mv = new MemoryValue(42L, MemoryValueTypes.WORD, false);
        assertFalse(mv.isUnsigned());
    }

//******************************************************
//   TESTS MEMORY VALUE — setValue
//******************************************************
    @Test
    public void testMVSetValue() {
        MemoryValue mv = new MemoryValue(42L, MemoryValueTypes.WORD, true);
        ConcolicValues.V newV = new ConcolicValues.V(21L, new SymbolicLong(21L));
        mv.setValue(newV);
        assertEquals(21L, mv.getConcreteValue());
    }

    @Test
    public void testMVSetValueNegative() {
        MemoryValue mv = new MemoryValue(0L, MemoryValueTypes.DOUBLEWORD, false);
        ConcolicValues.V newV = new ConcolicValues.V(-99L, new SymbolicLong(-99L));
        mv.setValue(newV);
        assertEquals(newV, mv.getRawValue());
    }

//******************************************************
//   TESTS MEMORY VALUE — getConcolicValue & getSymbolicValue
//******************************************************
    @Test
    public void testMVGetConcolicValueConsistency() {
        MemoryValue mv = new MemoryValue(100L, MemoryValueTypes.WORD, true);
        ConcolicValues.V cv = mv.getConcolicValue();
        assertEquals(mv.getConcreteValue(), cv.concrete);
    }

    @Test
    public void testMVGetSymbolicValueNotNull() {
        MemoryValue mv = new MemoryValue(100L, MemoryValueTypes.WORD, true);
        assertNotNull(mv.getSymbolicValue());
    }

//******************************************************
//   TESTS MEMORY STORAGE
//******************************************************
    @Test
    public void testStoreMemoryDataByte() throws Exception {
        MemoryValue value = new MemoryValue(0xABL, MemoryValueTypes.BYTE, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65544; // 0x10010008 - DATA_BASE_ADDRESS
        assertEquals(0xABL, table[base].concrete & 0xFFL);
    }

    @Test
    public void testStoreMemoryHalfword() throws Exception {
        MemoryValue value = new MemoryValue(0xABCDL, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65544;
        assertEquals(0xCDL, table[base].concrete & 0xFFL);
        assertEquals(0xABL, table[base + 1].concrete & 0xFFL);
    }

    @Test
    public void testStoreMemoryWord() throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65544;
        assertEquals(0xEFL, table[base].concrete & 0xFFL);
        assertEquals(0xCDL, table[base + 1].concrete & 0xFFL);
        assertEquals(0xABL, table[base + 2].concrete & 0xFFL);
        assertEquals(0x89L, table[base + 3].concrete & 0xFFL);
    }

    @Test
    public void testStoreMemoryDoubleword() throws Exception {
        MemoryValue value = new MemoryValue(0x0123456789ABCDEFL, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65544;
        long[] expected = {0xEF, 0xCD, 0xAB, 0x89, 0x67, 0x45, 0x23, 0x01};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], table[base + i].concrete & 0xFFL,
                    "Octet " + i + " incorrect");
        }
    }

    @Test
    public void testStoreWithPositiveOffset() throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x10010008L;
        long offset = 4L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(offset), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65548; // 0x10010008 + 4 - DATA_BASE_ADDRESS
        assertEquals(0xEFL, table[base].concrete & 0xFFL);
        assertEquals(0xCDL, table[base + 1].concrete & 0xFFL);
        assertEquals(0xABL, table[base + 2].concrete & 0xFFL);
        assertEquals(0x89L, table[base + 3].concrete & 0xFFL);
    }

    @Test
    public void testStoreWithNegativeOffset() throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x10010010L;
        long offset = -8L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(offset), op.inject(address));

        Field field = Memory.class.getDeclaredField("dataBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        int base = 65544; // 0x10010010 - 8 - DATA_BASE_ADDRESS
        assertEquals(0xEFL, table[base].concrete & 0xFFL);
        assertEquals(0xCDL, table[base + 1].concrete & 0xFFL);
        assertEquals(0xABL, table[base + 2].concrete & 0xFFL);
        assertEquals(0x89L, table[base + 3].concrete & 0xFFL);
    }

    @Test
    public void testStoreStack() throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("stackBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        assertEquals(0xEFL, table[0].concrete & 0xFFL);
        assertEquals(0xCDL, table[1].concrete & 0xFFL);
        assertEquals(0xABL, table[2].concrete & 0xFFL);
        assertEquals(0x89L, table[3].concrete & 0xFFL);
    }

//******************************************************
//   TESTS MEMORY ACCESS
//******************************************************
    @Test
    public void testAccessMemoryByte() throws Exception {
        MemoryValue stored = new MemoryValue(0xABL, MemoryValueTypes.BYTE, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.BYTE, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testAccessMemoryHalfword() throws Exception {
        MemoryValue stored = new MemoryValue(0xABCDL, MemoryValueTypes.HALFWORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.HALFWORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testAccessMemoryWord() throws Exception {
        MemoryValue stored = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testAccessMemoryDoubleword() throws Exception {
        MemoryValue stored = new MemoryValue(0x0123456789ABCDEFL, MemoryValueTypes.DOUBLEWORD, true);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.DOUBLEWORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testAccessMemoryStack() throws Exception {
        MemoryValue stored = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

//******************************************************
//   TESTS OFFSET
//******************************************************
    @Test
    public void testStoreAndAccessWithPositiveOffset() throws Exception {
        MemoryValue stored = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x10010008L;
        long offset = 4L;

        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(offset), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(offset), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testStoreAndAccessWithNegativeOffset() throws Exception {
        MemoryValue stored = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        // adresse + offset = 0x10010005 → pas aligné sur 4, on choisit -8
        long address = 0x10010010L;
        long offset = -8L;

        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(offset), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(offset), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

//******************************************************
//   TESTS DEFAULT MEMORY
//******************************************************
    @Test
    public void testDefaultMemoryIsZero() throws Exception {
        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        ConcolicValues op = new ConcolicValues();
        memory.accessMemory(read, op.inject(Memory.DATA_BASE_ADDRESS + 1024), op.inject(0));
        assertEquals(0L, read.getConcreteValue());
    }

//******************************************************
//   TESTS SIGNED VALUES
//******************************************************
    @Test
    public void testAccessMemorySignedByte() throws Exception {
        MemoryValue stored = new MemoryValue(0xFFL, MemoryValueTypes.BYTE, false);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.BYTE, false);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(-1L, read.getConcreteValue());
    }

    @Test
    public void testAccessMemorySignedHalfword() throws Exception {
        MemoryValue stored = new MemoryValue(0xFFFFL, MemoryValueTypes.HALFWORD, false);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.HALFWORD, false);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(-1L, read.getConcreteValue());
    }

    @Test
    public void testAccessMemorySignedWord() throws Exception {
        MemoryValue stored = new MemoryValue(0xFFFFFFFFL, MemoryValueTypes.WORD, false);
        long address = 0x10010008L;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, false);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(-1L, read.getConcreteValue());
    }

//******************************************************
//   TESTS STACK
//******************************************************
    @Test
    public void testStoreAndAccessStack() throws Exception {
        MemoryValue stored = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(stored, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(stored.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testStoreStackLittleEndian() throws Exception {
        MemoryValue value = new MemoryValue(0x89ABCDEFL, MemoryValueTypes.WORD, true);
        long address = 0x7FFFDFFC;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        Field field = Memory.class.getDeclaredField("stackBlockTable");
        field.setAccessible(true);
        ConcolicValues.V[] table = (ConcolicValues.V[]) field.get(memory);

        // index 0 correspond à STACK_BASE_ADDRESS - address
        assertEquals(0xEFL, table[0].concrete & 0xFFL);
        assertEquals(0xCDL, table[1].concrete & 0xFFL);
        assertEquals(0xABL, table[2].concrete & 0xFFL);
        assertEquals(0x89L, table[3].concrete & 0xFFL);
    }

//******************************************************
//   TESTS MEMORY LIMITS
//******************************************************
    @Test
    public void testStoreAndAccessDataLowerBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42L, MemoryValueTypes.WORD, true);
        long address = Memory.DATA_BASE_ADDRESS;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(value.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testStoreAndAccessStackUpperBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42L, MemoryValueTypes.WORD, true);
        long address = Memory.DEFAULT_STACK_POINTER;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(value.getConcreteValue(), read.getConcreteValue());
    }

    @Test
    public void testStoreAndAccessStackLowerBound() throws Exception {
        MemoryValue value = new MemoryValue(0x42L, MemoryValueTypes.WORD, true);
        long address = Memory.STACK_LIMIT_ADDRESS;
        ConcolicValues op = new ConcolicValues();
        memory.storeMemory(value, op.inject(0), op.inject(address));

        MemoryValue read = new MemoryValue(MemoryValueTypes.WORD, true);
        memory.accessMemory(read, op.inject(0), op.inject(address));

        assertEquals(value.getConcreteValue(), read.getConcreteValue());
    }

//******************************************************
//   TESTS ERRORS
//******************************************************
    @Test
    public void testStoreAddressNotAligned() {
        MemoryValue value = new MemoryValue(0x42L, MemoryValueTypes.WORD, true);
        long address = 0x10010001L; // not aligned on 4 bytes
        ConcolicValues op = new ConcolicValues();
        assertThrows(AddressErrorException.class, () ->
            memory.storeMemory(value, op.inject(0), op.inject(address)));
    }

    @Test
    public void testAccessAddressNotAligned() {
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        long address = 0x10010001L; // not aligned on 4 bytes
        ConcolicValues op = new ConcolicValues();
        assertThrows(AddressErrorException.class, () ->
                memory.accessMemory(value, op.inject(0), op.inject(address)));
    }

    @Test
    public void testStoreOutsideSegments() {
        MemoryValue value = new MemoryValue(0x42L, MemoryValueTypes.WORD, true);
        long address = 0x00400000L; // segment .text — invalid
        ConcolicValues op = new ConcolicValues();
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                memory.storeMemory(value, op.inject(0), op.inject(address)));
    }

    @Test
    public void testAccessOutsideSegments() {
        MemoryValue value = new MemoryValue(MemoryValueTypes.WORD, true);
        long address = 0x00400000L; // segment .text
        ConcolicValues op = new ConcolicValues();
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                memory.accessMemory(value, op.inject(0), op.inject(address)));
    }

    @Test
    public void testSBrkNegative() {
        assertThrows(IllegalArgumentException.class, () -> memory.sBrk(-1));
    }

    @Test
    public void testSBrkZero() {
        int addr1 = memory.sBrk(0);
        int addr2 = memory.sBrk(0);
        assertEquals(addr1, addr2);
    }

    @Test
    public void testSBrkAlignment() {
        memory.sBrk(1); // force un décalage non aligné
        int addr = memory.sBrk(0);
        int size = MemoryValueTypes.DOUBLEWORD.getSize();
        assertEquals(0, addr % size, "L'adresse heap doit être alignée sur DOUBLEWORD");
    }

    @Test
    public void testSBrkReturnsInitialAddress() {
        int before = memory.sBrk(0);
        int returned = memory.sBrk(16);
        assertEquals(before, returned);
    }

//******************************************************
//   TEAR DOWN
//******************************************************

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }
}