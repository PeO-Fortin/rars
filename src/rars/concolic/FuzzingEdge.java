package rars.concolic;

import rars.cfg.*;

import java.util.Objects;

public class FuzzingEdge {
    public BasicBlock from;
    public BasicBlock to;
    public FuzzingEdge(BasicBlock from, BasicBlock to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FuzzingEdge other = (FuzzingEdge) object;
        return from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
