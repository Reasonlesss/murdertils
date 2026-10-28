package cloud.emilys.murdertils;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Murdertils implements ClientModInitializer {
    public static Murdertils INSTANCE;

    private KeyMapping roleSelectionWheelKey;
    private KeyMapping copyRoleInfoKey;
    private final GameState gameState = new GameState();
    private final GameTracker gameTracker = new GameTracker(gameState);
    private final GameTimers gameTimers = new GameTimers(gameState);
    private final RoleSelectionWheel roleSelectionWheel = new RoleSelectionWheel(gameState);

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        registerKeybindings();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                gameTracker.handleMessage(message, overlay, Minecraft.getInstance())
        );
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) ->
                gameTracker.handleMessage(message, false, Minecraft.getInstance())
        );
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("murdertils", "game_hud"),
                new HudRenderer(gameState, gameTimers, roleSelectionWheel)::render
        );
    }

    private void registerKeybindings() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("murdertils", "murdertils")
        );
        roleSelectionWheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.murdertils.role_selection_wheel",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_G,
                category
        ));

        copyRoleInfoKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.murdertils.copy_role_info",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_C,
                category
        ));
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || roleSelectionWheelKey == null) {
            return;
        }

        gameTracker.tick(client);
        roleSelectionWheel.tick(client, roleSelectionWheelKey);

        while (copyRoleInfoKey.consumeClick()) {
            String summary = gameState.compactRoleSummary();
            if (summary != null) {
                client.keyboardHandler.setClipboard(summary);
                client.player.sendOverlayMessage(Component.literal("Copied role summary."));
            }
        }
    }

    public KeyMapping getRoleSelectionWheelKey() {
        return roleSelectionWheelKey;
    }

    public KeyMapping getCopyRoleInfoKey() {
        return copyRoleInfoKey;
    }

    public GameState getGameState() {
        return gameState;
    }

}
