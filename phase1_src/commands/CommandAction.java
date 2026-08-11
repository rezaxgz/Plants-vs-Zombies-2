package commands;

import java.util.regex.Matcher;

@FunctionalInterface
public interface CommandAction<R> {
    R execute(Matcher matcher);
}