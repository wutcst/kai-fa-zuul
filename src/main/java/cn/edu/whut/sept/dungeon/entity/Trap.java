package cn.edu.whut.sept.dungeon.entity;

import cn.edu.whut.sept.dungeon.world.Position;

public final class Trap {
    public static final String DAMAGE = "damage";
    public static final String TELEPORT = "teleport";
    public static final String WEAKNESS = "weakness";

    private final String id;
    private final String type;
    private final Position position;
    private final boolean triggered;

    public Trap(String id, String type, Position position, boolean triggered) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.triggered = triggered;
    }

    public static Trap damage(String id, Position position) {
        return new Trap(id, DAMAGE, position, false);
    }

    public static Trap teleport(String id, Position position) {
        return new Trap(id, TELEPORT, position, false);
    }

    public static Trap weakness(String id, Position position) {
        return new Trap(id, WEAKNESS, position, false);
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

    public boolean isTriggered() {
        return triggered;
    }

    public Trap trigger() {
        return new Trap(id, type, position, true);
    }
}
