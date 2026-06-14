package cn.edu.whut.sept.dungeon.world;

public final class Corridor {
    private final Position from;
    private final Position corner;
    private final Position to;

    public Corridor(Position from, Position corner, Position to) {
        this.from = from;
        this.corner = corner;
        this.to = to;
    }

    public Position getFrom() {
        return from;
    }

    public Position getCorner() {
        return corner;
    }

    public Position getTo() {
        return to;
    }
}
