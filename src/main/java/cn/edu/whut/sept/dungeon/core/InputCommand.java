package cn.edu.whut.sept.dungeon.core;

public final class InputCommand {
    public enum Type {
        NEW_GAME,
        LOAD,
        MOVE,
        INTERACT,
        ANSWER,
        INVENTORY,
        SAVE_AND_QUIT,
        UNKNOWN
    }

    private final Type type;
    private final Direction direction;
    private final long seed;
    private final String answer;
    private final char rawInput;

    private InputCommand(Type type, Direction direction, long seed, String answer, char rawInput) {
        this.type = type;
        this.direction = direction;
        this.seed = seed;
        this.answer = answer;
        this.rawInput = rawInput;
    }

    public static InputCommand newGame(long seed) {
        return new InputCommand(Type.NEW_GAME, null, seed, null, '\0');
    }

    public static InputCommand saveAndQuit() {
        return new InputCommand(Type.SAVE_AND_QUIT, null, 0L, null, '\0');
    }

    public static InputCommand answer(String answer) {
        return new InputCommand(Type.ANSWER, null, 0L, answer, '\0');
    }

    public static InputCommand fromKey(char input) {
        char lowerInput = Character.toLowerCase(input);
        switch (lowerInput) {
            case 'l':
                return new InputCommand(Type.LOAD, null, 0L, null, lowerInput);
            case 'w':
                return new InputCommand(Type.MOVE, Direction.NORTH, 0L, null, lowerInput);
            case 'a':
                return new InputCommand(Type.MOVE, Direction.WEST, 0L, null, lowerInput);
            case 's':
                return new InputCommand(Type.MOVE, Direction.SOUTH, 0L, null, lowerInput);
            case 'd':
                return new InputCommand(Type.MOVE, Direction.EAST, 0L, null, lowerInput);
            case 'e':
                return new InputCommand(Type.INTERACT, null, 0L, null, lowerInput);
            case 'i':
                return new InputCommand(Type.INVENTORY, null, 0L, null, lowerInput);
            default:
                return new InputCommand(Type.UNKNOWN, null, 0L, null, lowerInput);
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

    public String getAnswer() {
        return answer;
    }

    public char getRawInput() {
        return rawInput;
    }
}
