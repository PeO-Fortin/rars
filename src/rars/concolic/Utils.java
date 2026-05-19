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
                if(value > Byte.MAX_VALUE) {
                    value = Byte.MIN_VALUE + (value - (Byte.MAX_VALUE + 1L));
                }
                break;
            case HALFWORD:
                if(value > Short.MAX_VALUE) {
                    value = Short.MIN_VALUE + (value - (Short.MAX_VALUE + 1L));
                }
                break;
            case WORD:
                if(value > Integer.MAX_VALUE) {
                    value = Integer.MIN_VALUE + (value - (Integer.MAX_VALUE +1L));
                }
                break;
        }
        return value;
    }
}
