package io.github.mechtasnezhevna.creeper_rearranged.entity;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Explosion damage calculator that lets a blast pass through water exactly like it passes through
 * air. Vanilla adds the 100 explosion resistance of the water fluid on top of the resistance of the
 * block, which is what makes a vanilla TNT - and a vanilla creeper - harmless under water.
 *
 * <p>Shared by the two things in this mod that are meant to keep their full blast power under
 * water: the primed entity of the underwater TNT and the creepop.
 */
public final class WaterTransparentExplosionDamageCalculator extends ExplosionDamageCalculator
{
    /**
     * Whether vanilla's "a TNT that travelled through a nether portal does not blow up portals"
     * rule is kept. Mobs have no such rule, so the creepop passes {@code false}.
     */
    private final boolean ignoresNetherPortals;

    public WaterTransparentExplosionDamageCalculator(boolean ignoresNetherPortals)
    {
        this.ignoresNetherPortals = ignoresNetherPortals;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(
        Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid
    )
    {
        if (state.is(Blocks.WATER)) {
            // A pure water block is transparent: it neither slows the ray down nor exists for it.
            return Optional.empty();
        }
        if (fluid.is(FluidTags.WATER)) {
            // A waterlogged block only resists with its own material, never with the water's 100.
            return Optional.of(state.getExplosionResistance(level, pos, explosion));
        }
        if (this.ignoresNetherPortals && state.is(Blocks.NETHER_PORTAL)) {
            return Optional.empty();
        }

        return super.getBlockExplosionResistance(explosion, level, pos, state, fluid);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power)
    {
        if (state.is(Blocks.WATER)) {
            // Removing water would only punch holes that flow shut again, so it is left untouched.
            return false;
        }
        if (this.ignoresNetherPortals && state.is(Blocks.NETHER_PORTAL)) {
            return false;
        }

        return super.shouldBlockExplode(explosion, level, pos, state, power);
    }
}
