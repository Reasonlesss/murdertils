package codes.reason.murdertils.mixin;

import codes.reason.murdertils.Murdertils;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void murdertils$appendRoleMarker(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state.nameTag == null || Murdertils.INSTANCE == null) {
            return;
        }
        state.nameTag = Murdertils.INSTANCE.getGameState().decorateNameTag(state.nameTag);
    }
}
