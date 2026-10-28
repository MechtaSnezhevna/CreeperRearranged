package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.block.SoulCorruptedTntBlock;
import io.github.mechtasnezhevna.creeper_rearranged.block.UnderwaterTntBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for the mod's blocks.
 *
 * <p>{@link #WARPED_SHROOMLIGHT} is a copy of vanilla's {@code minecraft:shroomlight} that differs
 * only in its texture: map colour, strength, sound and light level are the very same ones used by
 * {@code Blocks#SHROOMLIGHT}. The shroomlights a huge warped fungus grows - whether it came from
 * world generation or from using bone meal on a warped fungus - are swapped over to it by
 * overriding the vanilla {@code minecraft:warped_fungus} and {@code minecraft:warped_fungus_planted}
 * configured features from the mod's own data pack.
 *
 * <p>{@link #UNDERWATER_TNT} copies vanilla TNT the same way, but its primed entity blasts
 * through water instead of being absorbed by it.
 */
public final class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(CreeperRearranged.MODID);

    public static final DeferredBlock<Block> WARPED_SHROOMLIGHT = BLOCKS.registerSimpleBlock(
        "warped_shroomlight",
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(1.0F)
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.SHROOMLIGHT)
            .lightLevel(state -> 15)
    );

    /**
     * Vanilla TNT with its own textures: same map colour, instant breaking, sound, lava ignition
     * and "never a redstone conductor" as {@code Blocks.TNT}, plus a blast that ignores water.
     */
    public static final DeferredBlock<UnderwaterTntBlock> UNDERWATER_TNT = BLOCKS.register(
        "underwater_tnt",
        () -> new UnderwaterTntBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .instabreak()
                .sound(SoundType.GRASS)
                .ignitedByLava()
                .isRedstoneConductor((state, level, pos) -> false)
        )
    );

    /**
     * A cyan soul block that hums with creeper magic. Its aura draws peaceful creepers towards it
     * while driving cats and piglins away; both auras are implemented by {@code CreeperSoulAttractGoal}
     * and {@code CreeperSoulRepelGoal}, which {@code CreeperHooks#onEntityJoinLevel} injects into the
     * whitelisted and blacklisted mobs.
     *
     * <p>The block is also a working TNT: every ignition path primes {@code SoulCorruptedPrimedTnt},
     * and its blast withers every creature it damages ({@code SoulTntWithering}). The cyan map
     * colour and soul sand sound keep the soul identity of the block.
     */
    public static final DeferredBlock<SoulCorruptedTntBlock> SOUL_CORRUPTED_TNT = BLOCKS.register(
        "soul_corrupted_tnt",
        () -> new SoulCorruptedTntBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .instabreak()
                .sound(SoundType.SOUL_SAND)
                .ignitedByLava()
                .isRedstoneConductor((state, level, pos) -> false)
        )
    );

    private ModBlocks()
    {
    }
}
