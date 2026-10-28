package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.item.SoulCorruptedPrimedTnt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Withers the creatures a soul corrupted TNT blast damages.
 *
 * <p>The explosion's damage source has the primed TNT as its {@link DamageSource#getDirectEntity()},
 * so the blast is recognised here and the Wither effect is added to the victim. {@code addEffect}
 * still runs every entity-side immunity check, so wither skeletons, the wither and other wither
 * immune mobs are exempt on their own, without a damage-source-side blacklist.
 */
public final class SoulTntWithering
{
    /** 5 seconds, the same breath as the wiskelper's soul absorption. */
    public static final int WITHER_DURATION_TICKS = 100;
    public static final int WITHER_AMPLIFIER = 0;

    private SoulTntWithering()
    {
    }

    public static void onLivingDamage(LivingDamageEvent.Pre event)
    {
        DamageSource source = event.getSource();
        Entity directEntity = source.getDirectEntity();
        if (!(directEntity instanceof SoulCorruptedPrimedTnt)) {
            return;
        }
        if (!source.is(DamageTypes.EXPLOSION) && !source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, WITHER_AMPLIFIER));
    }
}
