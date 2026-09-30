package io.github.mechtasnezhevna.creeper_rearranged.block;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModBlocks;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Drives cats, piglins and piglin brutes away from nearby blocks of creeper soul.
 *
 * <p>The goal is injected into every blacklisted mob kind from {@code CreeperHooks#onEntityJoinLevel}.
 * While a soul block is within {@value #AURA_RANGE} blocks the mob navigates away from it until it
 * is {@value #ESCAPE_DISTANCE} blocks away, then the goal releases it back to its normal behaviour.
 */
public class CreeperSoulRepelGoal extends Goal
{
    /** Radius (blocks) of the soul aura. */
    public static final double AURA_RANGE = 15.0;
    /** A fleeing mob keeps running until it is this far (blocks) from the soul block. */
    private static final double ESCAPE_DISTANCE = 18.0;
    /** How often (ticks) the nearest soul block is re-scanned while fleeing. */
    private static final int RESCAN_INTERVAL = 10;
    /** How often (ticks) the escape waypoint is recomputed. */
    private static final int FLEE_RECALC_INTERVAL = 5;
    /** Run speed. */
    private static final double SPEED = 1.2;

    private final Mob mob;
    @Nullable
    private BlockPos soulBlock;
    private int nextScanTick;

    public CreeperSoulRepelGoal(Mob mob)
    {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (this.mob.isSleeping() || (this.mob instanceof TamableAnimal tamable && tamable.isInSittingPose())) {
            return false;
        }
        this.rescanIfDue();
        return this.soulBlock != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (this.soulBlock == null) {
            return false;
        }
        if (!this.mob.level().getBlockState(this.soulBlock).is(ModBlocks.BLOCK_OF_CREEPER_SOUL)) {
            return false;
        }
        return this.mob.blockPosition().distSqr(this.soulBlock) <= ESCAPE_DISTANCE * ESCAPE_DISTANCE;
    }

    @Override
    public void start()
    {
        this.flee();
    }

    @Override
    public void stop()
    {
        this.soulBlock = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        if (this.mob.tickCount >= this.nextScanTick) {
            this.nextScanTick = this.mob.tickCount + RESCAN_INTERVAL;
            this.soulBlock = this.findNearestSoulBlock();
        }
        if (this.soulBlock != null && this.mob.tickCount % FLEE_RECALC_INTERVAL == 0) {
            this.flee();
        }
    }

    /** Navigates to a waypoint on the far side of the soul block, outside the aura. */
    private void flee()
    {
        BlockPos block = this.soulBlock;
        if (block == null) {
            return;
        }
        Vec3 away = this.mob.position().subtract(Vec3.atCenterOf(block));
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(
                this.mob.getRandom().nextDouble() - 0.5,
                0.0,
                this.mob.getRandom().nextDouble() - 0.5
            );
        }
        Vec3 target = this.mob.position().add(away.normalize().scale(ESCAPE_DISTANCE));
        this.mob.getNavigation().moveTo(target.x, target.y, target.z, SPEED);
    }

    private void rescanIfDue()
    {
        if (this.mob.tickCount < this.nextScanTick) {
            return;
        }
        this.nextScanTick = this.mob.tickCount + RESCAN_INTERVAL;
        this.soulBlock = this.findNearestSoulBlock();
    }

    @Nullable
    private BlockPos findNearestSoulBlock()
    {
        BlockPos center = this.mob.blockPosition();
        int radius = (int)AURA_RANGE;
        BlockPos nearest = null;
        double nearestDistance = AURA_RANGE * AURA_RANGE;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (this.mob.level().getBlockState(pos).is(ModBlocks.BLOCK_OF_CREEPER_SOUL)) {
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
