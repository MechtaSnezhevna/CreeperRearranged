package io.github.mechtasnezhevna.creeper_rearranged.block;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModBlocks;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Walks a peaceful creeper towards the nearest block of creeper soul.
 *
 * <p>The goal is injected into every whitelisted creeper kind from {@code
 * CreeperHooks#onEntityJoinLevel}. While a soul block is within {@value #AURA_RANGE} blocks the
 * creeper navigates to within {@value #STOP_DISTANCE} blocks of it and stands there, and it keeps
 * a beacon-like Speed III boost for as long as it stays inside the aura. The goal only runs while
 * the creeper has no target, is not priming and is not asleep: once it picks a fight or starts
 * swelling, its combat goals - which hold higher priorities - take over.
 */
public class CreeperSoulAttractGoal extends Goal
{
    /** Radius (blocks) of the soul aura; attraction and speed boost share it. */
    public static final double AURA_RANGE = 15.0;
    /** The creeper stops once it is this close (blocks) to the soul block. */
    private static final double STOP_DISTANCE = 3.0;
    /** How often (ticks) the nearest soul block is re-scanned while walking. */
    private static final int RESCAN_INTERVAL = 20;
    /** Walk speed, the same one the vanilla random stroll uses. */
    private static final double SPEED = 1.0;
    /** How often (ticks) the beacon-like speed boost is re-applied. */
    private static final int EFFECT_INTERVAL = 40;
    /** Duration (ticks) of each speed boost; outlives the refresh interval. */
    private static final int EFFECT_DURATION = 120;
    /** Speed III = amplifier 2. */
    private static final int EFFECT_AMPLIFIER = 2;

    private final Creeper creeper;
    @Nullable
    private BlockPos soulBlock;
    private int nextScanTick;
    private int nextEffectTick;

    public CreeperSoulAttractGoal(Creeper creeper)
    {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (this.soulBlock != null
            && !this.creeper.level().getBlockState(this.soulBlock).is(ModBlocks.BLOCK_OF_CREEPER_SOUL)) {
            this.soulBlock = null;
        }
        this.rescanIfDue();
        return this.soulBlock != null && this.isPeaceful();
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.canUse();
    }

    @Override
    public void start()
    {
        this.walkTowardsSoulBlock();
        this.refreshSpeedBoost();
    }

    @Override
    public void stop()
    {
        this.soulBlock = null;
        this.creeper.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        this.rescanIfDue();
        this.walkTowardsSoulBlock();
        this.refreshSpeedBoost();
    }

    /** Whether the creeper is unattached: no target, not priming and not asleep. */
    private boolean isPeaceful()
    {
        return this.creeper.getTarget() == null
            && !this.creeper.isSleeping()
            && this.creeper.getSwellDir() <= 0;
    }

    /** Walks closer while outside the stop distance, then holds position. */
    private void walkTowardsSoulBlock()
    {
        BlockPos block = this.soulBlock;
        if (block == null) {
            return;
        }
        if (this.creeper.blockPosition().distSqr(block) <= STOP_DISTANCE * STOP_DISTANCE) {
            this.creeper.getNavigation().stop();
        } else {
            this.creeper.getNavigation().moveTo(block.getX(), block.getY(), block.getZ(), SPEED);
        }
    }

    /** Re-applies Speed III while the creeper stays inside the aura, beacon style. */
    private void refreshSpeedBoost()
    {
        if (this.creeper.tickCount < this.nextEffectTick) {
            return;
        }
        this.nextEffectTick = this.creeper.tickCount + EFFECT_INTERVAL;
        this.creeper.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, EFFECT_AMPLIFIER, true, true
        ));
    }

    private void rescanIfDue()
    {
        if (this.creeper.tickCount < this.nextScanTick) {
            return;
        }
        this.nextScanTick = this.creeper.tickCount + RESCAN_INTERVAL;
        this.soulBlock = this.findNearestSoulBlock();
    }

    @Nullable
    private BlockPos findNearestSoulBlock()
    {
        BlockPos center = this.creeper.blockPosition();
        int radius = (int)AURA_RANGE;
        BlockPos nearest = null;
        double nearestDistance = AURA_RANGE * AURA_RANGE;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (this.creeper.level().getBlockState(pos).is(ModBlocks.BLOCK_OF_CREEPER_SOUL)) {
                        double distance = center.distSqr(pos);
                        if (distance <= nearestDistance) {
                            nearest = pos;
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
