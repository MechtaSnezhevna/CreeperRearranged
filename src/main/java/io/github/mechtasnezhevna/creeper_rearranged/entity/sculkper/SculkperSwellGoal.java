package io.github.mechtasnezhevna.creeper_rearranged.entity.sculkper;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A sculkper's only aggression: it locks onto whoever attacked it, closes the distance, and starts
 * its 5-second fuse once the target is within 3 blocks. While the target is farther away it keeps
 * pursuing; the actual fuse countdown and the explosion itself are handled by the sculkper.
 */
public class SculkperSwellGoal extends Goal
{
    private final Sculkper sculkper;

    public SculkperSwellGoal(Sculkper sculkper)
    {
        this.sculkper = sculkper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return !this.sculkper.isCharging()
            && !this.sculkper.isResting()
            && !this.sculkper.isShrieking()
            && this.sculkper.getTarget() != null
            && this.sculkper.getTarget().isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        LivingEntity target = this.sculkper.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.sculkper.distanceToSqr(target) <= Sculkper.SWELL_RANGE_SQUARED
            && this.sculkper.getSensing().hasLineOfSight(target)) {
            this.sculkper.startCharging();
        } else {
            this.sculkper.getNavigation().moveTo(target, 1.0);
        }
    }
}
