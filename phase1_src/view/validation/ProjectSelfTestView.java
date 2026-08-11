package view.validation;

import java.util.List;

/**
 * Builds the textual summary of the built-in regression checks.
 */
public final class ProjectSelfTestView {
    private ProjectSelfTestView() {
    }

    public static String formatReport(
            int passedCount, int totalCount, List<String> lines) {
        StringBuilder output =
                new StringBuilder("Project regression checks");
        for (String line : lines) {
            output.append(System.lineSeparator())
                    .append(line);
        }
        output.append(System.lineSeparator())
                .append("Result: ")
                .append(passedCount)
                .append('/')
                .append(totalCount)
                .append(" checks passed");
        return output.toString();
    }
}
