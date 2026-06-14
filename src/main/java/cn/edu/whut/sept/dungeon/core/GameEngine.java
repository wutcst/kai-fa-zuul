package cn.edu.whut.sept.dungeon.core;

/**
 * Entry point for Campus Dungeon rules. GUI and tests should both drive the
 * game through this engine instead of mutating state directly.
 */
public class GameEngine {
    private GameState state;

    public GameEngine() {
        this.state = GameState.initial();
    }

    public GameResult playWithInputString(String input) {
        GameEngine engine = new GameEngine();
        if (input == null || input.isEmpty()) {
            return GameResult.of(engine.state.withMessage("No input."));
        }

        int index = 0;
        while (index < input.length()) {
            char current = Character.toLowerCase(input.charAt(index));
            if (current == 'n') {
                SeedParseResult seedResult = parseSeed(input, index + 1);
                engine.handleInput(InputCommand.newGame(seedResult.seed));
                index = seedResult.nextIndex;
                continue;
            }
            if (current == ':' && index + 1 < input.length()
                    && Character.toLowerCase(input.charAt(index + 1)) == 'q') {
                engine.handleInput(InputCommand.saveAndQuit());
                index += 2;
                continue;
            }

            engine.handleInput(InputCommand.fromKey(current));
            index++;
        }

        return GameResult.of(engine.state);
    }

    public GameState handleInput(InputCommand command) {
        if (command == null) {
            state = state.withMessage("Unknown input.");
            return state;
        }

        switch (command.getType()) {
            case NEW_GAME:
                state = GameState.newGame(command.getSeed());
                break;
            case LOAD:
                state = state.withMessage("No saved game.");
                break;
            case MOVE:
                state = state.withMessage("Movement is not implemented yet: " + command.getDirection().name());
                break;
            case INTERACT:
                state = state.withMessage("Interaction is not implemented yet.");
                break;
            case INVENTORY:
                state = state.withMessage("Inventory is empty.");
                break;
            case SAVE_AND_QUIT:
                state = state.markSaveRequested().markExited().withMessage("Save requested.");
                break;
            case UNKNOWN:
            default:
                state = state.withMessage("Unknown input: " + command.getRawInput());
                break;
        }
        return state;
    }

    public GameState getState() {
        return state;
    }

    private SeedParseResult parseSeed(String input, int start) {
        StringBuilder seedText = new StringBuilder();
        int index = start;
        while (index < input.length()) {
            char current = Character.toLowerCase(input.charAt(index));
            if (current == 's') {
                index++;
                break;
            }
            if (!Character.isDigit(current)) {
                return new SeedParseResult(0L, index + 1);
            }
            seedText.append(current);
            index++;
        }

        if (seedText.length() == 0) {
            return new SeedParseResult(0L, index);
        }

        try {
            return new SeedParseResult(Long.parseLong(seedText.toString()), index);
        } catch (NumberFormatException exception) {
            return new SeedParseResult(0L, index);
        }
    }

    private static final class SeedParseResult {
        private final long seed;
        private final int nextIndex;

        private SeedParseResult(long seed, int nextIndex) {
            this.seed = seed;
            this.nextIndex = nextIndex;
        }
    }
}
