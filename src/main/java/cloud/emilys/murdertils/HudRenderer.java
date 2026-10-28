package cloud.emilys.murdertils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HudRenderer {
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_BACKGROUND = 0xB0101118;
    private static final int PANEL_OUTLINE = 0x805A6070;
    private static final int PANEL_TITLE_COLOR = 0xFFB8BCC8;
    private static final int WHEEL_ITEM_SIZE = 16;
    private static final int WHEEL_CONTENT_Y_OFFSET = 1;

    private final GameState state;
    private final GameTimers gameTimers;
    private final RoleSelectionWheel roleSelectionWheel;
    private final EnumMap<Role, ItemStack> roleChestplates = new EnumMap<>(Role.class);
    private final EnumMap<Role, ItemStack> disabledRoleChestplates = new EnumMap<>(Role.class);

    public HudRenderer(GameState state, GameTimers gameTimers, RoleSelectionWheel roleSelectionWheel) {
        this.state = state;
        this.gameTimers = gameTimers;
        this.roleSelectionWheel = roleSelectionWheel;
        for (Role role : Role.values()) {
            ItemStack chestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
            chestplate.set(DataComponents.DYED_COLOR, new DyedItemColor(role.color() & 0xFFFFFF));
            roleChestplates.put(role, chestplate);

            ItemStack disabledChestplate = new ItemStack(Items.LEATHER_CHESTPLATE);
            disabledChestplate.set(DataComponents.DYED_COLOR, new DyedItemColor(0x6D717C));
            disabledRoleChestplates.put(role, disabledChestplate);
        }
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!state.isInGame() || client.player == null || client.gui.hud.isHidden()) {
            return;
        }

        Font font = client.font;
        int left = 8;
        int right = graphics.guiWidth() - 8;
        int rightY = 8;
        double age = (System.currentTimeMillis() - state.gameStartedAt()) / 250.0;
        float entrance = Easing.EASE_OUT_CUBIC.getFunction().apply(Math.min(1.0, age)).floatValue();
        int offset = Math.round((1.0f - entrance) * 60.0f);

        Map<Role, Integer> counts = state.remainingRoles();
        int panelWidth = font.width("Remaining roles") + PANEL_PADDING * 2;
        for (Role role : Role.values()) {
            String roleName = role.symbol() + " " + role.displayName() + ":";
            String alive = Integer.toString(state.trackedAliveCount(role));
            String markedAlive = markedAliveText(state.markerCount(role));
            panelWidth = Math.max(
                    panelWidth,
                    font.width(roleName) + font.width(alive) + font.width(markedAlive) + PANEL_PADDING * 3
            );
        }
        int panelX = right - panelWidth + offset;
        int panelHeight = 19 + Role.values().length * 11;
        graphics.fill(panelX, rightY, panelX + panelWidth, rightY + panelHeight, PANEL_BACKGROUND);
        graphics.outline(panelX, rightY, panelWidth, panelHeight, PANEL_OUTLINE);
        graphics.text(
                font,
                "Remaining roles",
                panelX + PANEL_PADDING,
                rightY + 5,
                PANEL_TITLE_COLOR,
                false
        );
        rightY += 17;
        for (Role role : Role.values()) {
            int aliveCount = state.trackedAliveCount(role);
            String roleName = role.symbol() + " " + role.displayName() + ":";
            String alive = Integer.toString(aliveCount);
            String markedAlive = markedAliveText(state.markerCount(role));
            int markedX = panelX + panelWidth - PANEL_PADDING - font.width(markedAlive);
            boolean eliminated = aliveCount == 0;
            int roleColor = eliminated ? 0xFF6D717C : role.color();
            int countColor = eliminated ? 0xFF8A8F9D : 0xFFFFFFFF;
            graphics.text(font, roleName, panelX + PANEL_PADDING, rightY, roleColor, false);
            graphics.text(font, alive, markedX - font.width(alive) - 3, rightY, countColor, false);
            graphics.text(font, markedAlive, markedX, rightY, 0xFF8A8F9D, false);
            rightY += 11;
        }

        if (!state.isLocalPlayerDead()) {
            rightY += 5;
            drawTimersPanel(graphics, font, right, rightY, System.currentTimeMillis(), true);
        }

        Map<String, List<GameState.Callout>> callouts = state.callouts();
        if (!callouts.isEmpty()) {
            drawCalloutsPanel(graphics, font, left - offset, 8, callouts);
        }

        renderRoleWheel(graphics, font);
    }

    private void renderRoleWheel(GuiGraphicsExtractor graphics, Font font) {
        if (!roleSelectionWheel.isOpen()) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        Role selectedRole = roleSelectionWheel.selection();
        drawWheelBounds(graphics, centerX, centerY);
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            Role role = roles[i];
            RoleWheelBounds bounds = RoleWheelLayout.boundsFor(role, centerX, centerY);
            boolean selected = role == selectedRole;
            boolean disabled = state.trackedAliveCount(role) == 0
                    || state.localRole() == Role.TRAITOR && (role == Role.TRAITOR || role == Role.ACCOMPLICE);
            int contentHeight = WHEEL_ITEM_SIZE + 4 + font.lineHeight;
            int contentTop = bounds.y() + (bounds.height() - contentHeight) / 2;
            graphics.item(
                    disabled ? disabledRoleChestplates.get(role) : roleChestplates.get(role),
                    bounds.x() + (bounds.width() - WHEEL_ITEM_SIZE) / 2,
                    contentTop + WHEEL_CONTENT_Y_OFFSET
            );
            graphics.centeredText(
                    font,
                    role.displayName(),
                    bounds.x() + bounds.width() / 2,
                    contentTop + WHEEL_ITEM_SIZE + 4 + WHEEL_CONTENT_Y_OFFSET,
                    disabled ? 0xFF6D717C : selected ? role.color() : 0xFFE7EAF0
            );
        }

        graphics.centeredText(
                font,
                roleSelectionWheel.target(),
                centerX,
                centerY - 10 + WHEEL_CONTENT_Y_OFFSET,
                0xFFFFFFFF
        );
        String instruction = selectedRole == null ? "Look toward a role" : "Release to mark and copy";
        graphics.centeredText(font, instruction, centerX, centerY + 4 + WHEEL_CONTENT_Y_OFFSET, 0xFFB8BCC8);
    }

    private static void drawWheelBounds(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        int outerRadius = (int) RoleWheelLayout.OUTER_RADIUS;
        int innerRadius = (int) RoleWheelLayout.INNER_RADIUS;
        int background = 0x58101218;

        for (int y = -outerRadius; y <= outerRadius; y++) {
            int outerExtent = (int) Math.floor(Math.sqrt(outerRadius * outerRadius - y * y));
            if (Math.abs(y) >= innerRadius) {
                graphics.fill(centerX - outerExtent, centerY + y, centerX + outerExtent + 1, centerY + y + 1, background);
                continue;
            }

            int innerExtent = (int) Math.ceil(Math.sqrt(innerRadius * innerRadius - y * y));
            graphics.fill(centerX - outerExtent, centerY + y, centerX - innerExtent, centerY + y + 1, background);
            graphics.fill(centerX + innerExtent, centerY + y, centerX + outerExtent + 1, centerY + y + 1, background);
        }
    }

    private int drawTimersPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            int anchor,
            int y,
            long now,
            boolean alignRight
    ) {
        int coinSeconds = gameTimers.coinSecondsRemaining(now);
        int fiendSeconds = gameTimers.fiendSecondsRemaining(now);
        String maxText = state.maxTimeSeconds() >= 0
                ? "⌚ Max  " + formatTime(state.maxTimeSeconds())
                : null;
        if (maxText == null && coinSeconds < 0 && fiendSeconds < 0) {
            return y;
        }

        String coinText = coinSeconds >= 0 ? "◎ Coins  " + formatTime(coinSeconds) : null;
        String fiendText = fiendSeconds >= 0 ? "🗡 Fiend  " + formatTime(fiendSeconds) : null;
        int width = font.width("Timers");
        int lineCount = 0;
        if (maxText != null) {
            width = Math.max(width, font.width(maxText));
            lineCount++;
        }
        if (coinText != null) {
            width = Math.max(width, font.width(coinText));
            lineCount++;
        }
        if (fiendText != null) {
            width = Math.max(width, font.width(fiendText));
            lineCount++;
        }
        width += PANEL_PADDING * 2;

        int height = 17 + lineCount * 11;
        int left = alignRight ? anchor - width : anchor;
        graphics.fill(left, y, left + width, y + height, PANEL_BACKGROUND);
        graphics.outline(left, y, width, height, PANEL_OUTLINE);
        graphics.text(font, "Timers", left + PANEL_PADDING, y + 4, PANEL_TITLE_COLOR, false);

        int lineY = y + 15;
        if (maxText != null) {
            graphics.text(font, maxText, left + PANEL_PADDING, lineY, 0xFF73CDFF, false);
            lineY += 11;
        }
        if (coinText != null) {
            graphics.text(font, coinText, left + PANEL_PADDING, lineY, 0xFFFFD45A, false);
            lineY += 11;
        }
        if (fiendText != null) {
            graphics.text(font, fiendText, left + PANEL_PADDING, lineY, Role.FIEND.color(), false);
        }
        return y + height;
    }

    private int drawCalloutsPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int y,
            Map<String, List<GameState.Callout>> callouts
    ) {
        int width = font.width("Callouts");
        int lineCount = 0;
        for (List<GameState.Callout> playerCallouts : callouts.values()) {
            for (GameState.Callout callout : playerCallouts) {
                width = Math.max(width, font.width(callout.text()) + 6);
                lineCount++;
            }
        }
        width += PANEL_PADDING * 2;

        int height = 19 + lineCount * 11;
        graphics.fill(left, y, left + width, y + height, PANEL_BACKGROUND);
        graphics.outline(left, y, width, height, PANEL_OUTLINE);
        graphics.text(font, "Callouts", left + PANEL_PADDING, y + 4, PANEL_TITLE_COLOR, false);

        int lineY = y + 17;
        for (List<GameState.Callout> playerCallouts : callouts.values()) {
            for (GameState.Callout callout : playerCallouts) {
                graphics.text(font, callout.text(), left + PANEL_PADDING, lineY, 0xFFFFFFFF, false);
                lineY += 11;
            }
        }
        return y + height;
    }

    private static String markedAliveText(int markedAlive) {
        return "(" + markedAlive + ")";
    }

    private static String formatTime(int seconds) {
        if (seconds < 0) {
            return "--:--";
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
