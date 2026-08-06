package io.github.some_example_name.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines a command's pre-results, main message, and post-results.
 */
public final class CommandResultView {
    private CommandResultView() {
    }

    public static String buildMessage(
            List<String> preCommandResults,
            String message,
            List<String> postCommandResults) {
        List<String> outputLines = new ArrayList<>();
        outputLines.addAll(preCommandResults);
        addNonBlank(outputLines, message);
        outputLines.addAll(postCommandResults);
        return String.join(System.lineSeparator(), outputLines);
    }

    private static void addNonBlank(
            List<String> destination, String result) {
        if (result != null && !result.isBlank()) {
            destination.add(result);
        }
    }
}
