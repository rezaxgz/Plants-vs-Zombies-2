package io.github.some_example_name.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.some_example_name.view.CommandResultView;

public class CommandResult {
    private final String message;
    private final boolean isSuccsesful;
    private final List<String> preCommandResults = new ArrayList<>();
    private final List<String> postCommandResults = new ArrayList<>();

    private CommandResult(String message, boolean isSuccsesful) {
        this.message = message == null ? "" : message;
        this.isSuccsesful = isSuccsesful;
    }

    public static CommandResult success(String message) {
        return new CommandResult(message, true);
    }

    public static CommandResult error(String message) {
        return new CommandResult(message, false);
    }

    public CommandResult addPreCommandResult(String result) {
        addNonBlank(preCommandResults, result);
        return this;
    }

    public CommandResult addPreCommandResults(Collection<String> results) {
        addAllNonBlank(preCommandResults, results);
        return this;
    }

    public CommandResult addPostCommandResult(String result) {
        addNonBlank(postCommandResults, result);
        return this;
    }

    public CommandResult addPostCommandResults(Collection<String> results) {
        addAllNonBlank(postCommandResults, results);
        return this;
    }

    public String getMessage() {
        return CommandResultView.buildMessage(
                preCommandResults, message, postCommandResults);
    }

    public boolean isSuccsesful() {
        return isSuccsesful;
    }

    public List<String> getPreCommandResults() {
        return List.copyOf(preCommandResults);
    }

    public List<String> getPostCommandResults() {
        return List.copyOf(postCommandResults);
    }

    private static void addAllNonBlank(List<String> destination, Collection<String> results) {
        if (results == null) {
            return;
        }
        for (String result : results) {
            addNonBlank(destination, result);
        }
    }

    private static void addNonBlank(List<String> destination, String result) {
        if (result != null && !result.isBlank()) {
            destination.add(result);
        }
    }
}
