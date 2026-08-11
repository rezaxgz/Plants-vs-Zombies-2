package io.github.Plants_Vs_Zombies_2.commands;

import java.util.regex.Matcher;

@FunctionalInterface
public interface CommandAction<R> {
    R execute(Matcher matcher);
}