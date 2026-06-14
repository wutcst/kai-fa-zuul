package cn.edu.whut.sept.dungeon.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public final class World {
    public static final int DEFAULT_WIDTH = 80;
    public static final int DEFAULT_HEIGHT = 40;

    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private final List<Room> rooms;
    private final List<Corridor> corridors;
    private final Position spawnPosition;
    private final Position defenseHallPosition;

    public World(int width, int height, Tile[][] tiles, List<Room> rooms, List<Corridor> corridors,
                 Position spawnPosition, Position defenseHallPosition) {
        this.width = width;
        this.height = height;
        this.tiles = copyTiles(tiles, width, height);
        this.rooms = Collections.unmodifiableList(new ArrayList<Room>(rooms));
        this.corridors = Collections.unmodifiableList(new ArrayList<Corridor>(corridors));
        this.spawnPosition = spawnPosition;
        this.defenseHallPosition = defenseHallPosition;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (!contains(x, y)) {
            return Tile.WALL;
        }
        return tiles[y][x];
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isWalkable(Position position) {
        return contains(position.getX(), position.getY())
                && getTile(position.getX(), position.getY()).isWalkable();
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Corridor> getCorridors() {
        return corridors;
    }

    public Position getSpawnPosition() {
        return spawnPosition;
    }

    public Position getDefenseHallPosition() {
        return defenseHallPosition;
    }

    public boolean isReachable(Position start, Position target) {
        if (!isWalkable(start) || !isWalkable(target)) {
            return false;
        }

        boolean[][] visited = new boolean[height][width];
        Queue<Position> queue = new ArrayDeque<Position>();
        queue.add(start);
        visited[start.getY()][start.getX()] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (current.equals(target)) {
                return true;
            }

            for (int i = 0; i < dx.length; i++) {
                int nextX = current.getX() + dx[i];
                int nextY = current.getY() + dy[i];
                if (contains(nextX, nextY) && !visited[nextY][nextX] && getTile(nextX, nextY).isWalkable()) {
                    visited[nextY][nextX] = true;
                    queue.add(new Position(nextX, nextY));
                }
            }
        }
        return false;
    }

    public String toTileString() {
        StringBuilder builder = new StringBuilder(width * height + height);
        for (int y = 0; y < height; y++) {
            if (y > 0) {
                builder.append('\n');
            }
            for (int x = 0; x < width; x++) {
                builder.append(tiles[y][x].getSymbol());
            }
        }
        return builder.toString();
    }

    private static Tile[][] copyTiles(Tile[][] source, int width, int height) {
        Tile[][] copy = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy[y][x] = source[y][x];
            }
        }
        return copy;
    }
}
