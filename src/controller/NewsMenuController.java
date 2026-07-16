package controller;

import java.util.List;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.news.News;
import model.user.User;

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

        StringBuilder sb = new StringBuilder("=== UNREAD NEWS ===\n");
        for (News n : unread) {
            sb.append("[NEW] ").append(n.getTitle()).append(": ").append(n.getDescription()).append("\n");
        }

        // Mark all unread news as read after showing them
        user.getNewsPanel().markAllAsRead();
        return CommandResult.success(sb.toString().trim());
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

        StringBuilder sb = new StringBuilder("=== ALL NEWS ===\n");
        for (News n : allNews) {
            String marker = n.isHasRead() ? "" : "[NEW] ";
            sb.append(marker).append(n.getTitle()).append(": ").append(n.getDescription()).append("\n");
        }

        user.getNewsPanel().markAllAsRead();
        return CommandResult.success(sb.toString().trim());
    }
}