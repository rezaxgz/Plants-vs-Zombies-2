package commands;

import controller.ShopMenuController;
import model.CommandResult;

public enum ShopMenuCommand implements Command<CommandResult> {
    SHOP_LIST("^shop\\s+list$",
            ShopMenuController::handleShopList),
    SHOP_DAILY("^shop\\s+daily$",
            ShopMenuController::handleShopDaily),
    SHOP_BUY("^shop\\s+buy\\s+-i\\s+(?<id>\\S+)"
            + "\\s+-n\\s+(?<count>\\d+)"
            + "(?:\\s+-t\\s+(?<plantType>.+?))?\\s*$",
            ShopMenuController::handleShopBuy);

    private final String pattern;
    private final CommandAction<CommandResult> action;

    ShopMenuCommand(String pattern,
            CommandAction<CommandResult> action) {
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
