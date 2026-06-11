package codes.reason.murdertils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum Role {
    TRAITOR("Traitor", "\uD83D\uDDE1", 0xFFFF4242, Set.of("traitor", "traitors", "trait", "traits", "t", "ts")),
    ACCOMPLICE("Accomplice", "\uD83D\uDD25", 0xFFFF8A00, Set.of("accomplice", "accomplices", "acc", "accs")),
    FIEND("Fiend", "\uD83D\uDDE1", 0xFF8354CC, Set.of("fiend", "fiends", "f", "fs")),
    DETECTIVE("Detective", "\uD83C\uDFF9", 0xFF73CDFF, Set.of("detective", "detectives", "det", "dets")),
    DOCTOR("Doctor", "\uD83E\uDDEA", 0xFFFF61B5, Set.of("doctor", "doctors", "doc", "docs")),
    INNOCENT("Innocent", "⭐", 0xFF90FF3D, Set.of("innocent", "innocents", "inno", "innos"));

    private final String displayName;
    private final String symbol;
    private final int color;
    private final Set<String> aliases;

    Role(String displayName, String symbol, int color, Set<String> aliases) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
        this.aliases = aliases;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    public int color() {
        return color;
    }

    public String summaryLabel(int count) {
        String base = displayName.toLowerCase(Locale.ROOT);
        if (count == 1) {
            return base;
        }
        if (this == INNOCENT) {
            return "innos";
        }
        return base + "s";
    }

    public boolean isEvil() {
        return this == TRAITOR || this == ACCOMPLICE;
    }

    public static Role fromAlias(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return Arrays.stream(values())
                .filter(role -> role.aliases.contains(normalized))
                .findFirst()
                .orElse(null);
    }
}
