package controller;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import model.App;
import model.CommandResult;
import model.quest.Quest;
import model.user.User;

public class TravelLogMenuController {

    private static final int QUESTS_PER_PAGE = 5;

    public static CommandResult handlePage(Matcher matcher) {
        int pageNumber = Integer.parseInt(matcher.group("page"));
        User currentUser = App.getInstance().getLoggedInUser();

        if (currentUser == null) {
            return CommandResult.error("You must be logged in to view the Travel Log.");
        }

        List<Quest> activeQuests = currentUser.getQuestProgress().getActiveQuests();
        Collections.sort(activeQuests); // Sorts by Priority

        int totalPages = (int) Math.ceil((double) activeQuests.size() / QUESTS_PER_PAGE);
        if (pageNumber < 1 || (pageNumber > totalPages && totalPages != 0)) {
            return CommandResult.error("Invalid page number.");
        }

        CommandResult result = CommandResult
                .success("--- Travel Log (Page " + pageNumber + " of " + Math.max(1, totalPages) + ") ---");

        int startIndex = (pageNumber - 1) * QUESTS_PER_PAGE;
        int endIndex = Math.min(startIndex + QUESTS_PER_PAGE, activeQuests.size());

        for (int i = startIndex; i < endIndex; i++) {
            Quest q = activeQuests.get(i);
            String questStr = String.format("[%s] %s: %s (Type: %s)",
                    q.getPriority(), q.getName(), q.getInstructions(), q.getType());
            result.addPostCommandResult(questStr);
        }

        if (activeQuests.isEmpty()) {
            result.addPostCommandResult("You have no active quests.");
        }

        return result;
    }
}