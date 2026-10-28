package cloud.emilys.murdertils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class RoleSelectionWheel {
    private static final double TARGET_DISTANCE = 64.0;
    private static final double TARGET_CONE_DEGREES = 10.0;
    private static final double TARGET_CONE_COSINE = Math.cos(Math.toRadians(TARGET_CONE_DEGREES));
    private static final int RELEASE_DEBOUNCE_TICKS = 2;

    private final GameState state;

    private boolean wasDown;
    private boolean open;
    private String target;
    private Role selection;
    private long openedAt;
    private int releaseTicks;

    public RoleSelectionWheel(GameState state) {
        this.state = state;
    }

    public void tick(Minecraft client, KeyMapping key) {
        boolean down = key.isDown();
        boolean eligible = state.isInGame()
                && !state.isLocalPlayerDead()
                && client.player != null
                && client.player.isAlive();
        if (open && !eligible) {
            close(client);
        } else if (eligible && down && !wasDown && client.gui.screen() == null) {
            open(client);
        }
        if (open && down) {
            releaseTicks = 0;
            updateSelection(client);
        }
        if (open && !down && ++releaseTicks >= RELEASE_DEBOUNCE_TICKS) {
            applySelection(client);
            close(client);
        }
        if (!open && down) {
            releaseTicks = 0;
        }
        wasDown = down;
    }

    private void open(Minecraft client) {
        String targetedPlayer = targetedPlayer(client);
        if (targetedPlayer == null) {
            client.player.sendOverlayMessage(Component.literal("Look at a player or dead body to mark them."));
            return;
        }
        if (isProtectedCriminalTeammate(targetedPlayer)) {
            client.player.sendOverlayMessage(Component.literal("You can't change marks on known criminals."));
            return;
        }

        open = true;
        target = targetedPlayer;
        selection = null;
        openedAt = System.currentTimeMillis();
        // releaseMouse() also centers the cursor in the window
        client.mouseHandler.releaseMouse();
    }

    private void updateSelection(Minecraft client) {
        double cursorX = client.mouseHandler.getScaledXPos(client.getWindow());
        double cursorY = client.mouseHandler.getScaledYPos(client.getWindow());
        double centerX = client.getWindow().getGuiScaledWidth() / 2.0;
        double centerY = client.getWindow().getGuiScaledHeight() / 2.0;

        Role hoveredRole = RoleWheelLayout.roleAt(cursorX, cursorY, centerX, centerY);
        selection = canAssignRole(target, hoveredRole) ? hoveredRole : null;
    }

    private void applySelection(Minecraft client) {
        if (target == null) {
            return;
        }
        if (isProtectedCriminalTeammate(target)) {
            return;
        }
        if (selection == null) {
            state.clearMarker(target);
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }
        if (!canAssignRole(target, selection)) {
            return;
        }
        state.setMarker(target, selection);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        client.keyboardHandler.setClipboard(
                target + " " + selection.displayName().toLowerCase(Locale.ROOT)
        );
    }

    private void close(Minecraft client) {
        open = false;
        target = null;
        selection = null;
        releaseTicks = 0;
        client.mouseHandler.grabMouse();
    }

    private static String targetedPlayer(Minecraft client) {
        Player localPlayer = client.player;
        Vec3 start = localPlayer.getEyePosition();
        Vec3 view = localPlayer.getViewVector(1.0F).normalize();
        String bestPlayer = null;
        double bestAlignment = TARGET_CONE_COSINE;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Player player : client.level.players()) {
            if (player == localPlayer) {
                continue;
            }

            Vec3 target = player.getEyePosition(1.0F);
            Vec3 offset = target.subtract(start);
            double distanceSquared = offset.lengthSqr();
            if (distanceSquared > TARGET_DISTANCE * TARGET_DISTANCE || distanceSquared == 0.0) {
                continue;
            }

            double alignment = view.dot(offset.normalize());
            if (alignment < TARGET_CONE_COSINE || !hasLineOfSight(client, localPlayer, start, target, distanceSquared)) {
                continue;
            }

            if (alignment > bestAlignment
                    || alignment == bestAlignment && distanceSquared < bestDistanceSquared) {
                bestPlayer = player.getGameProfile().name();
                bestAlignment = alignment;
                bestDistanceSquared = distanceSquared;
            }
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player) {
                continue;
            }

            String label = entity instanceof Display.TextDisplay textDisplay
                    ? textDisplay.getText().getString()
                    : entity.getCustomName() == null ? null : entity.getCustomName().getString();
            String playerName = playerNameInLabel(label, client);
            if (playerName == null) {
                continue;
            }

            Vec3 target = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
            Vec3 offset = target.subtract(start);
            double distanceSquared = offset.lengthSqr();
            if (distanceSquared > TARGET_DISTANCE * TARGET_DISTANCE || distanceSquared == 0.0) {
                continue;
            }

            double alignment = view.dot(offset.normalize());
            if (alignment < TARGET_CONE_COSINE
                    || !hasLineOfSight(client, localPlayer, start, target, distanceSquared)) {
                continue;
            }

            if (alignment > bestAlignment
                    || alignment == bestAlignment && distanceSquared < bestDistanceSquared) {
                bestPlayer = playerName;
                bestAlignment = alignment;
                bestDistanceSquared = distanceSquared;
            }
        }
        return bestPlayer;
    }

    private static String playerNameInLabel(String label, Minecraft client) {
        if (label == null || label.isBlank() || client.getConnection() == null) {
            return null;
        }

        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            String playerName = info.getProfile().name();
            if (containsPlayerName(normalizedLabel, playerName.toLowerCase(Locale.ROOT))) {
                return playerName;
            }
        }
        return null;
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

    private static boolean hasLineOfSight(
            Minecraft client,
            Player localPlayer,
            Vec3 start,
            Vec3 target,
            double targetDistanceSquared
    ) {
        HitResult blockHit = client.level.clip(new ClipContext(
                start,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                localPlayer
        ));
        return blockHit.getType() == HitResult.Type.MISS
                || blockHit.getLocation().distanceToSqr(start) + 0.25 >= targetDistanceSquared;
    }

    public boolean isOpen() {
        return open;
    }

    public String target() {
        return target;
    }

    public Role selection() {
        return selection;
    }

    public long openedAt() {
        return openedAt;
    }

    private boolean isProtectedCriminalTeammate(String playerName) {
        if (state.localRole() != Role.TRAITOR) {
            return false;
        }
        Role marker = state.markerFor(playerName);
        return marker == Role.TRAITOR || marker == Role.ACCOMPLICE;
    }

    private boolean canAssignRole(String playerName, Role role) {
        if (playerName == null || role == null) {
            return false;
        }
        if (state.trackedAliveCount(role) == 0) {
            return false;
        }
        return state.localRole() != Role.TRAITOR || (role != Role.TRAITOR && role != Role.ACCOMPLICE);
    }
}
