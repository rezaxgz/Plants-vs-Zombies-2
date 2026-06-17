package controller;

import java.util.regex.Matcher;

import model.CommandResult;

public class SignupMenuController {
    public static CommandResult handleRegister(Matcher matcher) {
        return CommandResult.success("ok");
    }
}
