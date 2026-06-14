package cn.edu.whut.sept.dungeon.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorldGeneratorTest {
    @Test
    public void sameSeedGeneratesSameWorld() {
        WorldGenerator generator = new WorldGenerator();

        World first = generator.generate(20260614L);
        World second = generator.generate(20260614L);

        assertEquals(first.toTileString(), second.toTileString());
        assertEquals(first.getSpawnPosition(), second.getSpawnPosition());
        assertEquals(first.getDefenseHallPosition(), second.getDefenseHallPosition());
    }

    @Test
    public void differentSeedsGenerateDifferentWorlds() {
        WorldGenerator generator = new WorldGenerator();

        World first = generator.generate(20260614L);
        World second = generator.generate(20260615L);

        assertFalse(first.toTileString().equals(second.toTileString()));
    }

    @Test
    public void spawnIsWalkable() {
        World world = new WorldGenerator().generate(123L);

        assertTrue(world.isWalkable(world.getSpawnPosition()));
    }

    @Test
    public void generatedRoomsAreReachable() {
        World world = new WorldGenerator().generate(123L);

        for (Room room : world.getRooms()) {
            assertTrue(world.isReachable(world.getSpawnPosition(), room.getCenter()));
        }
        assertTrue(world.isReachable(world.getSpawnPosition(), world.getDefenseHallPosition()));
    }

    @Test
    public void generatedWorldHasExpectedDimensionsAndRoomCount() {
        World world = new WorldGenerator().generate(123L);

        assertEquals(World.DEFAULT_WIDTH, world.getWidth());
        assertEquals(World.DEFAULT_HEIGHT, world.getHeight());
        assertTrue(world.getRooms().size() >= 10);
        assertTrue(world.getRooms().size() <= 14);
    }

    @Test
    public void worldBoundariesStayWallsAndTilesAreComplete() {
        World world = new WorldGenerator().generate(123L);

        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                assertTrue(world.getTile(x, y) == Tile.WALL || world.getTile(x, y) == Tile.FLOOR);
                if (x == 0 || y == 0 || x == world.getWidth() - 1 || y == world.getHeight() - 1) {
                    assertEquals(Tile.WALL, world.getTile(x, y));
                }
            }
        }
    }
}
