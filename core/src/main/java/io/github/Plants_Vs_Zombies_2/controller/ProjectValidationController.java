package io.github.Plants_Vs_Zombies_2.controller;

import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.validation.ProjectSelfTest;
import io.github.Plants_Vs_Zombies_2.model.validation.ProjectSelfTestReport;

/**
 * Runs deterministic regression checks from the main menu.
 */
public final class ProjectValidationController {
    private ProjectValidationController() {
    }

    public static CommandResult handleRunChecks(
            Matcher matcher) {
        ProjectSelfTestReport report = ProjectSelfTest.runAll();
        if (report.isSuccessful()) {
            return CommandResult.success(report.format());
        }
        return CommandResult.error(report.format());
    }
}
