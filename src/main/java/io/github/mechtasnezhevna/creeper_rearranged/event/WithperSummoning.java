package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.withper.Withper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModBlocks;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.neoforged.neoforge.event.level.BlockEvent;

public class WithperSummoning {

    /** Lazily built BlockPattern for summoning a withper; built at first use, like vanilla's. */
    private static BlockPattern withperPattern;

    /**
     * Summons a withper when the last wither skeleton skull of the soul-block T is placed. The
     * pattern is the wither's T with the middle soul sand (directly under the middle skull)
     * replaced by a block of creeper soul; the vanilla wither pattern never matches it because the
     * soul block is not a {@code WITHER_SUMMON_BASE_BLOCKS}, and this pattern never matches a
     * vanilla wither T because its centre is plain soul sand. Fired for any entity-placed block,
     * so dispensers can build the structure too.
     */
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event)
    {
        BlockState placed = event.getPlacedBlock();
        if (!placed.is(Blocks.WITHER_SKELETON_SKULL) && !placed.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
            return;
        }
        if (!(event.getLevel() instanceof Level level)
                || level.isClientSide
                || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        BlockPos pos = event.getPos();
        if (pos.getY() < level.getMinBuildHeight()) {
            return;
        }
        BlockPattern.BlockPatternMatch match = getOrCreateWithperPattern().find(level, pos);
        if (match == null) {
            return;
        }
        Withper withper = new Withper(ModEntities.WITHPER.get(), level);
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
        withper.moveTo(
                (double) spawnPos.getX() + 0.5,
                (double) spawnPos.getY() + 0.55,
                (double) spawnPos.getZ() + 0.5,
                match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F,
                0.0F
        );
        withper.yBodyRot = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
        withper.makeInvulnerable();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, withper.getBoundingBox().inflate(50.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, withper);
        }
        level.addFreshEntity(withper);
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    /** The soul-block T: wither skeleton skulls on top, soul sand body, creeper soul at the centre. */
    private static BlockPattern getOrCreateWithperPattern()
    {
        BlockPattern pattern = withperPattern;
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("^^^", "#$#", "~#~")
                    .where('#', block -> block.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
                    .where(
                            '^',
                            BlockInWorld.hasState(
                                    BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL)
                                            .or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL))
                            )
                    )
                    .where('$', BlockInWorld.hasState(BlockStatePredicate.forBlock(ModBlocks.SOUL_CORRUPTED_TNT.get())))
                    .where('~', block -> block.getState().isAir())
                    .build();
            withperPattern = pattern;
        }
        return pattern;
    }

}
