package cn.edu.whut.sept.dungeon.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Inventory {
    private final List<String> itemIds;

    private Inventory(List<String> itemIds) {
        this.itemIds = Collections.unmodifiableList(new ArrayList<String>(itemIds));
    }

    public static Inventory empty() {
        return new Inventory(Collections.<String>emptyList());
    }

    public static Inventory of(List<String> itemIds) {
        return new Inventory(itemIds == null ? Collections.<String>emptyList() : itemIds);
    }

    public Inventory add(String itemId) {
        if (contains(itemId)) {
            return this;
        }
        List<String> next = new ArrayList<String>(itemIds);
        next.add(itemId);
        return new Inventory(next);
    }

    public Inventory remove(String itemId) {
        List<String> next = new ArrayList<String>(itemIds);
        next.remove(itemId);
        return new Inventory(next);
    }

    public boolean contains(String itemId) {
        return itemIds.contains(itemId);
    }

    public boolean containsAll(String... requiredItems) {
        for (String requiredItem : requiredItems) {
            if (!contains(requiredItem)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public String summary() {
        if (itemIds.isEmpty()) {
            return "empty";
        }
        return itemIds.toString();
    }
}
