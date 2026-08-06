package io.github.some_example_name.controller;

import java.util.regex.Matcher;

import io.github.some_example_name.model.CommandResult;
import io.github.some_example_name.model.validation.ProjectSelfTest;
import io.github.some_example_name.model.validation.ProjectSelfTestReport;

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
