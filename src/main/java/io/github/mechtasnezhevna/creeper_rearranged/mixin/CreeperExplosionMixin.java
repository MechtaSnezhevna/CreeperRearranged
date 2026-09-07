package io.github.mechtasnezhevna.creeper_rearranged.mixin;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@link Creeper#explodeCreeper()} is private and called non-virtually, so variant creepers
 * cannot detonate through subclass overrides. This mixin hands the explosion over to
 * {@link VariantCreeper#triggerVariantExplosion()} whenever a variant reaches the detonation point.
 */
@Mixin(Creeper.class)
public abstract class CreeperExplosionMixin
{
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void creeperRearranged$routeVariantExplosion(CallbackInfo callbackInfo)
    {
        if ((Object) this instanceof VariantCreeper variantCreeper) {
            callbackInfo.cancel();
            variantCreeper.triggerVariantExplosion();
        }
    }
}
