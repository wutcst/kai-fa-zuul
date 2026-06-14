package cn.edu.whut.sept.zuul;

/**
 * Represents a non-player character that can provide clues.
 */
public class Npc
{
    private final String name;
    private final String description;
    private final String defaultDialogue;

    public Npc(String name, String description, String defaultDialogue)
    {
        if(name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("NPC name must not be empty.");
        }

        this.name = name.toLowerCase();
        this.description = description;
        this.defaultDialogue = defaultDialogue;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getDefaultDialogue()
    {
        return defaultDialogue;
    }

    public String getDisplayText()
    {
        return name + " - " + description;
    }
}
