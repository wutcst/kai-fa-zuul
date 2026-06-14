package cn.edu.whut.sept.dungeon.entity;

import cn.edu.whut.sept.dungeon.world.Position;

public final class Npc {
    private final String id;
    private final String name;
    private final Position position;

    public Npc(String id, String name, Position position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }
}
