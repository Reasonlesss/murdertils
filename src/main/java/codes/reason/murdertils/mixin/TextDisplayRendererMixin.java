package codes.reason.murdertils.mixin;

import codes.reason.murdertils.Murdertils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayRendererMixin {
    @Shadow
    @Final
    private Font font;

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void murdertils$appendRoleMarker(
            Display.TextDisplay entity,
            TextDisplayEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (Murdertils.INSTANCE == null || state.textRenderState == null) {
            return;
        }

        Display.TextDisplay.TextRenderState textState = state.textRenderState;
        Component decorated = Murdertils.INSTANCE.getGameState().decorateNameTag(textState.text());
        if (decorated == textState.text()) {
            return;
        }
        FormattedCharSequence fcs = decorated.getVisualOrderText();
        int width = font.width(fcs);
        state.cachedInfo = new Display.TextDisplay.CachedInfo(
                List.of(new Display.TextDisplay.CachedLine(decorated.getVisualOrderText(), width)),
                width
        );
    }
}
