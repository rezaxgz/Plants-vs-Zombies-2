package io.github.Plants_Vs_Zombies_2.controller;

import java.util.List;
import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.news.News;
import io.github.Plants_Vs_Zombies_2.model.user.User;

public final class NewsMenuController {

    private NewsMenuController() {
    }

    public static CommandResult handleShowUnread(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("No user is currently logged in!");
        }

        List<News> unread = user.getNewsPanel().getUnreadNews();
        if (unread.isEmpty()) {
            return CommandResult.success("You have no unread news.");
        }

        String message = formatNews("=== UNREAD NEWS ===", unread, true);
        user.getNewsPanel().markAsRead(unread);
        UserManager.saveAllUsers();
        return CommandResult.success(message);
    }

    public static CommandResult handleShowAll(Matcher matcher) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return CommandResult.error("No user is currently logged in!");
        }

        List<News> allNews = user.getNewsPanel().getAllNews();
        if (allNews.isEmpty()) {
            return CommandResult.success("No news available.");
        }

        return CommandResult.success(
                formatNews("=== ALL NEWS ===", allNews, false));
    }

    private static String formatNews(String heading, List<News> news,
            boolean forceNewMarker) {
        StringBuilder output = new StringBuilder(heading);
        for (News item : news) {
            output.append(System.lineSeparator());
            if (forceNewMarker || !item.isHasRead()) {
                output.append("[NEW] ");
            }
            output.append(item.getTitle())
                    .append(": ")
                    .append(item.getDescription());
        }
        return output.toString();
    }
}
