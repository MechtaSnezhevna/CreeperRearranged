package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepaler.Creepaler;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.EndermanApproachWarperGoal;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Game-bus handlers shared by creeper variants.
 */
public final class CreeperHooks
{
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
    /** Goal priority of the warper approach; the enderman's vanilla random stroll sits at 7. */
    private static final int WARPER_APPROACH_GOAL_PRIORITY = 6;
    /**
     * Endermen that already received {@link EndermanApproachWarperGoal}, so a mob that re-joins a
     * level does not collect duplicate goals. Weak keys let unloaded endermen be collected.
     */
    private static final Set<EnderMan> ENDERMEN_APPROACHING_WARPERS =
        Collections.newSetFromMap(new WeakHashMap<>());

    private CreeperHooks()
    {
    }

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
        }
    }

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

    /**
     * Registers how natural crimper and warper spawns are validated. Like the other Nether monsters
     * (blaze, magma cube) they spawn on the ground regardless of light level, which is what their
     * biomes need; the biome modifiers add them to those biomes' spawn lists.
     */
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event)
    {
        event.register(
            ModEntities.CRIMPER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkAnyLightMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
            ModEntities.WARPER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkAnyLightMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        // Cherry grove spawns: like the nether monsters, any light level is fine, so the docile
        // cherreeper can be found while exploring the grove during the day as well as at night.
        event.register(
            ModEntities.CHERREEPER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkAnyLightMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        // Ocean floor spawns, under water and in the dark exactly like the drowned's.
        event.register(
            ModEntities.CREEPOP.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Creepop::checkCreepopSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    /**
     * Teaches endermen to walk towards nearby warpers.
     *
     * <p>Fired for every entity that joins a level - freshly spawned ones as well as those read
     * back from disk - so endermen in existing worlds get the goal too. The goal outranks the
     * vanilla random stroll but not the combat goals, so an angry enderman keeps fighting instead
     * of wandering off to a warper. No mixin is involved.
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof EnderMan enderman)) {
            return;
        }
        if (!ENDERMEN_APPROACHING_WARPERS.add(enderman)) {
            return;
        }
        enderman.goalSelector.addGoal(WARPER_APPROACH_GOAL_PRIORITY, new EndermanApproachWarperGoal(enderman));
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

    private static void replaceWithCreepaler(FinalizeSpawnEvent event, ServerLevel serverLevel, Mob mob)
    {
        event.setSpawnCancelled(true);
        Creepaler creepaler = new Creepaler(ModEntities.CREEPALER.get(), serverLevel);
        creepaler.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(creepaler, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(creepaler);
    }

    /**
     * Snapping an (empty) bee nest onto a vanilla creeper's head turns it into a honeeper.
     * Handled on the server; nests that still carry bees are not consumed.
     */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        Level level = event.getEntity().level();
        if (level.isClientSide) {
            return;
        }

        if (event.getTarget() instanceof Honeeper honeeper) {
            harvestFullHoneeper(event, honeeper);
        } else if (event.getTarget() instanceof Creeper creeper && creeper.getType() == EntityType.CREEPER) {
            snapNestOntoCreeper(event, creeper);
        }
    }

    /**
     * Harvests a full-honey honeeper like a vanilla full beehive: shears drop honeycomb and a glass
     * bottle is filled with honey. Either harvest empties the nest, which bees must refill.
     */
    private static void harvestFullHoneeper(PlayerInteractEvent.EntityInteract event, Honeeper honeeper)
    {
        if (!honeeper.isFullHoney()) {
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        Level level = honeeper.level();

        if (stack.is(Items.SHEARS)) {
            level.playSound(null, honeeper.getX(), honeeper.getY(), honeeper.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            honeeper.spawnAtLocation(new ItemStack(Items.HONEYCOMB, 3));
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            stack.shrink(1);
            level.playSound(null, honeeper.getX(), honeeper.getY(), honeeper.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, new ItemStack(Items.HONEY_BOTTLE));
            } else if (!player.getInventory().add(new ItemStack(Items.HONEY_BOTTLE))) {
                player.drop(new ItemStack(Items.HONEY_BOTTLE), false);
            }
        } else {
            return;
        }

        honeeper.setHoneyLevel(0);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void snapNestOntoCreeper(PlayerInteractEvent.EntityInteract event, Creeper creeper)
    {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.BEE_NEST) || !stack.getOrDefault(DataComponents.BEES, List.of()).isEmpty()) {
            return;
        }

        convertToHoneeper(creeper);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        creeper.level().playSound(null, creeper.blockPosition(), SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Slows down entities damaged by a full-honey honeeper's explosion. */
    public static void onLivingDamage(LivingDamageEvent.Pre event)
    {
        DamageSource source = event.getSource();
        Entity directEntity = source.getDirectEntity();
        if (!(directEntity instanceof Honeeper honeeper) || !honeeper.isFullHoney()) {
            return;
        }
        if (!source.is(DamageTypes.EXPLOSION) && !source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        victim.addEffect(
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Honeeper.SLOWNESS_DURATION_TICKS, Honeeper.SLOWNESS_AMPLIFIER),
            honeeper
        );
    }

    /**
     * Before a full-honey honeeper's blast breaks blocks, remembers which positions hold real
     * (non-air) blocks. {@code Explosion.explode()} also lists pure-air cells in its {@code toBlow},
     * so this snapshot keeps the honey scatter from filling cells that were already air.
     */
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        Entity source = event.getExplosion().getIndirectSourceEntity();
        if (!(source instanceof Honeeper honeeper) || !honeeper.isFullHoney()) {
            return;
        }

        Set<BlockPos> destroyedBlocks = new HashSet<>();
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (!level.getBlockState(pos).isAir()) {
                destroyedBlocks.add(pos);
            }
        }
        honeeper.recordDestroyedBlocks(destroyedBlocks);
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

    private static void convertToHoneeper(Creeper creeper)
    {
        Level level = creeper.level();
        Honeeper honeeper = new Honeeper(ModEntities.HONEEPER.get(), level);
        honeeper.moveTo(creeper.getX(), creeper.getY(), creeper.getZ(), creeper.getYRot(), creeper.getXRot());
        honeeper.setHealth(creeper.getHealth());
        honeeper.setPersistenceRequired();
        if (creeper.hasCustomName()) {
            honeeper.setCustomName(creeper.getCustomName());
            honeeper.setCustomNameVisible(creeper.isCustomNameVisible());
        }
        creeper.discard();
        level.addFreshEntity(honeeper);
    }
}
