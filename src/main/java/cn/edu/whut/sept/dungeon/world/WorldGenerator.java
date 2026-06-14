package cn.edu.whut.sept.dungeon.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WorldGenerator {
    private static final int MIN_ROOMS = 10;
    private static final int MAX_ROOMS = 14;
    private static final int MIN_ROOM_WIDTH = 4;
    private static final int MAX_ROOM_WIDTH = 10;
    private static final int MIN_ROOM_HEIGHT = 4;
    private static final int MAX_ROOM_HEIGHT = 8;
    private static final int MAX_ATTEMPTS = 500;

    public World generate(long seed) {
        return generate(seed, World.DEFAULT_WIDTH, World.DEFAULT_HEIGHT);
    }

    public World generate(long seed, int width, int height) {
        Random random = new Random(seed);
        Tile[][] tiles = filledWithWalls(width, height);
        int targetRooms = MIN_ROOMS + random.nextInt(MAX_ROOMS - MIN_ROOMS + 1);
        List<Room> rooms = createRooms(random, width, height, targetRooms);
        List<Corridor> corridors = new ArrayList<Corridor>();

        for (Room room : rooms) {
            carveRoom(tiles, room);
        }
        for (int i = 1; i < rooms.size(); i++) {
            Corridor corridor = connectRooms(tiles, rooms.get(i - 1), rooms.get(i), random.nextBoolean());
            corridors.add(corridor);
        }

        Room entrance = rooms.get(0);
        Position spawn = entrance.getCenter();
        Position defenseHall = findFarthestRoomCenter(rooms, spawn);
        return new World(width, height, tiles, rooms, corridors, spawn, defenseHall);
    }

    private List<Room> createRooms(Random random, int width, int height, int targetRooms) {
        List<Room> rooms = new ArrayList<Room>();
        int attempts = 0;
        while (rooms.size() < targetRooms && attempts < MAX_ATTEMPTS) {
            attempts++;
            int roomWidth = randomBetween(random, MIN_ROOM_WIDTH, MAX_ROOM_WIDTH);
            int roomHeight = randomBetween(random, MIN_ROOM_HEIGHT, MAX_ROOM_HEIGHT);
            int x = randomBetween(random, 1, width - roomWidth - 2);
            int y = randomBetween(random, 1, height - roomHeight - 2);
            Room candidate = new Room(x, y, roomWidth, roomHeight);
            if (!intersectsExistingRoom(candidate, rooms)) {
                rooms.add(candidate);
            }
        }

        if (rooms.isEmpty()) {
            throw new IllegalStateException("Failed to generate any room.");
        }
        return rooms;
    }

    private boolean intersectsExistingRoom(Room candidate, List<Room> rooms) {
        for (Room room : rooms) {
            if (candidate.intersectsWithMargin(room, 1)) {
                return true;
            }
        }
        return false;
    }

    private Tile[][] filledWithWalls(int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = Tile.WALL;
            }
        }
        return tiles;
    }

    private void carveRoom(Tile[][] tiles, Room room) {
        for (int y = room.getY(); y <= room.getBottom(); y++) {
            for (int x = room.getX(); x <= room.getRight(); x++) {
                tiles[y][x] = Tile.FLOOR;
            }
        }
    }

    private Corridor connectRooms(Tile[][] tiles, Room first, Room second, boolean horizontalFirst) {
        Position from = first.getCenter();
        Position to = second.getCenter();
        Position corner;
        if (horizontalFirst) {
            corner = new Position(to.getX(), from.getY());
        } else {
            corner = new Position(from.getX(), to.getY());
        }

        carveLine(tiles, from, corner);
        carveLine(tiles, corner, to);
        return new Corridor(from, corner, to);
    }

    private void carveLine(Tile[][] tiles, Position from, Position to) {
        int xStep = Integer.compare(to.getX(), from.getX());
        int yStep = Integer.compare(to.getY(), from.getY());
        int x = from.getX();
        int y = from.getY();
        tiles[y][x] = Tile.FLOOR;
        while (x != to.getX() || y != to.getY()) {
            if (x != to.getX()) {
                x += xStep;
            } else if (y != to.getY()) {
                y += yStep;
            }
            tiles[y][x] = Tile.FLOOR;
        }
    }

    private Position findFarthestRoomCenter(List<Room> rooms, Position spawn) {
        Position farthest = spawn;
        int maxDistance = -1;
        for (Room room : rooms) {
            Position center = room.getCenter();
            int distance = spawn.manhattanDistanceTo(center);
            if (distance > maxDistance) {
                maxDistance = distance;
                farthest = center;
            }
        }
        return farthest;
    }

    private int randomBetween(Random random, int minInclusive, int maxInclusive) {
        if (maxInclusive < minInclusive) {
            return minInclusive;
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }
}
