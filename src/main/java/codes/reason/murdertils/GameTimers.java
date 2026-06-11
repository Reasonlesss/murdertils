package codes.reason.murdertils;

public final class GameTimers {
    private static final int[] COIN_TIMERS_SECONDS = {
            120, 240, 360, 480, 600, 720, 840, 960, 1080, 1200
    };
    private static final int[] FIEND_TIMERS_SECONDS = {
            100, 170, 240, 310, 380, 450, 520, 590, 660, 730
    };

    private final GameState state;

    public GameTimers(GameState state) {
        this.state = state;
    }

    public int coinSecondsRemaining(long now) {
        return secondsUntilNext(COIN_TIMERS_SECONDS, now);
    }

    public int fiendSecondsRemaining(long now) {
        if (state.startingRoleCount(Role.FIEND) == 0 || state.remainingRoleCount(Role.FIEND) == 0) {
            return -1;
        }
        return secondsUntilNext(FIEND_TIMERS_SECONDS, now);
    }

    private int secondsUntilNext(int[] timers, long now) {
        long elapsedMillis = Math.max(0L, now - state.gameStartedAt());
        for (int timer : timers) {
            long remainingMillis = timer * 1000L - elapsedMillis;
            if (remainingMillis > 0L) {
                return (int) ((remainingMillis + 999L) / 1000L);
            }
        }
        return -1;
    }
}
