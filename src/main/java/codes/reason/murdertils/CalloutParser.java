package codes.reason.murdertils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Arrays;
import java.util.Locale;

public final class CalloutParser {
    public ParsedCallout parse(String body, Minecraft client) {
        String[] words = body.trim().split("\\s+");
        if (words.length < 2 || containsNegation(words)) {
            return null;
        }

        Role role = resolveRole(words[words.length - 1]);
        String target = resolvePlayer(words[0], client);
        if (role == null || target == null) {
            return null;
        }
        return new ParsedCallout(target, role);
    }

    private static boolean containsNegation(String[] words) {
        return Arrays.stream(words)
                .map(word -> word.replaceAll("[^A-Za-z]", ""))
                .anyMatch(word -> word.equalsIgnoreCase("not"));
    }

    private String resolvePlayer(String token, Minecraft client) {
        String clean = token.replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isBlank() || client.getConnection() == null) {
            return null;
        }

        String lower = clean.toLowerCase(Locale.ROOT);
        String prefixMatch = null;
        boolean ambiguousPrefix = false;
        String fuzzyMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean ambiguousFuzzyMatch = false;
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            String name = info.getProfile().name();
            String candidate = name.toLowerCase(Locale.ROOT);
            if (candidate.equals(lower)) {
                return name;
            }
            if (candidate.startsWith(lower) || lower.startsWith(candidate)) {
                if (prefixMatch != null) {
                    ambiguousPrefix = true;
                } else {
                    prefixMatch = name;
                }
                continue;
            }

            int distance = playerNameDistance(lower, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                fuzzyMatch = name;
                ambiguousFuzzyMatch = false;
            } else if (distance == bestDistance) {
                ambiguousFuzzyMatch = true;
            }
        }
        if (prefixMatch != null && !ambiguousPrefix) {
            return prefixMatch;
        }

        int maxDistance = lower.length() >= 7 ? 2 : lower.length() >= 4 ? 1 : 0;
        return bestDistance <= maxDistance && !ambiguousFuzzyMatch ? fuzzyMatch : null;
    }

    private Role resolveRole(String token) {
        Role exact = Role.fromAlias(token);
        if (exact != null) {
            return exact;
        }

        String normalized = token.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalized.length() < 4) {
            return null;
        }

        Role match = null;
        int bestDistance = Integer.MAX_VALUE;
        boolean ambiguous = false;
        for (Role role : Role.values()) {
            String candidate = role.displayName().toLowerCase(Locale.ROOT);
            int distance = editDistance(normalized, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                match = role;
                ambiguous = false;
            } else if (distance == bestDistance) {
                ambiguous = true;
            }
        }
        int maxDistance = normalized.length() >= 7 ? 2 : 1;
        return bestDistance <= maxDistance && !ambiguous ? match : null;
    }

    private static int playerNameDistance(String token, String playerName) {
        int distance = editDistance(token, playerName);
        if (playerName.length() > token.length()) {
            distance = Math.min(distance, editDistance(token, playerName.substring(0, token.length())));
        }
        return distance;
    }

    private static int editDistance(String left, String right) {
        int[][] distances = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            distances[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            distances[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                distances[i][j] = Math.min(
                        Math.min(distances[i - 1][j] + 1, distances[i][j - 1] + 1),
                        distances[i - 1][j - 1] + substitutionCost
                );
                if (i > 1
                        && j > 1
                        && left.charAt(i - 1) == right.charAt(j - 2)
                        && left.charAt(i - 2) == right.charAt(j - 1)) {
                    distances[i][j] = Math.min(distances[i][j], distances[i - 2][j - 2] + 1);
                }
            }
        }
        return distances[left.length()][right.length()];
    }

    public record ParsedCallout(String target, Role role) {
    }
}
