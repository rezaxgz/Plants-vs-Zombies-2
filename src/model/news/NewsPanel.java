package model.news;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewsPanel {
    private final List<News> newsList;

    public NewsPanel() {
        this.newsList = new ArrayList<>();
    }

    public void addNews(News news) {
        if (news == null) {
            throw new IllegalArgumentException("news cannot be null");
        }
        newsList.add(news);
    }

    public boolean addNewsIfAbsent(News news) {
        if (news == null) {
            throw new IllegalArgumentException("news cannot be null");
        }
        if (containsSameNews(news)) {
            return false;
        }
        newsList.add(news);
        return true;
    }

    private boolean containsSameNews(News candidate) {
        for (News news : newsList) {
            if (news.getTitle().equals(candidate.getTitle())
                    && news.getDescription().equals(candidate.getDescription())) {
                return true;
            }
        }
        return false;
    }

    public List<News> getAllNews() {
        return List.copyOf(newsList);
    }

    public List<News> getUnreadNews() {
        List<News> unread = new ArrayList<>();
        for (News news : newsList) {
            if (!news.isHasRead()) {
                unread.add(news);
            }
        }
        return List.copyOf(unread);
    }

    public int getUnreadCount() {
        int count = 0;
        for (News news : newsList) {
            if (!news.isHasRead()) {
                count++;
            }
        }
        return count;
    }

    public void markAsRead(Collection<News> newsToMark) {
        if (newsToMark == null) {
            return;
        }
        for (News news : newsToMark) {
            if (newsList.contains(news)) {
                news.setHasRead(true);
            }
        }
    }

    public void markAllAsRead() {
        markAsRead(newsList);
    }

    public boolean hasUnreadNews() {
        return getUnreadCount() > 0;
    }
}
