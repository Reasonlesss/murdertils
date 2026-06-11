package codes.reason.murdertils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameTracker {
    private static final Pattern START = Pattern.compile("Starting the game with:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STARTING_ROLE = Pattern.compile(
            "(\\d+)\\s+(traitors?|accomplices?|fiends?|detectives?|doctors?|innocents?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "You are an? (traitor|accomplice|fiend|detective|doctor|innocent)!",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DISCOVERED = Pattern.compile(
            "☠\\s+([A-Za-z0-9_]{1,16}) was found dead as an? "
                    + "(traitor|accomplice|fiend|detective|doctor|innocent)\\.",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CHAT = Pattern.compile("^⏵\\s*([A-Za-z0-9_]{1,16}):\\s*(.+)$");
    private static final Pattern HEALED = Pattern.compile(
            "❤\\s*Healed By\\s+([A-Za-z0-9_]{1,16})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern END_GAME = Pattern.compile(
            "^(?:"
                    + "⏩\\s*(?:Innocents?|Criminals?) win!"
                    + "|🗡\\s*(?:(?:A|The) fiend .+ is|The fiends \\(.+\\) are)"
                    + " alive, overriding the win condition\\."
                    + ")\\s*Click here to view round information\\.$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SITTING_OUT = Pattern.compile(
            "^▶\\s*You are sitting out from this game\\.$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CRIMINAL_REVEAL = Pattern.compile(
            "^▶\\s*Traitors can see their allies\\. Criminal players in this game:$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CRIMINAL_TEAM = Pattern.compile(
            "^-\\s*(Traitors?|Accomplices?):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SURVIVORS = Pattern.compile("⛨\\s*Survivors:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTUAL_SURVIVORS = Pattern.compile(
            "⛨\\s*Actual survivors:\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TIMER = Pattern.compile(
            "⌚\\s*Time Left:\\s*(\\d{1,2}):(\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    private final GameState state;
    private final CalloutParser calloutParser;

    public GameTracker(GameState state) {
        this.state = state;
        this.calloutParser = new CalloutParser();
    }

    public void handleMessage(Component component, boolean overlay, Minecraft client) {
        String message = sanitize(component.getString());
        if (message.isBlank()) {
            return;
        }

        if (overlay) {
            Matcher healed = HEALED.matcher(message);
            if (healed.find()) {
                state.setMarker(healed.group(1), Role.DOCTOR);
            }
        }

        Matcher start = START.matcher(message);
        if (start.find()) {
            EnumMap<Role, Integer> roles = parseStartingRoles(start.group(1));
            if (!roles.isEmpty()) {
                state.beginGame(roles, System.currentTimeMillis());
                return;
            }
        }

        if (END_GAME.matcher(message).matches()) {
            state.endGame();
            return;
        }

        if (SITTING_OUT.matcher(message).matches()) {
            state.markLocalPlayerDead();
            return;
        }

        if (CRIMINAL_REVEAL.matcher(message).matches()) {
            return;
        }

        Matcher assignment = ASSIGNMENT.matcher(message);
        if (assignment.find()) {
            Role role = Role.fromAlias(assignment.group(1));
            state.setLocalRole(role);
            if (role != null && client.player != null) {
                state.setMarker(client.player.getGameProfile().name(), role);
            }
        }

        Matcher criminalTeam = CRIMINAL_TEAM.matcher(message);
        if (criminalTeam.matches()) {
            markCriminalTeammates(criminalTeam.group(1), criminalTeam.group(2), client);
            return;
        }

        Matcher discovered = DISCOVERED.matcher(message);
        if (discovered.find()) {
            String deadPlayer = discovered.group(1);
            Role role = Role.fromAlias(discovered.group(2));
            if (state.recordDeath(deadPlayer, role)) {
                if (client.player != null
                        && normalize(client.player.getGameProfile().name()).equals(normalize(deadPlayer))) {
                    state.markLocalPlayerDead();
                }
                notifyIfLastInno(client, deadPlayer);
            }
        }

        Matcher chat = CHAT.matcher(message);
        if (state.isInGame() && chat.matches()) {
            recordCallout(chat.group(1), chat.group(2), component, client);
        }
    }

    public void tick(Minecraft client) {
        if (!state.isInGame() || client.level == null) {
            return;
        }
        readSidebar(client.level.getScoreboard());
        evaluateFiendWarning(client);
    }

    private EnumMap<Role, Integer> parseStartingRoles(String roleList) {
        EnumMap<Role, Integer> roles = new EnumMap<>(Role.class);
        Matcher roleMatcher = STARTING_ROLE.matcher(roleList);
        while (roleMatcher.find()) {
            Role role = Role.fromAlias(roleMatcher.group(2));
            if (role != null) {
                roles.put(role, Integer.parseInt(roleMatcher.group(1)));
            }
        }
        return roles;
    }

    private void recordCallout(String caller, String body, Component message, Minecraft client) {
        CalloutParser.ParsedCallout callout = calloutParser.parse(body, client);
        if (callout == null || normalize(callout.target()).equals(normalize(caller))) {
            return;
        }
        state.addCallout(caller, callout.target(), callout.role(), message);
    }

    private void markCriminalTeammates(String teamLabel, String playerList, Minecraft client) {
        Role role = Role.fromAlias(teamLabel);
        if (role == null || client.player == null) {
            return;
        }

        for (String playerName : playerList.split(",\\s*")) {
            if (playerName.isBlank()) {
                continue;
            }
            state.setMarker(playerName, role);
        }
    }

    private void readSidebar(Scoreboard scoreboard) {
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }

        int survivors = state.survivors();
        int actualSurvivors = state.actualSurvivors();
        int currentTimeSeconds = state.currentTimeSeconds();
        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry entry : entries) {
            String line = sanitize(
                    entry.display() != null
                            ? entry.display().getString()
                            : entry.ownerName().getString()
            );
            Matcher survivorMatcher = SURVIVORS.matcher(line);
            if (survivorMatcher.find()) {
                survivors = Integer.parseInt(survivorMatcher.group(1));
            }
            Matcher actualSurvivorMatcher = ACTUAL_SURVIVORS.matcher(line);
            if (actualSurvivorMatcher.find()) {
                actualSurvivors = Integer.parseInt(actualSurvivorMatcher.group(1));
            }
            Matcher timerMatcher = TIMER.matcher(line);
            if (timerMatcher.find()) {
                currentTimeSeconds = Integer.parseInt(timerMatcher.group(1)) * 60
                        + Integer.parseInt(timerMatcher.group(2));
            }
        }

        int trackedSurvivors = state.effectiveSurvivorCount();
        int possibleDeaths = Math.max(0, trackedSurvivors - 2);
        int maxTimeSeconds = currentTimeSeconds >= 0
                ? currentTimeSeconds + possibleDeaths * 20
                : -1;
        state.updateSidebar(survivors, actualSurvivors, currentTimeSeconds, maxTimeSeconds);
    }

    private void evaluateFiendWarning(Minecraft client) {
        if (state.fiendWarningSent()
                || state.localRole() == Role.FIEND
                || state.startingRoleCount(Role.FIEND) == 0) {
            return;
        }

        int criminals = state.remainingRoleCount(Role.TRAITOR) + state.remainingRoleCount(Role.ACCOMPLICE);
        int trackedSurvivors = state.effectiveSurvivorCount();
        boolean allFiendsDiscovered = state.remainingRoleCount(Role.FIEND) == 0;
        boolean fiendWinThresholdPassed = trackedSurvivors >= 0 && trackedSurvivors == criminals + 1;
        if (allFiendsDiscovered || fiendWinThresholdPassed) {
            state.markFiendWarningSent();
            TitleNotification.show(client, "Fiend Dead", Role.FIEND.color());
        }
    }

    private void notifyIfLastInno(Minecraft client, String deadPlayer) {
        if (state.lastInnoMessageSent()
                || client.player == null
                || state.isLocalPlayerDead()
                || normalize(client.player.getGameProfile().name()).equals(normalize(deadPlayer))
                || state.localRole() != Role.INNOCENT
                || state.remainingRoleCount(Role.INNOCENT) != 1) {
            return;
        }

        TitleNotification.show(client, "Last Inno", Role.INNOCENT.color());
        state.markLastInnoMessageSent();
    }

    private static String sanitize(String value) {
        return value.replace("\u200C", "").replace("\u200B", "").trim();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
