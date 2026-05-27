package rars.concolic;

public class Utils {
    /**
     * Apply the correct mask to a concolic value based on the size required.
     *
     * @param value     the value on which the mask has to be applied
     * @param valueType the size required (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the correct mask applied
     */
    public static ConcolicValues.V maskedValue(ConcolicValues.V value, MemoryValueTypes valueType) {
        ConcolicValues op = new ConcolicValues();
        switch (valueType) {
            case BYTE:
                value = op.and(value, op.inject(0xFF));
                break;
            case HALFWORD:
                value = op.and(value, op.inject(0xFFFF));
                break;
            case WORD:
                value = op.and(value, op.inject(0xFFFFFFFFL));
                break;
        }
        return value;
    }

    /**
     * Apply the right sign to an unsigned concolic value.
     *
     * @param value     the unsigned value
     * @param valueSize the size of the value (BYTE, HALFWORD, WORD, DOUBLEWORD)
     * @return the value with the right sign
     */
    public static ConcolicValues.V signedValue(ConcolicValues.V value, MemoryValueTypes valueSize) {
        ConcolicValues op = new ConcolicValues();
        switch (valueSize) {
            case BYTE:
                if(value.concrete > Byte.MAX_VALUE) {
                    value = op.sub(value, op.inject(Byte.MAX_VALUE + 1L));
                    value = op.add(value, op.inject(Byte.MIN_VALUE));
                }
                break;
            case HALFWORD:
                if(value.concrete > Short.MAX_VALUE) {
                    value = op.sub(value, op.inject(Short.MAX_VALUE + 1L));
                    value = op.add(value, op.inject(Short.MIN_VALUE));
                }
                break;
            case WORD:
                if(value.concrete > Integer.MAX_VALUE) {
                    value = op.sub(value, op.inject(Integer.MAX_VALUE + 1L));
                    value = op.add(value, op.inject(Integer.MIN_VALUE));
                }
                break;
        }
        return value;


    }
}
