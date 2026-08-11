package io.github.Plants_Vs_Zombies_2.model.menu;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.user.User;

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
