package cn.edu.whut.sept.dungeon.core;

public final class InputCommand {
    public enum Type {
        NEW_GAME,
        LOAD,
        MOVE,
        INTERACT,
        INVENTORY,
        SAVE_AND_QUIT,
        UNKNOWN
    }

    private final Type type;
    private final Direction direction;
    private final long seed;
    private final char rawInput;

    private InputCommand(Type type, Direction direction, long seed, char rawInput) {
        this.type = type;
        this.direction = direction;
        this.seed = seed;
        this.rawInput = rawInput;
    }

    public static InputCommand newGame(long seed) {
        return new InputCommand(Type.NEW_GAME, null, seed, '\0');
    }

    public static InputCommand saveAndQuit() {
        return new InputCommand(Type.SAVE_AND_QUIT, null, 0L, '\0');
    }

    public static InputCommand fromKey(char input) {
        char lowerInput = Character.toLowerCase(input);
        switch (lowerInput) {
            case 'l':
                return new InputCommand(Type.LOAD, null, 0L, lowerInput);
            case 'w':
                return new InputCommand(Type.MOVE, Direction.NORTH, 0L, lowerInput);
            case 'a':
                return new InputCommand(Type.MOVE, Direction.WEST, 0L, lowerInput);
            case 's':
                return new InputCommand(Type.MOVE, Direction.SOUTH, 0L, lowerInput);
            case 'd':
                return new InputCommand(Type.MOVE, Direction.EAST, 0L, lowerInput);
            case 'e':
                return new InputCommand(Type.INTERACT, null, 0L, lowerInput);
            case 'i':
                return new InputCommand(Type.INVENTORY, null, 0L, lowerInput);
            default:
                return new InputCommand(Type.UNKNOWN, null, 0L, lowerInput);
        }
    }

    public Type getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getSeed() {
        return seed;
    }

    public char getRawInput() {
        return rawInput;
    }
}
