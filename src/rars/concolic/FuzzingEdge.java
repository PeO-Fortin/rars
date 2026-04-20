package rars.concolic;

import minic.ir.*;

import java.util.Objects;

public class Edge {
    public BasicBlock from;
    public BasicBlock to;
    public Edge(BasicBlock from, BasicBlock to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Edge other = (Edge) object;
        return from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
