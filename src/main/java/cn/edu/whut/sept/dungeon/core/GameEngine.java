package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.io.SaveManager;

/**
 * Entry point for Campus Dungeon rules. GUI and tests should both drive the
 * game through this engine instead of mutating state directly.
 */
public class GameEngine {
    private final SaveManager saveManager;
    private GameState state;

    public GameEngine() {
        this(new SaveManager());
    }

    public GameEngine(SaveManager saveManager) {
        this.saveManager = saveManager;
        this.state = GameState.initial();
    }

    public GameResult playWithInputString(String input) {
        state = GameState.initial();
        if (input == null || input.isEmpty()) {
            state = state.withMessage("No input.");
            return GameResult.of(state);
        }

        int index = 0;
        while (index < input.length()) {
            char current = Character.toLowerCase(input.charAt(index));
            if (current == 'n') {
                SeedParseResult seedResult = parseSeed(input, index + 1);
                handleInput(InputCommand.newGame(seedResult.seed));
                index = seedResult.nextIndex;
                continue;
            }
            if (current == ':' && index + 1 < input.length()
                    && Character.toLowerCase(input.charAt(index + 1)) == 'q') {
                handleInput(InputCommand.saveAndQuit());
                index += 2;
                continue;
            }
            if (current == '!' && input.startsWith("!answer(", index)) {
                int closeIndex = input.indexOf(')', index);
                if (closeIndex > index) {
                    handleInput(InputCommand.answer(input.substring(index + 8, closeIndex)));
                    index = closeIndex + 1;
                    continue;
                }
            }

            handleInput(InputCommand.fromKey(current));
            index++;
        }

        return GameResult.of(state);
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
                state = saveManager.load();
                break;
            case MOVE:
                state = state.movePlayer(command.getDirection());
                break;
            case INTERACT:
                state = state.interact();
                break;
            case ANSWER:
                state = state.answer(command.getAnswer());
                break;
            case INVENTORY:
                state = state.describeInventory();
                break;
            case SAVE_AND_QUIT:
                state = state.markSaveRequested().markExited().withMessage("Save requested.");
                saveManager.save(state);
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
