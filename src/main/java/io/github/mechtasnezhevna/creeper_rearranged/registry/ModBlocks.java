package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
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

    private ModBlocks()
    {
    }
}
