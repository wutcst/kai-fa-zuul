package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.combat.CombatSystem;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Inventory;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.quest.QuestState;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Room;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.world.WorldGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameState {
    public static final int VISION_RADIUS = 5;
    private static final String REPORT = "report";
    private static final String LAPTOP = "laptop";
    private static final String SLIDES = "slides";
    private static final String PASS = "pass";
    private static final String STUDENT_CARD = "student-card";
    private static final String USB = "usb";
    private static final String SMALL_POTION = "small-potion";
    private static final String BIG_POTION = "big-potion";
    private static final String COFFEE = "coffee";
    private static final String WEAPON_1 = "wooden-keyboard";
    private static final String WEAPON_2 = "steel-keyboard";
    private static final String WEAPON_3 = "refactor-blade";
    private static final String ARMOR_1 = "lab-coat";
    private static final String ARMOR_2 = "review-robe";
    private static final String ARMOR_3 = "defense-suit";
    private static final int ENEMY_AGGRO_RANGE = 8;

    private final Long seed;
    private final boolean started;
    private final boolean exited;
    private final boolean saveRequested;
    private final GameStatus status;
    private final PlayerState player;
    private final World world;
    private final Inventory inventory;
    private final List<Item> items;
    private final List<Enemy> enemies;
    private final List<Npc> npcs;
    private final QuestState quest;
    private final boolean[][] explored;
    private final boolean[][] visible;
    private final String message;

    private GameState(Long seed, boolean started, boolean exited, boolean saveRequested,
                      PlayerState player, World world, Inventory inventory, List<Item> items, boolean completed,
                      boolean[][] explored, boolean[][] visible, String message) {
        this(seed, started, exited, saveRequested, completed ? GameStatus.COMPLETED : GameStatus.PLAYING,
                player, world, inventory, items,
                Collections.<Enemy>emptyList(), Collections.<Npc>emptyList(),
                new QuestState(false, false, false, false, completed),
                explored, visible, message);
    }

    private GameState(Long seed, boolean started, boolean exited, boolean saveRequested,
                      GameStatus status, PlayerState player, World world, Inventory inventory, List<Item> items,
                      List<Enemy> enemies, List<Npc> npcs, QuestState quest, boolean[][] explored,
                      boolean[][] visible, String message) {
        this.seed = seed;
        this.started = started;
        this.exited = exited;
        this.saveRequested = saveRequested;
        this.status = status == null ? GameStatus.PLAYING : status;
        this.player = player;
        this.world = world;
        this.inventory = inventory == null ? Inventory.empty() : inventory;
        this.items = Collections.unmodifiableList(new ArrayList<Item>(items == null
                ? Collections.<Item>emptyList()
                : items));
        this.enemies = Collections.unmodifiableList(new ArrayList<Enemy>(enemies == null
                ? Collections.<Enemy>emptyList()
                : enemies));
        this.npcs = Collections.unmodifiableList(new ArrayList<Npc>(npcs == null
                ? Collections.<Npc>emptyList()
                : npcs));
        this.quest = quest == null ? QuestState.initial() : quest;
        this.explored = copyGrid(explored);
        this.visible = copyGrid(visible);
        this.message = message;
    }

    public static GameState initial() {
        return new GameState(null, false, false, false, GameStatus.PLAYING, PlayerState.origin(),
                null, Inventory.empty(), Collections.<Item>emptyList(), Collections.<Enemy>emptyList(),
                Collections.<Npc>emptyList(),
                QuestState.initial(), null, null, "Ready.");
    }

    public static GameState newGame(long seed) {
        World world = new WorldGenerator().generate(seed);
        PlayerState player = PlayerState.at(world.getSpawnPosition());
        return new GameState(seed, true, false, false, GameStatus.PLAYING, player, world,
                Inventory.empty(), createItems(world), createEnemies(world), createNpcs(world), QuestState.initial(),
                createExploredFor(world, player), createVisibleFor(world, player),
                "New game started with seed " + seed + ".");
    }

    public static GameState restored(Long seed, boolean started, boolean exited, boolean saveRequested,
                                     PlayerState player, World world, Inventory inventory, List<Item> items,
                                     List<Npc> npcs, QuestState quest, boolean[][] explored, String message) {
        GameStatus restoredStatus = quest != null && quest.isCompleted() ? GameStatus.COMPLETED : GameStatus.PLAYING;
        return restored(seed, started, exited, saveRequested, restoredStatus, player, world,
                inventory, items, npcs, quest, explored, message);
    }

    public static GameState restored(Long seed, boolean started, boolean exited, boolean saveRequested,
                                     GameStatus status, PlayerState player, World world, Inventory inventory,
                                     List<Item> items, List<Npc> npcs, QuestState quest, boolean[][] explored,
                                     String message) {
        return restored(seed, started, exited, saveRequested, status, player, world, inventory, items,
                Collections.<Enemy>emptyList(), npcs, quest, explored, message);
    }

    public static GameState restored(Long seed, boolean started, boolean exited, boolean saveRequested,
                                     GameStatus status, PlayerState player, World world, Inventory inventory,
                                     List<Item> items, List<Enemy> enemies, List<Npc> npcs, QuestState quest,
                                     boolean[][] explored, String message) {
        return new GameState(seed, started, exited, saveRequested, status, player, world,
                inventory, items, enemies, npcs, quest, explored, world == null ? null : createVisibleFor(world, player),
                message);
    }

    public Long getSeed() {
        return seed;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isExited() {
        return exited;
    }

    public boolean isSaveRequested() {
        return saveRequested;
    }

    public GameStatus getStatus() {
        return status;
    }

    public boolean isGameOver() {
        return status == GameStatus.GAME_OVER;
    }

    public PlayerState getPlayer() {
        return player;
    }

    public String getWorldStatus() {
        return world == null ? "not-created" : "generated";
    }

    public World getWorld() {
        return world;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    public QuestState getQuest() {
        return quest;
    }

    public boolean isCompleted() {
        return status == GameStatus.COMPLETED || quest.isCompleted();
    }

    public boolean isExplored(int x, int y) {
        return isMarked(explored, x, y);
    }

    public boolean isVisible(int x, int y) {
        return isMarked(visible, x, y);
    }

    public VisibilityState getVisibilityState(int x, int y) {
        if (isVisible(x, y)) {
            return VisibilityState.VISIBLE;
        }
        if (isExplored(x, y)) {
            return VisibilityState.SEEN;
        }
        return VisibilityState.UNSEEN;
    }

    public boolean[][] copyExplored() {
        return copyGrid(explored);
    }

    public int getVisibleCount() {
        return countMarked(visible);
    }

    public int getExploredCount() {
        return countMarked(explored);
    }

    public String getMessage() {
        return message;
    }

    public GameState withMessage(String nextMessage) {
        return new GameState(seed, started, exited, saveRequested, status, player, world,
                inventory, items, enemies, npcs, quest, explored, visible, nextMessage);
    }

    public GameState markExited() {
        return new GameState(seed, started, true, saveRequested, status, player, world,
                inventory, items, enemies, npcs, quest, explored, visible, message);
    }

    public GameState markSaveRequested() {
        return new GameState(seed, started, exited, true, status, player, world,
                inventory, items, enemies, npcs, quest, explored, visible, message);
    }

    public GameState movePlayer(Direction direction) {
        if (isGameOver()) {
            return withMessage("Game over. Start a new game to try again.");
        }
        if (!started || world == null || direction == null) {
            return withMessage("Start a new game first.");
        }

        PlayerState turnedPlayer = player.withDirection(direction);
        Position target = new Position(player.getX() + direction.getDx(), player.getY() + direction.getDy());
        Enemy enemy = enemyAt(target);
        if (enemy != null) {
            return attackEnemy(turnedPlayer, enemy);
        }
        if (!world.isWalkable(target)) {
            return new GameState(seed, started, exited, saveRequested, status, turnedPlayer, world,
                    inventory, items, enemies, npcs, quest, explored, visible, "Blocked by wall.");
        }

        PlayerState movedPlayer = turnedPlayer.moveTo(target);
        return new GameState(seed, started, exited, saveRequested, status, movedPlayer, world,
                inventory, items, enemies, npcs, quest,
                createExploredFor(world, movedPlayer, explored), createVisibleFor(world, movedPlayer),
                "Moved " + direction.name() + ".");
    }

    public GameState interact() {
        if (isGameOver()) {
            return withMessage("Game over. Start a new game to try again.");
        }
        if (!started || world == null) {
            return withMessage("Start a new game first.");
        }

        Npc npc = npcAt(player.getPosition());
        if (npc != null) {
            return talkTo(npc);
        }
        if (player.getPosition().equals(world.getDefenseHallPosition())) {
            if (inventory.containsAll(REPORT, LAPTOP, SLIDES, PASS)) {
                return new GameState(seed, started, exited, saveRequested, GameStatus.COMPLETED, player, world,
                        inventory, items, enemies, npcs, quest.withCompleted(), explored, visible,
                        "Defense completed in " + player.getSteps() + " steps. Excellent software engineering practice!");
            }
            return withMessage("Defense hall locked. Missing: " + missingDefenseMaterials() + ".");
        }
        Item item = itemAt(player.getPosition());
        if (item != null) {
            return collectItem(item);
        }
        return withMessage("Nothing to interact with here.");
    }

    public GameState answer(String answer) {
        if (isGameOver()) {
            return withMessage("Game over. Start a new game to try again.");
        }
        if ("pom.xml".equalsIgnoreCase(answer == null ? "" : answer.trim())) {
            return new GameState(seed, started, exited, saveRequested, status, player, world,
                    inventory, items, enemies, npcs, quest.withMavenPuzzleSolved(), explored, visible,
                    "Correct. Maven project configuration lives in pom.xml.");
        }
        return withMessage("Incorrect. Hint: Maven keeps project configuration in a file named pom.xml.");
    }

    public GameState describeInventory() {
        if (isGameOver()) {
            return withMessage("Game over. Start a new game to try again.");
        }
        if (inventory.contains(BIG_POTION) && player.getHp() < player.getMaxHp()) {
            return usePotion(BIG_POTION, 16);
        }
        if (inventory.contains(SMALL_POTION) && player.getHp() < player.getMaxHp()) {
            return usePotion(SMALL_POTION, 8);
        }
        if (inventory.contains(COFFEE) && player.getCoffeeBoost() == 0) {
            Inventory nextInventory = inventory.remove(COFFEE);
            PlayerState boostedPlayer = player.withCoffeeBoost(3);
            return new GameState(seed, started, exited, saveRequested, status, boostedPlayer, world,
                    nextInventory, items, enemies, npcs, quest, explored, visible,
                    "Coffee inspiration active. Next attacks gain +3 ATK.");
        }
        return withMessage("Inventory: " + inventory.summary() + ".");
    }

    private GameState usePotion(String potionId, int amount) {
        Inventory nextInventory = inventory.remove(potionId);
        PlayerState healedPlayer = player.heal(amount);
        return new GameState(seed, started, exited, saveRequested, status, healedPlayer, world,
                nextInventory, items, enemies, npcs, quest, explored, visible,
                "Used " + potionId + " and restored " + (healedPlayer.getHp() - player.getHp()) + " HP.");
    }

    public GameState damagePlayer(int amount) {
        if (!started || amount <= 0) {
            return this;
        }
        PlayerState damagedPlayer = player.damage(amount);
        return withPlayerAfterDamage(damagedPlayer,
                damagedPlayer.getHp() <= 0 ? "Game over. HP reached 0." : "Player took " + amount + " damage.");
    }

    public GameState advanceEnemyTurn() {
        if (!started || world == null || isGameOver() || isCompleted()) {
            return this;
        }
        List<Enemy> nextEnemies = new ArrayList<Enemy>();
        PlayerState nextPlayer = player;
        GameStatus nextStatus = status;
        List<String> events = new ArrayList<String>();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                nextEnemies.add(enemy);
                continue;
            }
            if (enemy.getPosition().manhattanDistanceTo(nextPlayer.getPosition()) == 1) {
                int damage = CombatSystem.damage(enemy.getAtk(), nextPlayer.getDef());
                nextPlayer = nextPlayer.damage(damage);
                events.add(enemy.getType() + " hits you for " + damage + " damage");
                nextEnemies.add(enemy);
                if (nextPlayer.getHp() <= 0) {
                    nextStatus = GameStatus.GAME_OVER;
                    break;
                }
                continue;
            }
            Position nextPosition = nextEnemyPosition(enemy, nextPlayer.getPosition(), nextEnemies);
            nextEnemies.add(nextPosition.equals(enemy.getPosition()) ? enemy : enemy.moveTo(nextPosition));
        }

        if (nextStatus == GameStatus.GAME_OVER) {
            return new GameState(seed, started, exited, saveRequested, nextStatus, nextPlayer, world,
                    inventory, items, appendRemainingEnemies(nextEnemies), npcs, quest, explored, visible,
                    joinedTurnMessage(events, "Game over. HP reached 0."));
        }
        if (events.isEmpty() && sameEnemyPositions(enemies, nextEnemies)) {
            return this;
        }
        return new GameState(seed, started, exited, saveRequested, status, nextPlayer, world,
                inventory, items, nextEnemies, npcs, quest, explored, visible,
                joinedTurnMessage(events, message));
    }

    private GameState withPlayerAfterDamage(PlayerState damagedPlayer, String nextMessage) {
        GameStatus nextStatus = damagedPlayer.getHp() <= 0 ? GameStatus.GAME_OVER : status;
        return new GameState(seed, started, exited, saveRequested, nextStatus, damagedPlayer, world,
                inventory, items, enemies, npcs, quest, explored, visible, nextMessage);
    }

    public Item itemAt(Position position) {
        for (Item item : items) {
            if (!item.isCollected() && item.getPosition().equals(position)) {
                return item;
            }
        }
        return null;
    }

    public Npc npcAt(Position position) {
        for (Npc npc : npcs) {
            if (npc.getPosition().equals(position)) {
                return npc;
            }
        }
        return null;
    }

    public Enemy enemyAt(Position position) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getPosition().equals(position)) {
                return enemy;
            }
        }
        return null;
    }

    private GameState attackEnemy(PlayerState turnedPlayer, Enemy enemy) {
        int damage = CombatSystem.damage(turnedPlayer.effectiveAtk(), enemy.getDef());
        Enemy damagedEnemy = enemy.damage(damage);
        List<Enemy> nextEnemies = replaceEnemy(enemy, damagedEnemy);
        PlayerState nextPlayer = turnedPlayer.afterAttack();
        String nextMessage = "Hit " + enemy.getType() + " for " + damage + " damage.";
        if (!damagedEnemy.isAlive()) {
            nextPlayer = nextPlayer.gainExp(enemy.getExpReward());
            nextMessage = "Defeated " + enemy.getType() + " and gained " + enemy.getExpReward() + " EXP.";
        }
        return new GameState(seed, started, exited, saveRequested, status, nextPlayer, world,
                inventory, items, nextEnemies, npcs, quest, explored, visible, nextMessage);
    }

    private List<Enemy> replaceEnemy(Enemy enemy, Enemy replacement) {
        List<Enemy> result = new ArrayList<Enemy>();
        for (Enemy current : enemies) {
            if (current.getId().equals(enemy.getId())) {
                result.add(replacement);
            } else {
                result.add(current);
            }
        }
        return result;
    }

    private Position nextEnemyPosition(Enemy enemy, Position target, List<Enemy> alreadyMoved) {
        if (enemy.getPosition().manhattanDistanceTo(target) > ENEMY_AGGRO_RANGE) {
            return enemy.getPosition();
        }
        Direction[] directions = preferredDirectionsToward(enemy.getPosition(), target);
        for (Direction direction : directions) {
            Position candidate = new Position(enemy.getPosition().getX() + direction.getDx(),
                    enemy.getPosition().getY() + direction.getDy());
            if (canEnemyMoveTo(candidate, enemy, alreadyMoved)) {
                return candidate;
            }
        }
        return enemy.getPosition();
    }

    private Direction[] preferredDirectionsToward(Position from, Position target) {
        Direction horizontal = target.getX() < from.getX() ? Direction.WEST : Direction.EAST;
        Direction vertical = target.getY() < from.getY() ? Direction.NORTH : Direction.SOUTH;
        if (Math.abs(target.getX() - from.getX()) >= Math.abs(target.getY() - from.getY())) {
            return new Direction[]{horizontal, vertical, opposite(vertical), opposite(horizontal)};
        }
        return new Direction[]{vertical, horizontal, opposite(horizontal), opposite(vertical)};
    }

    private Direction opposite(Direction direction) {
        switch (direction) {
            case NORTH:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.NORTH;
            case WEST:
                return Direction.EAST;
            case EAST:
                return Direction.WEST;
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    private boolean canEnemyMoveTo(Position candidate, Enemy movingEnemy, List<Enemy> alreadyMoved) {
        if (!world.contains(candidate.getX(), candidate.getY()) || !world.isWalkable(candidate)) {
            return false;
        }
        if (candidate.equals(player.getPosition())) {
            return false;
        }
        for (Enemy enemy : alreadyMoved) {
            if (enemy.isAlive() && enemy.getPosition().equals(candidate)) {
                return false;
            }
        }
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()
                    && !enemy.getId().equals(movingEnemy.getId())
                    && enemy.getPosition().equals(candidate)
                    && !alreadyContains(alreadyMoved, enemy.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean alreadyContains(List<Enemy> alreadyMoved, String enemyId) {
        for (Enemy enemy : alreadyMoved) {
            if (enemy.getId().equals(enemyId)) {
                return true;
            }
        }
        return false;
    }

    private List<Enemy> appendRemainingEnemies(List<Enemy> nextEnemies) {
        List<Enemy> result = new ArrayList<Enemy>(nextEnemies);
        for (Enemy enemy : enemies) {
            if (!alreadyContains(result, enemy.getId())) {
                result.add(enemy);
            }
        }
        return result;
    }

    private boolean sameEnemyPositions(List<Enemy> before, List<Enemy> after) {
        if (before.size() != after.size()) {
            return false;
        }
        for (int i = 0; i < before.size(); i++) {
            Enemy left = before.get(i);
            Enemy right = after.get(i);
            if (!left.getId().equals(right.getId())
                    || !left.getPosition().equals(right.getPosition())
                    || left.isAlive() != right.isAlive()
                    || left.getHp() != right.getHp()) {
                return false;
            }
        }
        return true;
    }

    private String joinedTurnMessage(List<String> events, String fallback) {
        if (events.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder(fallback == null ? "" : fallback);
        for (String event : events) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(event).append('.');
        }
        return builder.toString();
    }

    private GameState collectItem(Item item) {
        List<Item> nextItems = new ArrayList<Item>();
        for (Item current : items) {
            nextItems.add(current.getId().equals(item.getId()) ? current.collect() : current);
        }
        Inventory nextInventory = inventory.add(item.getId());
        PlayerState nextPlayer = player;
        String nextMessage = "Picked up " + item.getName() + ".";
        if (isWeapon(item.getId())) {
            nextPlayer = player.equipWeapon(item.getId(), weaponBonus(item.getId()));
            nextMessage = "Equipped " + item.getName() + ". ATK is now " + nextPlayer.getAtk() + ".";
        } else if (isArmor(item.getId())) {
            nextPlayer = player.equipArmor(item.getId(), armorBonus(item.getId()));
            nextMessage = "Equipped " + item.getName() + ". DEF is now " + nextPlayer.getDef() + ".";
        }
        return new GameState(seed, started, exited, saveRequested, status, nextPlayer, world,
                nextInventory, nextItems, enemies, npcs, quest, explored, visible, nextMessage);
    }

    private GameState talkTo(Npc npc) {
        if ("librarian".equals(npc.getId())) {
            if (!inventory.contains(STUDENT_CARD)) {
                return withMessage("Librarian: Bring your student-card before I lend the defense report.");
            }
            if (!inventory.contains(REPORT)) {
                return grantItem(REPORT, "Librarian: Student-card checked. Here is the defense report.",
                        quest.withReportIssued());
            }
            return withMessage("Librarian: The report is already in your backpack.");
        }
        if ("assistant".equals(npc.getId())) {
            if (!inventory.contains(USB)) {
                return withMessage("Assistant: Find a usb first; the demo slides need to be exported.");
            }
            if (!quest.isMavenPuzzleSolved()) {
                return withMessage("Assistant: Course puzzle - what is Maven's configuration file? Use !answer(pom.xml).");
            }
            Inventory nextInventory = inventory.add(LAPTOP).add(SLIDES);
            return new GameState(seed, started, exited, saveRequested, status, player, world,
                    nextInventory, items, enemies, npcs, quest.withSlidesExported(), explored, visible,
                    "Assistant: USB accepted. Laptop ready and slides exported.");
        }
        if ("teacher".equals(npc.getId())) {
            if (!inventory.containsAll(REPORT, LAPTOP, SLIDES)) {
                return withMessage("Teacher: I need to see report, laptop, and slides before issuing the pass.");
            }
            if (!inventory.contains(PASS)) {
                return grantItem(PASS, "Teacher: Materials checked. Take this defense pass.", quest.withPassIssued());
            }
            return withMessage("Teacher: You already have the defense pass. Go to the defense hall.");
        }
        return withMessage(npc.getName() + ": Keep exploring and prepare your defense.");
    }

    private GameState grantItem(String itemId, String nextMessage, QuestState nextQuest) {
        return new GameState(seed, started, exited, saveRequested, status, player, world,
                inventory.add(itemId), items, enemies, npcs, nextQuest, explored, visible, nextMessage);
    }

    private String missingDefenseMaterials() {
        List<String> missing = new ArrayList<String>();
        addMissing(missing, REPORT);
        addMissing(missing, LAPTOP);
        addMissing(missing, SLIDES);
        addMissing(missing, PASS);
        return missing.toString();
    }

    private void addMissing(List<String> missing, String itemId) {
        if (!inventory.contains(itemId)) {
            missing.add(itemId);
        }
    }

    public static final class PlayerState {
        public static final int DEFAULT_MAX_HP = 30;
        public static final int DEFAULT_ATK = 5;
        public static final int DEFAULT_DEF = 1;
        public static final int DEFAULT_LEVEL = 1;
        public static final int DEFAULT_EXP = 0;

        private final int x;
        private final int y;
        private final Direction direction;
        private final int steps;
        private final int hp;
        private final int maxHp;
        private final int atk;
        private final int def;
        private final int level;
        private final int exp;
        private final String weapon;
        private final String armor;
        private final int coffeeBoost;

        private PlayerState(int x, int y, Direction direction, int steps,
                            int hp, int maxHp, int atk, int def, int level, int exp,
                            String weapon, String armor, int coffeeBoost) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.steps = steps;
            this.maxHp = Math.max(1, maxHp);
            this.hp = Math.max(0, Math.min(hp, this.maxHp));
            this.atk = Math.max(0, atk);
            this.def = Math.max(0, def);
            this.level = Math.max(1, level);
            this.exp = Math.max(0, exp);
            this.weapon = weapon == null ? "none" : weapon;
            this.armor = armor == null ? "none" : armor;
            this.coffeeBoost = Math.max(0, coffeeBoost);
        }

        public static PlayerState origin() {
            return withDefaults(0, 0, Direction.SOUTH, 0);
        }

        public static PlayerState at(Position position) {
            return withDefaults(position.getX(), position.getY(), Direction.SOUTH, 0);
        }

        public static PlayerState of(int x, int y, Direction direction, int steps) {
            return withDefaults(x, y, direction, steps);
        }

        public static PlayerState of(int x, int y, Direction direction, int steps,
                                     int hp, int maxHp, int atk, int def, int level, int exp) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, def, level, exp,
                    "none", "none", 0);
        }

        public static PlayerState of(int x, int y, Direction direction, int steps,
                                     int hp, int maxHp, int atk, int def, int level, int exp,
                                     String weapon, String armor, int coffeeBoost) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, def, level, exp,
                    weapon, armor, coffeeBoost);
        }

        private static PlayerState withDefaults(int x, int y, Direction direction, int steps) {
            return new PlayerState(x, y, direction, steps, DEFAULT_MAX_HP, DEFAULT_MAX_HP,
                    DEFAULT_ATK, DEFAULT_DEF, DEFAULT_LEVEL, DEFAULT_EXP, "none", "none", 0);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getSteps() {
            return steps;
        }

        public int getHp() {
            return hp;
        }

        public int getMaxHp() {
            return maxHp;
        }

        public int getAtk() {
            return atk;
        }

        public int getDef() {
            return def;
        }

        public int getLevel() {
            return level;
        }

        public int getExp() {
            return exp;
        }

        public String getWeapon() {
            return weapon;
        }

        public String getArmor() {
            return armor;
        }

        public int getCoffeeBoost() {
            return coffeeBoost;
        }

        public int effectiveAtk() {
            return atk + coffeeBoost;
        }

        public Position getPosition() {
            return new Position(x, y);
        }

        private PlayerState withDirection(Direction nextDirection) {
            return new PlayerState(x, y, nextDirection, steps, hp, maxHp, atk, def, level, exp,
                    weapon, armor, coffeeBoost);
        }

        private PlayerState moveTo(Position position) {
            return new PlayerState(position.getX(), position.getY(), direction, steps + 1,
                    hp, maxHp, atk, def, level, exp, weapon, armor, coffeeBoost);
        }

        private PlayerState damage(int amount) {
            return new PlayerState(x, y, direction, steps, hp - amount, maxHp, atk, def, level, exp,
                    weapon, armor, coffeeBoost);
        }

        private PlayerState gainExp(int amount) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, def, level, exp + amount,
                    weapon, armor, coffeeBoost);
        }

        private PlayerState heal(int amount) {
            return new PlayerState(x, y, direction, steps, hp + amount, maxHp, atk, def, level, exp,
                    weapon, armor, coffeeBoost);
        }

        private PlayerState equipWeapon(String itemId, int bonus) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, DEFAULT_ATK + bonus, def, level, exp,
                    itemId, armor, coffeeBoost);
        }

        private PlayerState equipArmor(String itemId, int bonus) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, DEFAULT_DEF + bonus, level, exp,
                    weapon, itemId, coffeeBoost);
        }

        private PlayerState withCoffeeBoost(int bonus) {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, def, level, exp,
                    weapon, armor, bonus);
        }

        private PlayerState afterAttack() {
            return new PlayerState(x, y, direction, steps, hp, maxHp, atk, def, level, exp,
                    weapon, armor, 0);
        }
    }

    private static boolean[][] createVisibleFor(World world, PlayerState player) {
        boolean[][] nextVisible = new boolean[world.getHeight()][world.getWidth()];
        markVision(world, player, nextVisible);
        return nextVisible;
    }

    private static boolean[][] createExploredFor(World world, PlayerState player) {
        boolean[][] nextExplored = new boolean[world.getHeight()][world.getWidth()];
        markVision(world, player, nextExplored);
        return nextExplored;
    }

    private static boolean[][] createExploredFor(World world, PlayerState player, boolean[][] previousExplored) {
        boolean[][] nextExplored = copyGrid(previousExplored);
        if (nextExplored == null) {
            nextExplored = new boolean[world.getHeight()][world.getWidth()];
        }
        markVision(world, player, nextExplored);
        return nextExplored;
    }

    private static void markVision(World world, PlayerState player, boolean[][] target) {
        for (int y = player.getY() - VISION_RADIUS; y <= player.getY() + VISION_RADIUS; y++) {
            for (int x = player.getX() - VISION_RADIUS; x <= player.getX() + VISION_RADIUS; x++) {
                if (world.contains(x, y)) {
                    target[y][x] = true;
                }
            }
        }
    }

    private static boolean isMarked(boolean[][] grid, int x, int y) {
        return grid != null && y >= 0 && y < grid.length && x >= 0 && x < grid[y].length && grid[y][x];
    }

    private static int countMarked(boolean[][] grid) {
        if (grid == null) {
            return 0;
        }
        int count = 0;
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean[][] copyGrid(boolean[][] source) {
        if (source == null) {
            return null;
        }
        boolean[][] copy = new boolean[source.length][];
        for (int y = 0; y < source.length; y++) {
            copy[y] = new boolean[source[y].length];
            System.arraycopy(source[y], 0, copy[y], 0, source[y].length);
        }
        return copy;
    }

    private static List<Item> createItems(World world) {
        List<String> ids = new ArrayList<String>();
        ids.add(STUDENT_CARD);
        ids.add(USB);
        ids.add(SMALL_POTION);
        ids.add(BIG_POTION);
        ids.add(COFFEE);
        ids.add(WEAPON_1);
        ids.add(WEAPON_2);
        ids.add(WEAPON_3);
        ids.add(ARMOR_1);
        ids.add(ARMOR_2);
        ids.add(ARMOR_3);

        List<Item> result = new ArrayList<Item>();
        List<Room> candidateRooms = itemRooms(world);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            result.add(new Item(id, displayName(id), itemPosition(candidateRooms, i), false));
        }
        return result;
    }

    private static Position itemPosition(List<Room> rooms, int itemIndex) {
        int roomIndex = itemIndex < 2 ? itemIndex : itemIndex + 5;
        Room room = rooms.get(roomIndex % rooms.size());
        Position center = room.getCenter();
        int offset = itemIndex % 4;
        int x = center.getX();
        int y = center.getY();
        if (offset == 0 && center.getX() + 1 <= room.getRight()) {
            x = center.getX() + 1;
        } else if (offset == 1 && center.getX() - 1 >= room.getX()) {
            x = center.getX() - 1;
        } else if (offset == 2 && center.getY() + 1 <= room.getBottom()) {
            y = center.getY() + 1;
        } else if (center.getY() - 1 >= room.getY()) {
            y = center.getY() - 1;
        }
        return new Position(x, y);
    }

    private static List<Npc> createNpcs(World world) {
        List<Npc> result = new ArrayList<Npc>();
        List<Room> rooms = itemRooms(world);
        result.add(new Npc("librarian", "Librarian", rooms.get(Math.min(2, rooms.size() - 1)).getCenter()));
        result.add(new Npc("assistant", "Assistant", rooms.get(Math.min(3, rooms.size() - 1)).getCenter()));
        result.add(new Npc("teacher", "Teacher", rooms.get(Math.min(4, rooms.size() - 1)).getCenter()));
        return result;
    }

    private static List<Enemy> createEnemies(World world) {
        List<Enemy> result = new ArrayList<Enemy>();
        List<Room> rooms = itemRooms(world);
        if (!rooms.isEmpty()) {
            result.add(Enemy.bug("bug-1", rooms.get(Math.min(5, rooms.size() - 1)).getCenter()));
            result.add(Enemy.deadline("deadline-1", rooms.get(Math.min(6, rooms.size() - 1)).getCenter()));
        }
        return result;
    }

    private static List<Room> itemRooms(World world) {
        List<Room> result = new ArrayList<Room>();
        for (Room room : world.getRooms()) {
            Position center = room.getCenter();
            if (!center.equals(world.getSpawnPosition()) && !center.equals(world.getDefenseHallPosition())) {
                result.add(room);
            }
        }
        if (result.isEmpty()) {
            result.addAll(world.getRooms());
        }
        return result;
    }

    private static String displayName(String itemId) {
        if (STUDENT_CARD.equals(itemId)) {
            return "student card";
        }
        if (SMALL_POTION.equals(itemId)) {
            return "small potion";
        }
        if (BIG_POTION.equals(itemId)) {
            return "big potion";
        }
        return itemId;
    }

    private static boolean isWeapon(String itemId) {
        return WEAPON_1.equals(itemId) || WEAPON_2.equals(itemId) || WEAPON_3.equals(itemId);
    }

    private static boolean isArmor(String itemId) {
        return ARMOR_1.equals(itemId) || ARMOR_2.equals(itemId) || ARMOR_3.equals(itemId);
    }

    private static int weaponBonus(String itemId) {
        if (WEAPON_3.equals(itemId)) {
            return 6;
        }
        if (WEAPON_2.equals(itemId)) {
            return 4;
        }
        if (WEAPON_1.equals(itemId)) {
            return 2;
        }
        return 0;
    }

    private static int armorBonus(String itemId) {
        if (ARMOR_3.equals(itemId)) {
            return 5;
        }
        if (ARMOR_2.equals(itemId)) {
            return 3;
        }
        if (ARMOR_1.equals(itemId)) {
            return 1;
        }
        return 0;
    }
}
