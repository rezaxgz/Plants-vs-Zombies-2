package model.menu;

import model.App;
import model.auth.UserManager;
import model.user.User;

public class LoginMenu extends Menu {
    private boolean StayLoggedIn;

    private User tempUser;

    @Override
    public void exit() {
        App.getInstance().changeMenu(new MainMenu());
    }

    @Override
    public String getName() {
        return "login";
    }

    public boolean isCorrectPassword(String username, String password) {
        return UserManager.getUserByUsername(username).doesMatchPassword(password);
    }

    public boolean isStayLoggedIn() {
        return StayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        StayLoggedIn = stayLoggedIn;
    }

    public void login(String username) {
        User user = UserManager.getUserByUsername(username);
        App.getInstance().setLoggedInUser(user);
    }

    public boolean isCorrectEmail(String username, String email) {
        return UserManager.getUserByUsername(username).doesMatchEmail(email);
    }

    public void setTempUser(User tempUser) {
        this.tempUser = tempUser;
    }

    public User getTempUser() {
        return tempUser;
    }

    public void setTempUserByName(String username) {
        this.setTempUser(UserManager.getUserByUsername(username));
    }

    public boolean isCorrectAnswer(String answer, User user) {
        return user.isCorrectSecurityAnswer(answer);
    }
}
