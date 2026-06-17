package commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Command<R> {
    String getPattern();

    CommandAction<R> getAction();

    default Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile(getPattern()).matcher(input);

        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
