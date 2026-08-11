package io.github.Plants_Vs_Zombies_2.model.news;

import io.github.Plants_Vs_Zombies_2.model.user.User;

public final class NewsMessages {
    private NewsMessages() {
    }

    public static String unreadBadge(User user) {
        if (user == null) {
            return "";
        }
        int unreadCount = user.getNewsPanel().getUnreadCount();
        if (unreadCount == 0) {
            return "";
        }
        String itemWord = unreadCount == 1 ? "item" : "items";
        return "[RED NEWS BADGE] " + unreadCount + " unread news "
                + itemWord + ". Use 'menu enter news', then "
                + "'menu news show-unread'.";
    }
}
