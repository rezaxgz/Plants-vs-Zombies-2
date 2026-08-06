package io.github.some_example_name.model.menu;

import io.github.some_example_name.model.App;
import io.github.some_example_name.model.user.User;

public class SignUpMenu extends Menu {

    private User tempUser;

    @Override
    public void exit() {
        App.getInstance().stop();
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
