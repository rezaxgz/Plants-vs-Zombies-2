package io.github.some_example_name.commands;

import java.util.regex.Matcher;

@FunctionalInterface
public interface CommandAction<R> {
    R execute(Matcher matcher);
}