package io.github.Plants_Vs_Zombies_2.commands;

import io.github.Plants_Vs_Zombies_2.controller.ProfileMenuController;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;

public enum ProfileMenuCommand implements Command<CommandResult> {
    CHANGE_USERNAME(
            "^menu\\s+profile\\s+change-username\\s+-u\\s+(?<username>\\S+)$",
            ProfileMenuController::handleChangeUsername),
    CHANGE_NICKNAME(
            "^menu\\s+profile\\s+change-nickname\\s+-u\\s+(?<nickname>.+)$",
            ProfileMenuController::handleChangeNickname),
    CHANGE_EMAIL(
            "^menu\\s+profile\\s+change-email\\s+-e\\s+(?<email>\\S+)$",
            ProfileMenuController::handleChangeEmail),
    CHANGE_PASSWORD(
            "^menu\\s+profile\\s+change-password\\s+-p\\s+(?<newPassword>\\S+)"
                    + "\\s+-o\\s+(?<oldPassword>\\S+)$",
            ProfileMenuController::handleChangePassword),
    SHOW_INFO(
            "^menu\\s+profile\\s+show-info$",
            ProfileMenuController::handleShowInfo);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    ProfileMenuCommand(String pattern,
            CommandAction<CommandResult> action) {
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
