package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Game-bus handlers shared by creeper variants.
 */
public final class CreeperHooks
{
    /** Area around a natural creeper spawn that is searched for a bee nest (12 x 8 x 12). */
    private static final int SPAWN_RANGE_XZ = 6;
    private static final int SPAWN_MIN_Y = -3;
    private static final int SPAWN_MAX_Y = 4;

    private CreeperHooks()
    {
    }

    /**
     * Replaces a naturally spawning vanilla creeper with a honeeper when a bee nest is nearby.
     * Only vanilla {@link EntityType#CREEPER} and {@link MobSpawnType#NATURAL} spawns are handled,
     * so spawners, spawn eggs and other variants are never touched.
     */
    public static void onFinalizeSpawn(FinalizeSpawnEvent event)
    {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(mob instanceof Creeper creeper) || creeper.getType() != EntityType.CREEPER) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!hasBeeNestNearby(serverLevel, mob.blockPosition())) {
            return;
        }

        event.setSpawnCancelled(true);
        Honeeper honeeper = new Honeeper(ModEntities.HONEEPER.get(), serverLevel);
        honeeper.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        EventHooks.finalizeMobSpawn(honeeper, serverLevel, event.getDifficulty(), MobSpawnType.NATURAL, event.getSpawnData());
        serverLevel.tryAddFreshEntityWithPassengers(honeeper);
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
        if (!(event.getTarget() instanceof Creeper creeper) || creeper.getType() != EntityType.CREEPER) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(Items.BEE_NEST) || !stack.getOrDefault(DataComponents.BEES, List.of()).isEmpty()) {
            return;
        }

        convertToHoneeper(creeper);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, creeper.blockPosition(), SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
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
