package rars.concolic;

import java.util.function.Function;

public class MemoryValue<V> {
    private V value;
    private MemoryValueTypes type;
    private boolean unsigned;
    private Function<V, Long> toConcreteValue;
    private Function<V, SymbolicValue> toSymbolicValue;

    public MemoryValue(V value) {
        this.value = value;
        this.type = MemoryValueTypes.DOUBLEWORD;
        this.unsigned = false;
    }

    public MemoryValue(MemoryValueTypes type, boolean unsigned) {
        this.type = type;
        this.unsigned = unsigned;
    }

    public MemoryValue(V value, MemoryValueTypes type, boolean unsigned) {
        this.value = value;
        this.type = type;
        this.unsigned = unsigned;
    }

    public Long getConcreteValue() {
        return rawToRealValue().concrete;
    }

    public SymbolicValue getSymbolicValue() {;
        return rawToRealValue().symbolic;
    }

    public ConcolicValues.V getConcolicValue() {
        return rawToRealValue();
    }

    public V getRawValue() {
        return value;
    }

    public MemoryValueTypes getType() {
        return type;
    }

    public int getSize() {
        return type.getSize();
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    private ConcolicValues.V rawToRealValue() {
        ConcolicValues.V realValue = new ConcolicValues.V(toConcreteValue.apply(value),  toSymbolicValue.apply(value));
        realValue = Utils.maskedValue(realValue, type);

        if (!unsigned) {
            realValue = Utils.signedValue(realValue, type);
        }

        return realValue;
    }
}