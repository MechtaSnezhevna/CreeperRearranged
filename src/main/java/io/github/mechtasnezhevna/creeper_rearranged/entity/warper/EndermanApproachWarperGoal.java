package io.github.mechtasnezhevna.creeper_rearranged.entity.warper;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;

/**
 * Replaces an enderman's random stroll with a walk towards the nearest warper.
 *
 * <p>While a living warper is within {@value #SEARCH_RANGE} blocks the enderman navigates to within
 * {@value #STOP_DISTANCE} blocks of it and then stands there. The goal is injected at a higher
 * priority than the vanilla {@code WaterAvoidingRandomStrollGoal}, so "wander around" becomes
 * "drift towards the warper", while combat goals still win over it.
 *
 * <p>A warper whose fuse is already burning is skipped entirely: the enderman does not run from it,
 * it just loses interest and goes back to its random stroll.
 */
public class EndermanApproachWarperGoal extends Goal
{
    /** How far (blocks) around the enderman warpers are searched for. */
    private static final double SEARCH_RANGE = 16.0;
    /** The enderman stops once it is this close (blocks) to its warper. */
    private static final double STOP_DISTANCE = 5.0;
    /** How often (ticks) the nearest warper is re-scanned while walking. */
    private static final int RESCAN_INTERVAL = 10;
    /** Walk speed, the same one the enderman's vanilla random stroll uses. */
    private static final double SPEED = 1.0;

    private final EnderMan enderman;
    @Nullable
    private Warper warper;

    public EndermanApproachWarperGoal(EnderMan enderman)
    {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        this.warper = this.findNearestWarper();
        return this.warper != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.warper != null
            && this.warper.isAlive()
            && !isPriming(this.warper)
            && this.enderman.distanceToSqr(this.warper) <= SEARCH_RANGE * SEARCH_RANGE;
    }

    @Override
    public void start()
    {
        this.walkTowardsWarper();
    }

    @Override
    public void stop()
    {
        this.warper = null;
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        if (this.enderman.tickCount % RESCAN_INTERVAL == 0) {
            Warper nearest = this.findNearestWarper();
            if (nearest != null) {
                this.warper = nearest;
            }
        }
        this.walkTowardsWarper();
    }

    /** Walks closer while outside the stop distance, then holds position. */
    private void walkTowardsWarper()
    {
        Warper target = this.warper;
        if (target == null) {
            return;
        }
        if (this.enderman.distanceToSqr(target) <= STOP_DISTANCE * STOP_DISTANCE) {
            this.enderman.getNavigation().stop();
        } else {
            this.enderman.getNavigation().moveTo(target, SPEED);
        }
    }

    @Nullable
    private Warper findNearestWarper()
    {
        List<Warper> warpers = this.enderman.level().getEntitiesOfClass(
            Warper.class,
            this.enderman.getBoundingBox().inflate(SEARCH_RANGE),
            warper -> warper.isAlive() && !isPriming(warper)
        );

        Warper nearest = null;
        double nearestDistance = SEARCH_RANGE * SEARCH_RANGE;
        for (Warper warper : warpers) {
            double distance = this.enderman.distanceToSqr(warper);
            if (distance <= nearestDistance) {
                nearest = warper;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** Whether the warper's fuse is burning; an enderman neither approaches nor flees those. */
    private static boolean isPriming(Warper warper)
    {
        return warper.getSwellDir() > 0;
    }
}
