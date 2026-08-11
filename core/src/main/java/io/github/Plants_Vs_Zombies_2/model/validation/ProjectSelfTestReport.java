package io.github.Plants_Vs_Zombies_2.model.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.view.validation.ProjectSelfTestView;

/**
 * Immutable result of the built-in project regression checks.
 */
public final class ProjectSelfTestReport {
    private final int passedCount;
    private final int totalCount;
    private final List<String> lines;

    ProjectSelfTestReport(int passedCount,
            int totalCount, List<String> lines) {
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.lines = Collections.unmodifiableList(
                new ArrayList<>(lines));
    }

    public boolean isSuccessful() {
        return passedCount == totalCount;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<String> getLines() {
        return lines;
    }

    public String format() {
        return ProjectSelfTestView.formatReport(
                passedCount, totalCount, lines);
    }
}
