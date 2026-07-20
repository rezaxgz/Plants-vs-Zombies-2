package commands;

import controller.ShopMenuController;
import model.CommandResult;

public enum ShopMenuCommand implements Command<CommandResult> {
    SHOP_LIST("^shop list$", ShopMenuController::handleShopList),
    SHOP_DAILY("^shop daily$", ShopMenuController::handleShopDaily),
    SHOP_BUY("^shop buy -i (?<id>\\S+) -n (?<count>\\d+)(?: -t (?<plantType>[a-zA-Z0-9_]+))?$",
            ShopMenuController::handleShopBuy);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    ShopMenuCommand(String pattern, CommandAction<CommandResult> action) {
        this.pattern = pattern;
        this.action = action;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public CommandAction<CommandResult> getAction() {
        return action;
    }
}