package controller;

import java.util.regex.Matcher;

import model.CommandResult;
import model.validation.ProjectSelfTest;
import model.validation.ProjectSelfTestReport;

/**
 * Runs deterministic regression checks from the main menu.
 */
public final class ProjectValidationController {
    private ProjectValidationController() {
    }

    public static CommandResult handleRunChecks(
            Matcher matcher) {
        ProjectSelfTestReport report =
                ProjectSelfTest.runAll();
        if (report.isSuccessful()) {
            return CommandResult.success(report.format());
        }
        return CommandResult.error(report.format());
    }
}
