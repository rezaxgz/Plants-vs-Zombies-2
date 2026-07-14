package commands;

import controller.GreenhouseMenuController;
import model.CommandResult;
import java.util.regex.Matcher;

public enum GreenhouseMenuCommand implements Command<CommandResult> {
    SHOW_GREENHOUSE("^show greenhouse$", GreenhouseMenuController::handleShowGreenhouse),
    PLANT_POT("^plant pot at \\((?<x>[1-5]), (?<y>[1-4])\\)$", GreenhouseMenuController::handlePlantPot),
    COLLECT_PLANT("^collect \\((?<x>[1-5]), (?<y>[1-4])\\)$", GreenhouseMenuController::handleCollect),
    GROW_PLANT("^grow \\((?<x>[1-5]), (?<y>[1-4])\\)$", GreenhouseMenuController::handleGrow),
    UNLOCK_POT("^unlock pot at \\((?<x>[1-5]), (?<y>[1-4])\\)$", GreenhouseMenuController::handleUnlock);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    GreenhouseMenuCommand(String pattern, CommandAction<CommandResult> action) {
        this.pattern = pattern;
        this.action = action;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public CommandAction<CommandResult> getAction() {
        return action;
    }
}