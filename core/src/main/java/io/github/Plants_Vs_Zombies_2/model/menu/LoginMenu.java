package io.github.Plants_Vs_Zombies_2.model.menu;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.user.User;

public class LoginMenu extends Menu {
    private boolean stayLoggedIn;
    private User tempUser;

    @Override
    public void exit() {
        App.getInstance().changeMenu(new SignUpMenu());
    }

    @Override
    public String getName() {
        return "login";
    }

    public boolean isCorrectPassword(String username, String password) {
        User user = UserManager.getUserByUsername(username);
        return user != null && user.doesMatchPassword(password);
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    public void login(String username) {
        User user = UserManager.getUserByUsername(username);
        App.getInstance().setLoggedInUser(user);
    }

    public boolean isCorrectEmail(String username, String email) {
        User user = UserManager.getUserByUsername(username);
        return user != null && user.doesMatchEmail(email);
    }

    public void setTempUser(User tempUser) {
        this.tempUser = tempUser;
    }

    public User getTempUser() {
        return tempUser;
    }

    public void setTempUserByName(String username) {
        setTempUser(UserManager.getUserByUsername(username));
    }

    public boolean isCorrectAnswer(String answer, User user) {
        return user != null && user.isCorrectSecurityAnswer(answer);
    }
}
