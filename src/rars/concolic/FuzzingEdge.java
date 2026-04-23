package rars.concolic;

import java.util.Objects;

public class FuzzingEdge {
    public int from;
    public int to;
    public FuzzingEdge(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FuzzingEdge other = (FuzzingEdge) object;
        return from == other.from && to == other.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
