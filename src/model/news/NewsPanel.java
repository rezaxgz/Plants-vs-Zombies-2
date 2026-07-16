package model.news;

import java.util.ArrayList;
import java.util.List;

public class NewsPanel {
    private List<News> newsList;

    public NewsPanel() {
        this.newsList = new ArrayList<>();
    }

    public void addNews(News news) {
        newsList.add(news);
    }

    public List<News> getAllNews() {
        return newsList;
    }

    public List<News> getUnreadNews() {
        List<News> unread = new ArrayList<>();
        for (News n : newsList) {
            if (!n.isHasRead()) {
                unread.add(n);
            }
        }
        return unread;
    }

    public void markAllAsRead() {
        for (News n : newsList) {
            n.setHasRead(true);
        }
    }

    public boolean hasUnreadNews() {
        for (News n : newsList) {
            if (!n.isHasRead()) {
                return true;
            }
        }
        return false;
    }
}