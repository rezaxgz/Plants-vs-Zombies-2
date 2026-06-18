package model.menu;

import model.App;
import model.user.User;

public class SignUpMenu extends Menu {

    private User tempUser = null;

    @Override
    public void exit() {
        App.getInstance().changeMenu(new LoginMenu());
    }

    @Override
    public String getName() {
        return "signup";
    }

    public User getTempUser() {
        return tempUser;
    }

    public void setTempUser(User tempUser) {
        this.tempUser = tempUser;
    }

}
