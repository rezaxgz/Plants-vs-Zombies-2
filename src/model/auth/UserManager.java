package model.auth;

import java.util.ArrayList;
import java.util.List;

import model.user.User;

public class UserManager {
    private static final List<User> users = new ArrayList<>();

    public static List<User> loadAllUsers() {
        return users;
    }

    public static boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    public static void addUserToDatabase(User user) {
        if (user == null) {
            return;
        }

        if (!usernameExists(user.getUsername())) {
            users.add(user);
        }
    }

    public static User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        return null;
    }
}