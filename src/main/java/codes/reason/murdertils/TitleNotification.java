package codes.reason.murdertils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class TitleNotification {
    private static final int FADE_IN_TICKS = 0;
    private static final int STAY_TICKS = 80;
    private static final int FADE_OUT_TICKS = 10;

    private TitleNotification() {
    }

    public static void show(Minecraft client, String title, int color) {
        client.gui.setTimes(FADE_IN_TICKS, STAY_TICKS, FADE_OUT_TICKS);
        client.gui.setTitle(Component.literal(title).withColor(color));
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.0F));
    }
}
