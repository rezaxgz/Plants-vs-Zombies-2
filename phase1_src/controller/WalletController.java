package controller;

import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.enums.CurrencyType;
import model.user.User;

/**
 * Commands for displaying and changing the logged-in user's currencies.
 */
public final class WalletController {
    private WalletController() {
    }

    public static CommandResult handleShowCoins(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        return CommandResult.success("coins: " + user.getCoins());
    }

    public static CommandResult handleShowDiamonds(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }
        return CommandResult.success("diamonds: " + user.getDiamonds());
    }

    public static CommandResult handleCheatAddCurrency(Matcher matcher) {
        User user = getLoggedInUser();
        if (user == null) {
            return loginRequired();
        }

        int amount = parseAmount(matcher.group("count"));
        if (amount < 0) {
            return CommandResult.error("currency amount is too large!");
        }
        if (amount == 0) {
            return CommandResult.error("currency amount must be positive!");
        }

        CurrencyType currency = parseCurrency(matcher.group("currency"));
        if (currency == CurrencyType.COIN) {
            return addCoins(user, amount);
        }
        return addDiamonds(user, amount);
    }

    private static int parseAmount(String rawAmount) {
        try {
            return Integer.parseInt(rawAmount);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static CurrencyType parseCurrency(String rawCurrency) {
        String normalized = rawCurrency.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("coin")) {
            return CurrencyType.COIN;
        }
        return CurrencyType.DIAMOND;
    }

    private static CommandResult addCoins(User user, int amount) {
        if (wouldOverflow(user.getCoins(), amount)) {
            return CommandResult.error("coin total is too large!");
        }
        user.addCoins(amount);
        UserManager.saveAllUsers();
        return CommandResult.success("added " + amount + " coins"
                + System.lineSeparator() + "total coins: " + user.getCoins());
    }

    private static CommandResult addDiamonds(User user, int amount) {
        if (wouldOverflow(user.getDiamonds(), amount)) {
            return CommandResult.error("diamond total is too large!");
        }
        user.addDiamonds(amount);
        UserManager.saveAllUsers();
        return CommandResult.success("added " + amount + " diamonds"
                + System.lineSeparator() + "total diamonds: "
                + user.getDiamonds());
    }

    private static boolean wouldOverflow(int currentAmount, int addedAmount) {
        return currentAmount > Integer.MAX_VALUE - addedAmount;
    }

    private static User getLoggedInUser() {
        return App.getInstance().getLoggedInUser();
    }

    private static CommandResult loginRequired() {
        return CommandResult.error("login is required!");
    }
}
