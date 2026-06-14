package cn.edu.whut.sept.dungeon.world;

public final class Room {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Room(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Room size must be positive.");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRight() {
        return x + width - 1;
    }

    public int getBottom() {
        return y + height - 1;
    }

    public Position getCenter() {
        return new Position(x + width / 2, y + height / 2);
    }

    public boolean intersectsWithMargin(Room other, int margin) {
        return x - margin <= other.getRight()
                && getRight() + margin >= other.x
                && y - margin <= other.getBottom()
                && getBottom() + margin >= other.y;
    }
}
