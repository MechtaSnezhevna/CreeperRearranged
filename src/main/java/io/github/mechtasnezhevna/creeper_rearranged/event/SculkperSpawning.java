package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.sculkper.Sculkper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * The sculkper's extra-spawn rule: when a sculkper spawns naturally and a sculk shrieker stands
 * within {@value #SHRIEKER_SCAN_RANGE} blocks horizontally, one additional sculkper appears next
 * to it. The extra sculkper is added directly - not through the spawn pipeline - so the rule never
 * chains onto itself.
 */
public final class SculkperSpawning
{
    /** Horizontal search range (blocks) around the fresh spawn for a sculk shrieker. */
    private static final int SHRIEKER_SCAN_RANGE = 5;

    private SculkperSpawning()
    {
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event)
    {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        if (event.getEntity().getType() != ModEntities.SCULKPER.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity sculkper = event.getEntity();
        if (hasSculkShriekerNearby(serverLevel, sculkper.blockPosition())) {
            spawnExtraSculkper(serverLevel, sculkper);
        }
    }

    /** Whether a sculk shrieker exists within the horizontal scan range at roughly the same height. */
    private static boolean hasSculkShriekerNearby(ServerLevel level, BlockPos pos)
    {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -SHRIEKER_SCAN_RANGE; dx <= SHRIEKER_SCAN_RANGE; dx++) {
                for (int dz = -SHRIEKER_SCAN_RANGE; dz <= SHRIEKER_SCAN_RANGE; dz++) {
                    if (level.getBlockState(cursor.setWithOffset(pos, dx, dy, dz)).is(Blocks.SCULK_SHRIEKER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void spawnExtraSculkper(ServerLevel level, Entity original)
    {
        Sculkper extra = new Sculkper(ModEntities.SCULKPER.get(), level);
        extra.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        level.tryAddFreshEntityWithPassengers(extra);
    }
}
