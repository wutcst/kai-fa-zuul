package cn.edu.whut.sept.zuul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Room
{
    private String id;
    private String description;
    private HashMap<String, Room> exits;        // stores exits of this room.
    private List<Item> items;
    private List<Npc> npcs;
    private boolean teleportRoom;

    public Room(String description)
    {
        this(description, description);
    }

    public Room(String id, String description)
    {
        this.description = description;
        this.id = id;
        exits = new HashMap<String, Room>();
        items = new ArrayList<Item>();
        npcs = new ArrayList<Npc>();
        teleportRoom = false;
    }

    public void setExit(String direction, Room neighbor)
    {
        exits.put(direction, neighbor);
    }

    public String getId()
    {
        return id;
    }

    public String getShortDescription()
    {
        return description;
    }

    public String getLongDescription()
    {
        String text = "You are " + description + ".\n" + getExitString() + "\n" + getItemString() + "\n" + getNpcString();
        if(teleportRoom) {
            text += "\nA teleport gate is humming in this room.";
        }
        return text;
    }

    private String getExitString()
    {
        String returnString = "Exits:";
        Set<String> keys = exits.keySet();
        for(String exit : keys) {
            returnString += " " + exit;
        }
        return returnString;
    }

    private String getItemString()
    {
        if(items.isEmpty()) {
            return "Items: none";
        }

        StringBuilder builder = new StringBuilder("Items:");
        for(Item item : items) {
            builder.append("\n - ").append(item.getDisplayText());
        }
        return builder.toString();
    }

    private String getNpcString()
    {
        if(npcs.isEmpty()) {
            return "People: none";
        }

        StringBuilder builder = new StringBuilder("People:");
        for(Npc npc : npcs) {
            builder.append("\n - ").append(npc.getDisplayText());
        }
        return builder.toString();
    }

    public Room getExit(String direction)
    {
        return exits.get(direction);
    }

    public Map<String, Room> getExits()
    {
        return Collections.unmodifiableMap(exits);
    }

    public void addItem(Item item)
    {
        items.add(item);
    }

    public Item removeItem(String itemName)
    {
        if(itemName == null) {
            return null;
        }

        for(Iterator<Item> iterator = items.iterator(); iterator.hasNext(); ) {
            Item item = iterator.next();
            if(item.getName().equals(itemName.toLowerCase())) {
                iterator.remove();
                return item;
            }
        }
        return null;
    }

    public Item findItem(String itemName)
    {
        if(itemName == null) {
            return null;
        }

        for(Item item : items) {
            if(item.getName().equals(itemName.toLowerCase())) {
                return item;
            }
        }
        return null;
    }

    public List<Item> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    public void addNpc(Npc npc)
    {
        npcs.add(npc);
    }

    public Npc findNpc(String npcName)
    {
        if(npcName == null) {
            return null;
        }

        for(Npc npc : npcs) {
            if(npc.getName().equals(npcName.toLowerCase())) {
                return npc;
            }
        }
        return null;
    }

    public List<Npc> getNpcs()
    {
        return Collections.unmodifiableList(npcs);
    }

    public boolean isTeleportRoom()
    {
        return teleportRoom;
    }

    public void setTeleportRoom(boolean teleportRoom)
    {
        this.teleportRoom = teleportRoom;
    }
}
