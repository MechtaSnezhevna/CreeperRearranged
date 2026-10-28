package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.creepaler.Creepaler;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.wiskelper.Wiskelper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public class CreeperSpawnReplacement {
    /** Area around a natural creeper spawn that is searched for a bee nest (12 x 8 x 12). */
    private static final int SPAWN_RANGE_XZ = 6;
    private static final int SPAWN_MIN_Y = -3;
    private static final int SPAWN_MAX_Y = 4;
    /** Scan radius (blocks) around a natural creeper spawn for a living enderman. */
    private static final double ENDERMAN_SCAN_RANGE = 8.0;
    /** Chance that a natural End enderman spawn is replaced by an endper. */
    private static final float ENDPER_REPLACES_ENDERMAN_CHANCE = 1.0F / 24.0F;
    /** Chance that a natural phantom spawn is replaced by a phanper. */
    private static final float PHANPER_REPLACES_PHANTOM_CHANCE = 1.0F / 3.0F;
    /** Chance that a natural creeper spawn in a dark forest is replaced by a creepaler. */
    private static final float CREEPALER_REPLACES_CREEPER_CHANCE = 0.5F;
    /** Chance that a natural wither skeleton spawn in a nether fortress is replaced by a wiskelper. */
    private static final float WISKELPER_REPLACES_WITHER_SKELETON_CHANCE = 0.02F;

    /**
     * Routes natural spawns of vanilla creepers and endermen to creeper variants.
     *
     * <p>A vanilla creeper that is about to spawn within 8 blocks of a living enderman becomes an
     * endper instead; otherwise a bee nest nearby still turns it into a honeeper. Natural enderman
     * spawns in the End have a 1/24 chance to become an endper. Only vanilla entity types and
     * {@link MobSpawnType#NATURAL} spawns are handled, so spawners, spawn eggs and other variants
     * are never touched. A natural phantom spawn has a 1/3 chance to become a phanper instead.
     * In a dark forest - standing in for the pale garden, which does not exist in 1.21.1 - half of
     * the natural creeper spawns become a creepaler instead.
     */
    public static void onFinalizeSpawn(FinalizeSpawnEvent event)
    {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (mob.getType() == EntityType.CREEPER) {
            if (serverLevel.getBiome(mob.blockPosition()).is(Biomes.DARK_FOREST)
                    && serverLevel.random.nextFloat() < CREEPALER_REPLACES_CREEPER_CHANCE) {
                replaceWithCreepaler(event, serverLevel, mob);
                return;
            }
            if (hasEndermanNearby(serverLevel, mob)) {
                replaceWithEndper(event, serverLevel, mob);
                return;
            }
            if (hasBeeNestNearby(serverLevel, mob.blockPosition())) {
                replaceWithHoneeper(event, serverLevel, mob);
            }
        } else if (mob.getType() == EntityType.ENDERMAN
                && serverLevel.dimension() == Level.END
                && serverLevel.random.nextFloat() < ENDPER_REPLACES_ENDERMAN_CHANCE) {
            replaceWithEndper(event, serverLevel, mob);
        } else if (mob.getType() == EntityType.PHANTOM
                && serverLevel.random.nextFloat() < PHANPER_REPLACES_PHANTOM_CHANCE) {
            replaceWithPhanper(event, serverLevel, mob);
        } else if (mob.getType() == EntityType.WITHER_SKELETON
                && serverLevel.random.nextFloat() < WISKELPER_REPLACES_WITHER_SKELETON_CHANCE) {
            replaceWithWiskelper(event, serverLevel, mob);
        }
    }

    private static void replaceWithHoneeper(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Honeeper honeeper = new Honeeper(ModEntities.HONEEPER.get(), serverLevel);
        honeeper.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(honeeper, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        honeeper.setHoneyLevel(Honeeper.randomNaturalHoneyLevel(serverLevel.random));
        serverLevel.tryAddFreshEntityWithPassengers(honeeper);
    }

    private static void replaceWithEndper(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Endper endper = new Endper(ModEntities.ENDPER.get(), serverLevel);
        endper.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(endper, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(endper);
    }

    private static void replaceWithPhanper(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Phanper phanper = new Phanper(ModEntities.PHANPER.get(), serverLevel);
        phanper.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(phanper, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(phanper);
    }

    /**
     * Swaps a natural wither skeleton spawn for a wiskelper. Wither skeletons only spawn naturally
     * inside nether fortresses (their fortress structure spawn list), so the replacement inherently
     * stays limited to fortresses without needing a spawn placement or biome modifier.
     */
    private static void replaceWithWiskelper(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Wiskelper wiskelper = new Wiskelper(ModEntities.WISKELPER.get(), serverLevel);
        wiskelper.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(wiskelper, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(wiskelper);
    }

    private static void replaceWithCreepaler(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Creepaler creepaler = new Creepaler(ModEntities.CREEPALER.get(), serverLevel);
        creepaler.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(creepaler, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(creepaler);
    }

    private static boolean hasBeeNestNearby(ServerLevel level, BlockPos center)
    {
        for (int dy = SPAWN_MIN_Y; dy <= SPAWN_MAX_Y; dy++) {
            for (int dx = -SPAWN_RANGE_XZ; dx <= SPAWN_RANGE_XZ; dx++) {
                for (int dz = -SPAWN_RANGE_XZ; dz <= SPAWN_RANGE_XZ; dz++) {
                    if (level.getBlockState(center.offset(dx, dy, dz)).is(Blocks.BEE_NEST)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Whether a living enderman stands within 8 blocks (Euclidean) of the spawning mob. */
    private static boolean hasEndermanNearby(ServerLevel level, Mob mob)
    {
        Vec3 position = mob.position();
        return !level.getEntitiesOfClass(
                EnderMan.class,
                mob.getBoundingBox().inflate(ENDERMAN_SCAN_RANGE),
                enderman -> enderman.isAlive() && enderman.distanceToSqr(position) <= ENDERMAN_SCAN_RANGE * ENDERMAN_SCAN_RANGE
        ).isEmpty();
    }
}
