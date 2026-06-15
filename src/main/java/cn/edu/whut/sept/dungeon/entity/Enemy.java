package cn.edu.whut.sept.dungeon.entity;

import cn.edu.whut.sept.dungeon.world.Position;

public final class Enemy {
    private final String id;
    private final String type;
    private final Position position;
    private final int hp;
    private final int atk;
    private final int def;
    private final int expReward;
    private final boolean alive;

    public Enemy(String id, String type, Position position, int hp, int atk, int def, int expReward, boolean alive) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.hp = Math.max(0, hp);
        this.atk = Math.max(0, atk);
        this.def = Math.max(0, def);
        this.expReward = Math.max(0, expReward);
        this.alive = alive && this.hp > 0;
    }

    public static Enemy bug(String id, Position position) {
        return new Enemy(id, "Bug", position, 8, 3, 0, 5, true);
    }

    public static Enemy deadline(String id, Position position) {
        return new Enemy(id, "Deadline", position, 6, 6, 1, 8, true);
    }

    public static Enemy defenseCommittee(String id, Position position) {
        return new Enemy(id, "Defense Committee", position, 28, 9, 3, 30, true);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public int getHp() {
        return hp;
    }

    public int getAtk() {
        return atk;
    }

    public int getDef() {
        return def;
    }

    public int getExpReward() {
        return expReward;
    }

    public boolean isAlive() {
        return alive;
    }

    public Enemy damage(int amount) {
        int nextHp = Math.max(0, hp - amount);
        return new Enemy(id, type, position, nextHp, atk, def, expReward, nextHp > 0);
    }

    public Enemy moveTo(Position nextPosition) {
        return new Enemy(id, type, nextPosition, hp, atk, def, expReward, alive);
    }
}
