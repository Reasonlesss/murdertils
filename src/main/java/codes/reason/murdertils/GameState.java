package codes.reason.murdertils;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Set;

public final class GameState {
    private static final int MAX_CALLOUTS_PER_PLAYER = 3;

    private final EnumMap<Role, Integer> startingRoles = new EnumMap<>(Role.class);
    private final EnumMap<Role, Integer> remainingRoles = new EnumMap<>(Role.class);
    private final Map<String, Role> markers = new HashMap<>();
    private final Set<String> deadPlayers = new HashSet<>();
    private final Map<String, List<Callout>> callouts = new LinkedHashMap<>();

    private boolean inGame;
    private boolean localPlayerDead;
    private boolean fiendWarningSent;
    private boolean lastInnoMessageSent;
    private Role localRole;
    private long gameStartedAt;
    private int survivors;
    private int actualSurvivors;
    private int currentTimeSeconds;
    private int maxTimeSeconds;

    public GameState() {
        reset();
    }

    public void reset() {
        inGame = false;
        localPlayerDead = false;
        fiendWarningSent = false;
        lastInnoMessageSent = false;
        localRole = null;
        gameStartedAt = 0L;
        survivors = -1;
        actualSurvivors = -1;
        currentTimeSeconds = -1;
        maxTimeSeconds = -1;
        markers.clear();
        deadPlayers.clear();
        callouts.clear();
        startingRoles.clear();
        remainingRoles.clear();
        for (Role role : Role.values()) {
            startingRoles.put(role, 0);
            remainingRoles.put(role, 0);
        }
    }

    public void beginGame(Map<Role, Integer> roles, long startedAt) {
        reset();
        inGame = true;
        gameStartedAt = startedAt;
        roles.forEach((role, count) -> {
            startingRoles.put(role, count);
            remainingRoles.put(role, count);
        });
    }

    public void endGame() {
        reset();
    }

    public void markLocalPlayerDead() {
        localPlayerDead = true;
    }

    public void setLocalRole(Role role) {
        localRole = role;
    }

    public boolean recordDeath(String playerName, Role role) {
        if (playerName == null || role == null || !deadPlayers.add(normalize(playerName))) {
            return false;
        }
        remainingRoles.compute(role, (ignored, count) -> Math.max(0, count == null ? 0 : count - 1));
        String deadPlayer = normalize(playerName);
        markers.remove(deadPlayer);
        return true;
    }

    public void addCallout(String caller, String target, Role role, Component text) {
        List<Callout> playerCallouts = callouts.computeIfAbsent(caller, ignored -> new ArrayList<>());
        playerCallouts.removeIf(callout -> normalize(callout.target()).equals(normalize(target)));
        playerCallouts.add(new Callout(target, role, text.copy()));
        if (playerCallouts.size() > MAX_CALLOUTS_PER_PLAYER) {
            playerCallouts.removeFirst();
        }
    }

    public void updateSidebar(int survivors, int actualSurvivors, int currentTimeSeconds, int maxTimeSeconds) {
        this.survivors = survivors;
        this.actualSurvivors = actualSurvivors;
        this.currentTimeSeconds = currentTimeSeconds;
        this.maxTimeSeconds = maxTimeSeconds;
    }

    public void markFiendWarningSent() {
        fiendWarningSent = true;
    }

    public void markLastInnoMessageSent() {
        lastInnoMessageSent = true;
    }

    public void setMarker(String playerName, Role role) {
        if (playerName != null && role != null) {
            markers.put(normalize(playerName), role);
        }
    }

    public Role markerFor(String playerName) {
        return markers.get(normalize(playerName));
    }

    public Role markerForNameTag(String nameTag) {
        String normalized = normalize(nameTag);
        for (Map.Entry<String, Role> marker : markers.entrySet()) {
            if (containsPlayerName(normalized, marker.getKey())) {
                return marker.getValue();
            }
        }
        return null;
    }

    public Component decorateNameTag(Component nameTag) {
        Role marker = markerForNameTag(nameTag.getString());
        if (marker == null) {
            return nameTag;
        }
        return Component.literal(marker.symbol())
                .append(" ")
                .append(nameTag.copy())
                .withColor(marker.color());
    }

    public void clearMarker(String playerName) {
        markers.remove(normalize(playerName));
    }

    public boolean isInGame() {
        return inGame;
    }

    public boolean isLocalPlayerDead() {
        return localPlayerDead;
    }

    public boolean fiendWarningSent() {
        return fiendWarningSent;
    }

    public boolean lastInnoMessageSent() {
        return lastInnoMessageSent;
    }

    public Role localRole() {
        return localRole;
    }

    public int startingRoleCount(Role role) {
        return startingRoles.getOrDefault(role, 0);
    }

    public int remainingRoleCount(Role role) {
        return remainingRoles.getOrDefault(role, 0);
    }

    public Map<Role, Integer> remainingRoles() {
        return Map.copyOf(remainingRoles);
    }

    public int markerCount(Role role) {
        return (int) markers.values().stream().filter(role::equals).count();
    }

    public int trackedAliveCount(Role role) {
        if (localRole == Role.TRAITOR) {
            if (role == Role.TRAITOR) {
                return markerCount(Role.TRAITOR);
            }
            if (role == Role.ACCOMPLICE) {
                return markerCount(Role.ACCOMPLICE);
            }
        }
        return remainingRoleCount(role);
    }

    public Map<String, List<Callout>> callouts() {
        Map<String, List<Callout>> snapshot = new LinkedHashMap<>();
        callouts.forEach((caller, entries) -> snapshot.put(caller, List.copyOf(entries)));
        return Collections.unmodifiableMap(snapshot);
    }

    public long gameStartedAt() {
        return gameStartedAt;
    }

    public int survivors() {
        return survivors;
    }

    public int actualSurvivors() {
        return actualSurvivors;
    }

    public int effectiveSurvivorCount() {
        if (localRole == Role.TRAITOR && actualSurvivors >= 0) {
            return actualSurvivors;
        }
        return survivors;
    }

    public int currentTimeSeconds() {
        return currentTimeSeconds;
    }

    public int maxTimeSeconds() {
        return maxTimeSeconds;
    }

    public List<String> roleSummary() {
        List<String> lines = new ArrayList<>();
        for (Role role : Role.values()) {
            int confirmed = (int) markers.values().stream().filter(role::equals).count();
            lines.add(role.symbol() + " " + role.displayName() + ": "
                    + remainingRoles.get(role) + " (" + confirmed + " marked)");
        }
        return lines;
    }

    public String compactRoleSummary() {
        StringJoiner joiner = new StringJoiner(" ");
        for (Role role : Role.values()) {
            int count = remainingRoles.getOrDefault(role, 0);
            if (count > 0) {
                joiner.add(count + " " + role.summaryLabel(count));
            }
        }
        String summary = joiner.toString();
        return summary.isEmpty() ? null : summary;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean containsPlayerName(String text, String playerName) {
        int index = text.indexOf(playerName);
        while (index >= 0) {
            int end = index + playerName.length();
            boolean startsAtBoundary = index == 0 || !isPlayerNameCharacter(text.charAt(index - 1));
            boolean endsAtBoundary = end == text.length() || !isPlayerNameCharacter(text.charAt(end));
            if (startsAtBoundary && endsAtBoundary) {
                return true;
            }
            index = text.indexOf(playerName, index + 1);
        }
        return false;
    }

    private static boolean isPlayerNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    public record Callout(String target, Role role, Component text) {
    }
}
