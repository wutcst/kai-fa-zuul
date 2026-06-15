package cn.edu.whut.sept.dungeon.io;

import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.core.GameStatus;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Inventory;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.entity.Trap;
import cn.edu.whut.sept.dungeon.quest.QuestState;
import cn.edu.whut.sept.dungeon.world.Corridor;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Room;
import cn.edu.whut.sept.dungeon.world.Tile;
import cn.edu.whut.sept.dungeon.world.World;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SaveManager {
    public static final File DEFAULT_SAVE_FILE = new File("save/campus-dungeon-save.json");
    private static final int VERSION = 2;

    private final File saveFile;
    private final Gson gson;

    public SaveManager() {
        this(DEFAULT_SAVE_FILE);
    }

    public SaveManager(File saveFile) {
        this.saveFile = saveFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void save(GameState state) {
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create save directory: " + parent);
        }

        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(SaveData.from(state), writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save game.", exception);
        }
    }

    public GameState load() {
        if (!saveFile.exists()) {
            return GameState.initial().withMessage("No saved game.");
        }

        try (FileReader reader = new FileReader(saveFile)) {
            SaveData data = gson.fromJson(reader, SaveData.class);
            if (data == null) {
                return GameState.initial().withMessage("No saved game.");
            }
            if (!data.isLoadable()) {
                return GameState.initial().withMessage("Saved game is incompatible.");
            }
            return data.toGameState().withMessage("Loaded saved game.");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load game.", exception);
        }
    }

    static final class SaveData {
        int version;
        Long seed;
        int depth;
        boolean started;
        boolean exited;
        boolean saveRequested;
        GameStatus status;
        PlayerData player;
        WorldData world;
        List<String> inventory;
        QuestData quest;
        EntityData entities;
        List<String> explored;
        String message;

        static SaveData from(GameState state) {
            SaveData data = new SaveData();
            data.version = VERSION;
            data.seed = state.getSeed();
            data.depth = state.getDepth();
            data.started = state.isStarted();
            data.exited = state.isExited();
            data.saveRequested = state.isSaveRequested();
            data.status = state.getStatus();
            data.player = PlayerData.from(state.getPlayer());
            data.world = state.getWorld() == null ? null : WorldData.from(state.getWorld());
            data.inventory = state.getInventory().getItemIds();
            data.quest = QuestData.from(state.getQuest());
            data.entities = new EntityData();
            data.entities.items = new ArrayList<ItemData>();
            for (Item item : state.getItems()) {
                data.entities.items.add(ItemData.from(item));
            }
            data.entities.enemies = new ArrayList<EnemyData>();
            for (Enemy enemy : state.getEnemies()) {
                data.entities.enemies.add(EnemyData.from(enemy));
            }
            data.entities.npcs = new ArrayList<NpcData>();
            for (Npc npc : state.getNpcs()) {
                data.entities.npcs.add(NpcData.from(npc));
            }
            data.entities.traps = new ArrayList<TrapData>();
            for (Trap trap : state.getTraps()) {
                data.entities.traps.add(TrapData.from(trap));
            }
            data.explored = encodeBooleans(state.copyExplored());
            data.message = state.getMessage();
            return data;
        }

        GameState toGameState() {
            World restoredWorld = world == null ? null : world.toWorld();
            GameState.PlayerState restoredPlayer = player == null
                    ? GameState.PlayerState.origin()
                    : player.toPlayerState();
            List<Item> restoredItems = entities == null ? Collections.<Item>emptyList() : entities.toItems();
            List<Enemy> restoredEnemies = entities == null ? Collections.<Enemy>emptyList() : entities.toEnemies();
            List<Npc> restoredNpcs = entities == null ? Collections.<Npc>emptyList() : entities.toNpcs();
            List<Trap> restoredTraps = entities == null ? Collections.<Trap>emptyList() : entities.toTraps();
            QuestState restoredQuest = quest == null ? QuestState.initial() : quest.toQuestState();
            GameStatus restoredStatus = status == null
                    ? (restoredQuest.isCompleted() ? GameStatus.COMPLETED : GameStatus.PLAYING)
                    : status;
            int restoredDepth = depth <= 0 ? 1 : depth;
            return GameState.restored(seed, restoredDepth, started, exited, saveRequested, restoredStatus, restoredPlayer, restoredWorld,
                    Inventory.of(inventory), restoredItems, restoredEnemies, restoredNpcs, restoredTraps, restoredQuest,
                    decodeBooleans(explored), message);
        }

        boolean isLoadable() {
            return player != null && (world == null || world.isLoadable());
        }
    }

    static final class PlayerData {
        int x;
        int y;
        Direction direction;
        int steps;
        int hp;
        int maxHp;
        int atk;
        int def;
        int level;
        int exp;
        String weapon;
        String armor;
        int coffeeBoost;

        static PlayerData from(GameState.PlayerState player) {
            PlayerData data = new PlayerData();
            data.x = player.getX();
            data.y = player.getY();
            data.direction = player.getDirection();
            data.steps = player.getSteps();
            data.hp = player.getHp();
            data.maxHp = player.getMaxHp();
            data.atk = player.getAtk();
            data.def = player.getDef();
            data.level = player.getLevel();
            data.exp = player.getExp();
            data.weapon = player.getWeapon();
            data.armor = player.getArmor();
            data.coffeeBoost = player.getCoffeeBoost();
            return data;
        }

        GameState.PlayerState toPlayerState() {
            int restoredMaxHp = maxHp <= 0 ? GameState.PlayerState.DEFAULT_MAX_HP : maxHp;
            int restoredHp = hp <= 0 && maxHp <= 0 ? restoredMaxHp : hp;
            int restoredAtk = atk <= 0 ? GameState.PlayerState.DEFAULT_ATK : atk;
            int restoredDef = def < 0 ? GameState.PlayerState.DEFAULT_DEF : def;
            int restoredLevel = level <= 0 ? GameState.PlayerState.DEFAULT_LEVEL : level;
            int restoredExp = exp < 0 ? GameState.PlayerState.DEFAULT_EXP : exp;
            return GameState.PlayerState.of(x, y, direction == null ? Direction.SOUTH : direction, steps,
                    restoredHp, restoredMaxHp, restoredAtk, restoredDef, restoredLevel, restoredExp,
                    weapon, armor, coffeeBoost);
        }
    }

    static final class WorldData {
        int width;
        int height;
        List<String> tiles;
        List<RoomData> rooms;
        List<CorridorData> corridors;
        PositionData spawn;
        PositionData defenseHall;
        PositionData stairs;

        static WorldData from(World world) {
            WorldData data = new WorldData();
            data.width = world.getWidth();
            data.height = world.getHeight();
            data.tiles = encodeTiles(world);
            data.rooms = new ArrayList<RoomData>();
            for (Room room : world.getRooms()) {
                data.rooms.add(RoomData.from(room));
            }
            data.corridors = new ArrayList<CorridorData>();
            for (Corridor corridor : world.getCorridors()) {
                data.corridors.add(CorridorData.from(corridor));
            }
            data.spawn = PositionData.from(world.getSpawnPosition());
            data.defenseHall = PositionData.from(world.getDefenseHallPosition());
            data.stairs = PositionData.from(world.getStairsPosition());
            return data;
        }

        World toWorld() {
            PositionData restoredStairs = stairs == null ? defenseHall : stairs;
            return new World(width, height, decodeTiles(tiles, width, height), toRooms(), toCorridors(),
                    spawn.toPosition(), defenseHall.toPosition(), restoredStairs.toPosition());
        }

        boolean isLoadable() {
            return width > 0 && height > 0 && tiles != null && tiles.size() == height
                    && spawn != null && defenseHall != null;
        }

        private List<Room> toRooms() {
            List<Room> result = new ArrayList<Room>();
            if (rooms != null) {
                for (RoomData room : rooms) {
                    result.add(room.toRoom());
                }
            }
            return result;
        }

        private List<Corridor> toCorridors() {
            List<Corridor> result = new ArrayList<Corridor>();
            if (corridors != null) {
                for (CorridorData corridor : corridors) {
                    result.add(corridor.toCorridor());
                }
            }
            return result;
        }
    }

    static final class RoomData {
        int x;
        int y;
        int width;
        int height;

        static RoomData from(Room room) {
            RoomData data = new RoomData();
            data.x = room.getX();
            data.y = room.getY();
            data.width = room.getWidth();
            data.height = room.getHeight();
            return data;
        }

        Room toRoom() {
            return new Room(x, y, width, height);
        }
    }

    static final class CorridorData {
        PositionData from;
        PositionData corner;
        PositionData to;

        static CorridorData from(Corridor corridor) {
            CorridorData data = new CorridorData();
            data.from = PositionData.from(corridor.getFrom());
            data.corner = PositionData.from(corridor.getCorner());
            data.to = PositionData.from(corridor.getTo());
            return data;
        }

        Corridor toCorridor() {
            return new Corridor(from.toPosition(), corner.toPosition(), to.toPosition());
        }
    }

    static final class PositionData {
        int x;
        int y;

        static PositionData from(Position position) {
            PositionData data = new PositionData();
            data.x = position.getX();
            data.y = position.getY();
            return data;
        }

        Position toPosition() {
            return new Position(x, y);
        }
    }

    static final class QuestData {
        boolean reportIssued;
        boolean slidesExported;
        boolean passIssued;
        boolean mavenPuzzleSolved;
        boolean completed;

        static QuestData from(QuestState quest) {
            QuestData data = new QuestData();
            data.reportIssued = quest.isReportIssued();
            data.slidesExported = quest.isSlidesExported();
            data.passIssued = quest.isPassIssued();
            data.mavenPuzzleSolved = quest.isMavenPuzzleSolved();
            data.completed = quest.isCompleted();
            return data;
        }

        QuestState toQuestState() {
            return new QuestState(reportIssued, slidesExported, passIssued, mavenPuzzleSolved, completed);
        }
    }

    static final class EntityData {
        List<ItemData> items = Collections.emptyList();
        List<EnemyData> enemies = Collections.emptyList();
        List<NpcData> npcs = Collections.emptyList();
        List<TrapData> traps = Collections.emptyList();
        List<String> doors = Collections.emptyList();

        List<Item> toItems() {
            List<Item> result = new ArrayList<Item>();
            if (items != null) {
                for (ItemData item : items) {
                    result.add(item.toItem());
                }
            }
            return result;
        }

        List<Enemy> toEnemies() {
            List<Enemy> result = new ArrayList<Enemy>();
            if (enemies != null) {
                for (EnemyData enemy : enemies) {
                    result.add(enemy.toEnemy());
                }
            }
            return result;
        }

        List<Npc> toNpcs() {
            List<Npc> result = new ArrayList<Npc>();
            if (npcs != null) {
                for (NpcData npc : npcs) {
                    result.add(npc.toNpc());
                }
            }
            return result;
        }

        List<Trap> toTraps() {
            List<Trap> result = new ArrayList<Trap>();
            if (traps != null) {
                for (TrapData trap : traps) {
                    result.add(trap.toTrap());
                }
            }
            return result;
        }
    }

    static final class ItemData {
        String id;
        String name;
        PositionData position;
        boolean collected;

        static ItemData from(Item item) {
            ItemData data = new ItemData();
            data.id = item.getId();
            data.name = item.getName();
            data.position = PositionData.from(item.getPosition());
            data.collected = item.isCollected();
            return data;
        }

        Item toItem() {
            return new Item(id, name, position.toPosition(), collected);
        }
    }

    static final class EnemyData {
        String id;
        String type;
        PositionData position;
        int hp;
        int atk;
        int def;
        int expReward;
        boolean alive;

        static EnemyData from(Enemy enemy) {
            EnemyData data = new EnemyData();
            data.id = enemy.getId();
            data.type = enemy.getType();
            data.position = PositionData.from(enemy.getPosition());
            data.hp = enemy.getHp();
            data.atk = enemy.getAtk();
            data.def = enemy.getDef();
            data.expReward = enemy.getExpReward();
            data.alive = enemy.isAlive();
            return data;
        }

        Enemy toEnemy() {
            return new Enemy(id, type, position.toPosition(), hp, atk, def, expReward, alive);
        }
    }

    static final class NpcData {
        String id;
        String name;
        PositionData position;

        static NpcData from(Npc npc) {
            NpcData data = new NpcData();
            data.id = npc.getId();
            data.name = npc.getName();
            data.position = PositionData.from(npc.getPosition());
            return data;
        }

        Npc toNpc() {
            return new Npc(id, name, position.toPosition());
        }
    }

    static final class TrapData {
        String id;
        String type;
        PositionData position;
        boolean triggered;

        static TrapData from(Trap trap) {
            TrapData data = new TrapData();
            data.id = trap.getId();
            data.type = trap.getType();
            data.position = PositionData.from(trap.getPosition());
            data.triggered = trap.isTriggered();
            return data;
        }

        Trap toTrap() {
            return new Trap(id, type, position.toPosition(), triggered);
        }
    }

    private static List<String> encodeTiles(World world) {
        List<String> rows = new ArrayList<String>();
        for (int y = 0; y < world.getHeight(); y++) {
            StringBuilder row = new StringBuilder(world.getWidth());
            for (int x = 0; x < world.getWidth(); x++) {
                row.append(world.getTile(x, y).getSymbol());
            }
            rows.add(row.toString());
        }
        return rows;
    }

    private static Tile[][] decodeTiles(List<String> rows, int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < width; x++) {
                tiles[y][x] = row.charAt(x) == Tile.FLOOR.getSymbol() ? Tile.FLOOR : Tile.WALL;
            }
        }
        return tiles;
    }

    private static List<String> encodeBooleans(boolean[][] grid) {
        List<String> rows = new ArrayList<String>();
        if (grid == null) {
            return rows;
        }
        for (int y = 0; y < grid.length; y++) {
            StringBuilder row = new StringBuilder(grid[y].length);
            for (int x = 0; x < grid[y].length; x++) {
                row.append(grid[y][x] ? '1' : '0');
            }
            rows.add(row.toString());
        }
        return rows;
    }

    private static boolean[][] decodeBooleans(List<String> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        boolean[][] grid = new boolean[rows.size()][rows.get(0).length()];
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            for (int x = 0; x < row.length(); x++) {
                grid[y][x] = row.charAt(x) == '1';
            }
        }
        return grid;
    }
}
