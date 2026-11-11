package io.github.mechtasnezhevna.creeper_rearranged.mixin;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wardens never take a sculkper as a target: {@link Warden#canTargetEntity(Entity)} is the single
 * gate the warden's anger and targeting systems funnel through, so excluding the sculkper there
 * keeps a warden from ever gaining hatred toward one - even when they collide or when the
 * sculkper's blast damages the warden (the warden brain also re-validates its current target
 * through this method and drops it). {@link Warden#setAttackTarget(LivingEntity)} is intercepted
 * as well, because {@code Warden#hurt} sets an attack target directly when the warden is damaged,
 * which would otherwise let a sculkper's blast leave a transient target behind.
 */
@Mixin(Warden.class)
public abstract class WardenTargetingMixin
{
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void creeperRearranged$ignoreSculkper(Entity entity, CallbackInfoReturnable<Boolean> callbackInfoReturnable)
    {
        if (entity != null && entity.getType() == ModEntities.SCULKPER.get()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void creeperRearranged$neverAttackSculkper(LivingEntity attackTarget, CallbackInfo callbackInfo)
    {
        if (attackTarget != null && attackTarget.getType() == ModEntities.SCULKPER.get()) {
            callbackInfo.cancel();
        }
    }
}
