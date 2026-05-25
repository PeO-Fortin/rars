package rars.concolic;

public class Utils {
    /**
     * Apply the correct mask to a value based on the size required.
     *
     * @param value     the value on which the mask has to be applied
     * @param valueType the size required (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the correct mask applied
     */
    public static long maskedValue(long value, MemoryValueTypes valueType) {
        switch (valueType) {
            case BYTE:
                value = value & 0xFF;
                break;
            case HALFWORD:
                value = value & 0xFFFF;
                break;
            case WORD:
                value = value & 0xFFFFFFFFL;
                break;
        }
        return value;
    }

    /**
     * Apply the right sign to an unsigned value.
     *
     * @param value     the unsigned value
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the right sign
     */
    public static long signedValue(long value, MemoryValueTypes valueSize) {
        switch (valueSize) {
            case BYTE:
                return (byte) value;
            case HALFWORD:
                return (short) value;
            case WORD:
                return (int) value;
        }
        return value;
    }
}
