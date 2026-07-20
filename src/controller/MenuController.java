package controller;

import java.util.Locale;
import java.util.regex.Matcher;

import model.App;
import model.CommandResult;
import model.menu.CollectionMenu;
import model.menu.GameMenu;
import model.menu.LoginMenu;
import model.menu.MainMenu;
import model.menu.Menu;
import model.menu.NetworkMenu;
import model.menu.NewsMenu;
import model.menu.ProfileMenu;
import model.news.NewsMessages;
import model.menu.SettingsMenu;
import model.menu.SignUpMenu;
import model.menu.TravelLogMenu;
import model.menu.GreenhouseMenu;
import model.menu.LeaderboardMenu;

public final class MenuController {
    private MenuController() {
    }

    public static CommandResult handleEnter(Matcher matcher) {
        App app = App.getInstance();
        Menu currentMenu = app.getCurrentMenu();
        String requestedName = normalizeMenuName(matcher.group("menuName"));

        if (!isKnownMenu(requestedName)) {
            return CommandResult.error("menu does not exist!");
        }
        if (currentMenu.getName().equalsIgnoreCase(requestedName) ||
                (currentMenu.getName().equals("GreenhouseMenu") && requestedName.equals("greenhouse"))) {
            return CommandResult.error("you are already in the " + requestedName + " menu!");
        }

        Menu destination = createDestination(currentMenu, requestedName);
        if (destination == null) {
            return CommandResult
                    .error("cannot enter the " + requestedName + " menu from the " + currentMenu.getName() + " menu!");
        }

        app.changeMenu(destination);

        String successMsg = "entered " + destination.getName() + " menu";
        if (destination instanceof MainMenu) {
            successMsg = appendNewsBadge(successMsg, app);
        }

        return CommandResult.success(successMsg);
    }

    public static CommandResult handleShowCurrent(Matcher matcher) {
        return CommandResult.success(App.getInstance().getCurrentMenu().getName());
    }

    public static CommandResult handleExit(Matcher matcher) {
        App app = App.getInstance();
        Menu currentMenu = app.getCurrentMenu();
        if (currentMenu instanceof MainMenu) {
            return CommandResult.error("use menu logout to exit the main menu!");
        }

        String exitedMenuName = currentMenu.getName();
        currentMenu.exit();
        if (!app.isRunning()) {
            return CommandResult.success("program ended");
        }

        String message = "exited " + exitedMenuName + " menu"
                + System.lineSeparator() + "entered "
                + app.getCurrentMenu().getName() + " menu";
        if (app.getCurrentMenu() instanceof MainMenu) {
            message = appendNewsBadge(message, app);
        }
        return CommandResult.success(message);
    }

    private static String appendNewsBadge(String message, App app) {
        String badge = NewsMessages.unreadBadge(app.getLoggedInUser());
        if (badge.isBlank()) {
            return message;
        }
        return message + System.lineSeparator() + badge;
    }

    private static Menu createDestination(Menu currentMenu, String requestedName) {
        if (currentMenu instanceof SignUpMenu && "login".equals(requestedName)) {
            return new LoginMenu();
        }
        if (currentMenu instanceof LoginMenu && "signup".equals(requestedName)) {
            return new SignUpMenu();
        }
        if (currentMenu instanceof MainMenu) {
            return createMainChild(requestedName);
        }
        if (currentMenu instanceof GameMenu) {
            if ("collection".equals(requestedName)) {
                return new CollectionMenu((GameMenu) currentMenu);
            }
            if ("main".equals(requestedName)) {
                return new MainMenu();
            }
        }
        if (currentMenu instanceof CollectionMenu && "game".equals(requestedName)) {
            return ((CollectionMenu) currentMenu).getParentGameMenu();
        }
        if (isMainChild(currentMenu) && "main".equals(requestedName)) {
            return new MainMenu();
        }
        return null;
    }

    private static Menu createMainChild(String requestedName) {
        if ("game".equals(requestedName))
            return new GameMenu();
        if ("settings".equals(requestedName))
            return new SettingsMenu();
        if ("network".equals(requestedName))
            return new NetworkMenu();
        if ("news".equals(requestedName))
            return new NewsMenu();
        if ("profile".equals(requestedName))
            return new ProfileMenu();
        if ("greenhouse".equals(requestedName))
            return new GreenhouseMenu();
        if ("leaderboard".equals(requestedName)) 
            return new LeaderboardMenu(); 
        if ("travellog".equals(requestedName)) 
            return new TravelLogMenu(); 
        if ("collection".equals(requestedName)) 
            return new CollectionMenu(); 
        return null;
    }

    private static boolean isMainChild(Menu menu) {
        return menu instanceof SettingsMenu || menu instanceof NetworkMenu || menu instanceof NewsMenu
                || menu instanceof ProfileMenu || menu instanceof GreenhouseMenu
                || menu instanceof LeaderboardMenu || menu instanceof TravelLogMenu ||  menu instanceof CollectionMenu;
    }

    private static boolean isKnownMenu(String menuName) {
        return "signup".equals(menuName) || "login".equals(menuName) || "main".equals(menuName)
                || "game".equals(menuName) || "collection".equals(menuName) || "settings".equals(menuName)
                || "network".equals(menuName) || "news".equals(menuName) || "profile".equals(menuName)
                || "greenhouse".equals(menuName)
                || "leaderboard".equals(menuName) || "travellog".equals(menuName);
    }

    private static String normalizeMenuName(String menuName) {
        String normalized = menuName.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        if (normalized.endsWith("menu")) {
            normalized = normalized.substring(0, normalized.length() - "menu".length());
        }
        if ("signup".equals(normalized) || "registration".equals(normalized)) {
            return "signup";
        }
        return normalized;
    }
}