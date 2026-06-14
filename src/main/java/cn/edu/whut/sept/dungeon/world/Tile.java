package cn.edu.whut.sept.dungeon.world;

public enum Tile {
    WALL('#', false),
    FLOOR('.', true);

    private final char symbol;
    private final boolean walkable;

    Tile(char symbol, boolean walkable) {
        this.symbol = symbol;
        this.walkable = walkable;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean isWalkable() {
        return walkable;
    }
}
