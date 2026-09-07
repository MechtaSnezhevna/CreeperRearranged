package io.github.mechtasnezhevna.creeper_rearranged.entity;

import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * Base class for creeper variants.
 *
 * <p>Vanilla creepers detonate through the private {@code Creeper#explodeCreeper()}, which cannot
 * be overridden. {@code CreeperExplosionMixin} routes that call here instead, so variants can
 * customize the blast through {@link #createVariantExplosion()} and {@link #afterVariantExplosion(Explosion)}.
 */
public abstract class VariantCreeper extends Creeper
{
    protected VariantCreeper(EntityType<? extends VariantCreeper> entityType, Level level)
    {
        super(entityType, level);
    }

    /**
     * Called by {@code CreeperExplosionMixin} in place of vanilla's private {@code explodeCreeper()}.
     * Mirrors the vanilla choreography: mark dead, explode, lingering cloud, death effects, discard.
     */
    public final void triggerVariantExplosion()
    {
        if (this.level().isClientSide) {
            return;
        }
        this.dead = true;
        Explosion explosion = this.createVariantExplosion();
        this.afterVariantExplosion(explosion);
        this.spawnVariantLingeringCloud();
        this.triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
        this.discard();
    }

    /**
     * Creates and runs the explosion. The default matches a vanilla creeper blast:
     * radius {@link #getVariantExplosionRadius()}, respecting the mobGriefing game rule.
     */
    protected Explosion createVariantExplosion()
    {
        return this.level()
            .explode(this, this.getX(), this.getY(), this.getZ(), this.getVariantExplosionRadius(), Level.ExplosionInteraction.MOB);
    }

    /** Blast radius for this variant; doubled while powered, like a vanilla creeper. */
    protected float getVariantExplosionRadius()
    {
        return 3.0F * (this.isPowered() ? 2.0F : 1.0F);
    }

    /** Hook run after the explosion is fully resolved (block destruction already happened). */
    protected void afterVariantExplosion(Explosion explosion)
    {
    }

    /** Copy of vanilla {@code Creeper#spawnLingeringCloud()}, which is private. */
    private void spawnVariantLingeringCloud()
    {
        Collection<MobEffectInstance> collection = this.getActiveEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloud areaEffectCloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            areaEffectCloud.setRadius(2.5F);
            areaEffectCloud.setRadiusOnUse(-0.5F);
            areaEffectCloud.setWaitTime(10);
            areaEffectCloud.setDuration(areaEffectCloud.getDuration() / 2);
            areaEffectCloud.setRadiusPerTick(-areaEffectCloud.getRadius() / (float) areaEffectCloud.getDuration());
            for (MobEffectInstance mobEffectInstance : collection) {
                areaEffectCloud.addEffect(new MobEffectInstance(mobEffectInstance));
            }
            this.level().addFreshEntity(areaEffectCloud);
        }
    }
}
