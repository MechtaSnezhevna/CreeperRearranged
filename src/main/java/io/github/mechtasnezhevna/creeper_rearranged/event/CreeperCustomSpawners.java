package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepot.Creepot;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CreeperCustomSpawners {

    /** Chance that one phanper spawns above a random player on any given night. */
    private static final float PHANPER_NIGHT_SPAWN_CHANCE = 1.0F / 13.0F;
    /** Nightly phanper spawn checks run at most once per this many ticks. */
    private static final int PHANPER_SPAWN_CHECK_INTERVAL = 100;
    /** Day index whose nightly 1/13 roll has already been resolved. */
    private static long lastPhanperNightRoll = Long.MIN_VALUE;
    private static int phanperSpawnCheckTicks;
    /** How often (ticks) the grove spawner rolls for a cherreeper near a player. */
    private static final int CHERREEPER_SPAWN_CHECK_INTERVAL = 100;
    /** Candidate spots tried per grove spawner roll. */
    private static final int CHERREEPER_SPAWN_ATTEMPTS = 5;
    /** Annulus around the player (blocks) where the grove spawner looks. */
    private static final double CHERREEPER_SPAWN_MIN_DISTANCE = 24.0;
    private static final double CHERREEPER_SPAWN_MAX_DISTANCE = 48.0;
    /** Wild cherreeper within range of a player at which the grove spawner stops. */
    private static final int CHERREEPER_CAP_PER_PLAYER = 4;
    private static int cherreeperSpawnCheckTicks;
    /** How many ticks a queued creepot spawn waits for its chunk to tick before giving up. */
    private static final int CREEPOT_SPAWN_MAX_RETRIES = 600;



    public static void onLevelTick(LevelTickEvent.Pre event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        // The phanper's own nightly spawn: once per night there is a 1/13 chance that one phanper
        // is summoned above a random player, mirroring the vanilla {@code PhantomSpawner}.
        if (--phanperSpawnCheckTicks <= 0) {
            phanperSpawnCheckTicks = PHANPER_SPAWN_CHECK_INTERVAL;
            if (level.isNight()) {
                long night = level.getDayTime() / 24000L;
                if (night != lastPhanperNightRoll) {
                    lastPhanperNightRoll = night;
                    if (level.random.nextFloat() < PHANPER_NIGHT_SPAWN_CHANCE) {
                        spawnPhanperAbovePlayer(level);
                    }
                }
            }
        }
        // The cherreeper grove spawner runs day and night; see spawnCherreeperInGroves.
        if (--cherreeperSpawnCheckTicks <= 0) {
            cherreeperSpawnCheckTicks = CHERREEPER_SPAWN_CHECK_INTERVAL;
            spawnCherreeperInGroves(level);
        }
        drainCreepotSpawns(level);
    }

    private static void spawnPhanperAbovePlayer(ServerLevel level)
    {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        BlockPos pos = player.blockPosition()
                .above(20 + level.random.nextInt(15))
                .east(-10 + level.random.nextInt(21))
                .south(-10 + level.random.nextInt(21));
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, blockState, fluidState, ModEntities.PHANPER.get())) {
            return;
        }
        Phanper phanper = new Phanper(ModEntities.PHANPER.get(), level);
        phanper.moveTo(pos, 0.0F, 0.0F);
        EventHooks.finalizeMobSpawn(phanper, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.tryAddFreshEntityWithPassengers(phanper);
    }

    /**
     * The grove spawner: cherry trees turn the vanilla natural spawn attempts into near misses,
     * because the candidate Y is drawn uniformly from the bedrock level up to the tree canopy, so
     * it almost never lands on the actual ground. This instead drops a cherreeper onto a real
     * ground spot inside a {@code cherry_grove} around a random player, at any time of day, so the
     * grove population is actually visible.
     */
    private static void spawnCherreeperInGroves(ServerLevel level)
    {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        BlockPos center = player.blockPosition();
        // Keep a calm wild population: once enough untamed cherreeper are near the player, stop.
        int wild = 0;
        for (Cherreeper cherreeper : level.getEntitiesOfClass(Cherreeper.class, new AABB(center).inflate(48.0))) {
            if (cherreeper.isAlive() && !cherreeper.isTamed()) {
                wild++;
            }
        }
        if (wild >= CHERREEPER_CAP_PER_PLAYER) {
            return;
        }
        for (int attempt = 0; attempt < CHERREEPER_SPAWN_ATTEMPTS; attempt++) {
            double dx = Mth.nextDouble(level.random, -CHERREEPER_SPAWN_MAX_DISTANCE, CHERREEPER_SPAWN_MAX_DISTANCE);
            double dz = Mth.nextDouble(level.random, -CHERREEPER_SPAWN_MAX_DISTANCE, CHERREEPER_SPAWN_MAX_DISTANCE);
            if (dx * dx + dz * dz < CHERREEPER_SPAWN_MIN_DISTANCE * CHERREEPER_SPAWN_MIN_DISTANCE) {
                continue;
            }
            BlockPos pos = getTopNonCollidingPos(
                    level,
                    ModEntities.CHERREEPER.get(),
                    center.getX() + (int) Math.floor(dx),
                    center.getZ() + (int) Math.floor(dz)
            );
            double distanceSqr = player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (distanceSqr < 24.0 * 24.0 || distanceSqr > 128.0 * 128.0) {
                continue;
            }
            if (!level.getBiome(pos).is(Biomes.CHERRY_GROVE)
                    || !SpawnPlacements.isSpawnPositionOk(ModEntities.CHERREEPER.get(), level, pos)
                    || !Monster.checkAnyLightMonsterSpawnRules(
                    ModEntities.CHERREEPER.get(), level, MobSpawnType.NATURAL, pos, level.random
            )
                    || !level.noCollision(ModEntities.CHERREEPER.get().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                continue;
            }
            Cherreeper cherreeper = new Cherreeper(ModEntities.CHERREEPER.get(), level);
            cherreeper.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            EventHooks.finalizeMobSpawn(cherreeper, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.tryAddFreshEntityWithPassengers(cherreeper);
            return;
        }
    }

    /**
     * Copy of the vanilla ground finder used by chunk-generation spawns: walk down from the
     * entity's heightmap to the first non-empty block and stand the mob one block above it.
     */
    private static BlockPos getTopNonCollidingPos(LevelReader level, EntityType<?> entityType, int x, int z)
    {
        int y = level.getHeight(SpawnPlacements.getHeightmapType(entityType), x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos.move(Direction.DOWN);
        }
        return pos.above();
    }

    /**
     * Grows the trial chamber pots that {@code CreepotStructureMixin} swapped for air into sleeping
     * creepots. Structure placement happens while the chunk is being generated, which is too early
     * to add entities, so each position waits until its chunk is loaded and the spot is still empty
     * ground, then a sleeping, persistent, trial-flagged creepot takes its place. Entries that wait
     * longer than {@value #CREEPOT_SPAWN_MAX_RETRIES} ticks (a chunk that never ticks, for example)
     * are dropped.
     */
    private static void drainCreepotSpawns(ServerLevel level)
    {
        if (CREEPOT_SPAWNS.isEmpty()) {
            return;
        }
        for (CreepotSpawnEntry entry : CREEPOT_SPAWNS) {
            if (entry.level != level) {
                continue;
            }
            if (--entry.retries < 0 || !level.isLoaded(entry.pos)) {
                CREEPOT_SPAWNS.remove(entry);
                continue;
            }
            if (!level.getBlockState(entry.pos).isAir() || !level.getBlockState(entry.pos.below()).isSolid()) {
                continue;
            }
            Creepot creepot = new Creepot(ModEntities.CREEPOT.get(), level);
            creepot.moveTo(entry.pos.getX() + 0.5, entry.pos.getY(), entry.pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            creepot.setSleeping(true);
            creepot.setTrial(true);
            creepot.setPersistenceRequired();
            EventHooks.finalizeMobSpawn(creepot, level, level.getCurrentDifficultyAt(entry.pos), MobSpawnType.STRUCTURE, null);
            creepot.fillTrialLoot();
            level.tryAddFreshEntityWithPassengers(creepot);
            CREEPOT_SPAWNS.remove(entry);
        }
    }

    /**
     * Trial chamber pots that {@code CreepotStructureMixin} replaced with air. Structure placement
     * runs while the chunk is being generated, too early to add entities, so the positions are
     * queued here and drained by {@link #onLevelTick} once the chunk is actually ticking. Written
     * from worldgen threads, read from the server thread: a concurrent queue keeps both safe.
     */
    private static final Queue<CreepotSpawnEntry> CREEPOT_SPAWNS = new ConcurrentLinkedQueue<>();

    /** Records a trial chamber pot position that should grow into a sleeping creepot. */
    public static void queueCreepotSpawn(ServerLevel level, BlockPos pos)
    {
        CREEPOT_SPAWNS.add(new CreepotSpawnEntry(level, pos));
    }

    /** One replaced pot waiting for its chunk to tick. */
    private static final class CreepotSpawnEntry
    {
        private final ServerLevel level;
        private final BlockPos pos;
        private int retries = CREEPOT_SPAWN_MAX_RETRIES;

        private CreepotSpawnEntry(ServerLevel level, BlockPos pos)
        {
            this.level = level;
            this.pos = pos;
        }
    }

}
