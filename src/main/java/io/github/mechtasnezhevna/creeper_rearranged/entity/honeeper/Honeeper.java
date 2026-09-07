package io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Honeeper - a creeper variant capped with a bee nest.
 *
 * <p>When bees fly over its head {@value #BEE_VISITS_TO_FULL_HONEY} times it becomes full of honey:
 * its explosion deals half damage, lines the outer rim of the blast crater with honey blocks, and
 * slows affected creatures. Full-honey state is tracked in NBT under {@code FullHoney}.
 */
public class Honeeper extends VariantCreeper implements GeoEntity
{
    private static final EntityDataAccessor<Boolean> DATA_FULL_HONEY =
        SynchedEntityData.defineId(Honeeper.class, EntityDataSerializers.BOOLEAN);

    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "honeeper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/honeeper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.honeeper.idle";
    public static final String ANIMATION_MOVE = "animation.honeeper.move";
    public static final String ANIMATION_IDLE_FULL_HONEY = "animation.honeeper.idleh";
    public static final String ANIMATION_MOVE_FULL_HONEY = "animation.honeeper.moveh";

    /** How many times bees need to fly over the head to fill the hive. */
    public static final int BEE_VISITS_TO_FULL_HONEY = 5;
    /** Slowness applied to explosion victims of a full-honey honeeper (Slowness I for 5 seconds). */
    public static final int SLOWNESS_DURATION_TICKS = 5 * 20;
    public static final int SLOWNESS_AMPLIFIER = 0;

    private static final int BEE_CHECK_INTERVAL = 10;
    private static final float HONEY_EXPLOSION_DAMAGE_FACTOR = 0.5F;
    private static final String TAG_BEE_VISITS = "BeeVisits";
    private static final String TAG_FULL_HONEY = "FullHoney";
    private static final int ANIMATION_TRANSITION_TICKS = 5;

    private int beeVisits;
    private final Set<UUID> beesOverhead = new HashSet<>();
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public Honeeper(EntityType<? extends Honeeper> entityType, Level level)
    {
        super(entityType, level);
    }

    public boolean isFullHoney()
    {
        return this.entityData.get(DATA_FULL_HONEY);
    }

    public void setFullHoney(boolean fullHoney)
    {
        this.entityData.set(DATA_FULL_HONEY, fullHoney);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(
            new AnimationController<>(this, ANIMATION_CONTROLLER, ANIMATION_TRANSITION_TICKS, this::animationPredicate)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.animatableCache;
    }

    /**
     * Selects the looping animation based on movement and the full-honey state. Full-honey variants
     * ({@code idleh}/{@code moveh}) push the honey bone forward, mirroring the honey-covered look.
     */
    private PlayState animationPredicate(AnimationState<Honeeper> state)
    {
        boolean fullHoney = this.isFullHoney();
        String animation = state.isMoving()
            ? (fullHoney ? ANIMATION_MOVE_FULL_HONEY : ANIMATION_MOVE)
            : (fullHoney ? ANIMATION_IDLE_FULL_HONEY : ANIMATION_IDLE);

        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_FULL_HONEY, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_BEE_VISITS, this.beeVisits);
        tag.putBoolean(TAG_FULL_HONEY, this.isFullHoney());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.beeVisits = tag.getInt(TAG_BEE_VISITS);
        this.setFullHoney(tag.getBoolean(TAG_FULL_HONEY));
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide || !this.isAlive() || this.isFullHoney()) {
            return;
        }
        if (this.tickCount % BEE_CHECK_INTERVAL != 0) {
            return;
        }

        Set<UUID> overhead = new HashSet<>();
        for (Bee bee : this.level().getEntitiesOfClass(Bee.class, this.overheadBox())) {
            if (bee.isAlive()) {
                overhead.add(bee.getUUID());
            }
        }

        for (UUID uuid : overhead) {
            if (this.beesOverhead.add(uuid) && ++this.beeVisits >= BEE_VISITS_TO_FULL_HONEY) {
                this.setFullHoney(true);
                this.level().playSound(null, this.blockPosition(), SoundEvents.BEEHIVE_DRIP, SoundSource.NEUTRAL, 1.0F, 1.0F);
                break;
            }
        }
        this.beesOverhead.retainAll(overhead);
    }

    private AABB overheadBox()
    {
        return new AABB(
            this.getX() - 1.0,
            this.getY() + 1.6,
            this.getZ() - 1.0,
            this.getX() + 1.0,
            this.getY() + 3.4,
            this.getZ() + 1.0
        );
    }

    @Override
    protected Explosion createVariantExplosion()
    {
        if (!this.isFullHoney()) {
            return super.createVariantExplosion();
        }

        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator()
        {
            @Override
            public float getEntityDamageAmount(Explosion explosion, Entity entity)
            {
                return super.getEntityDamageAmount(explosion, entity) * HONEY_EXPLOSION_DAMAGE_FACTOR;
            }
        };
        return this.level().explode(
            this,
            null,
            calculator,
            this.getX(),
            this.getY(),
            this.getZ(),
            this.getVariantExplosionRadius(),
            false,
            Level.ExplosionInteraction.MOB
        );
    }

    @Override
    protected void afterVariantExplosion(Explosion explosion)
    {
        if (!this.isFullHoney() || this.level().isClientSide) {
            return;
        }

        // Replace the outer rim of the crater (blocks that were actually destroyed) with honey
        // blocks; the inner destroyed blocks stay air, like a normal creeper blast.
        Level level = this.level();
        Set<BlockPos> destroyed = new HashSet<>(explosion.getToBlow());
        for (BlockPos pos : explosion.getToBlow()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() || !isOuterRim(pos, destroyed)) {
                continue;
            }
            level.setBlock(pos, Blocks.HONEY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean isOuterRim(BlockPos pos, Set<BlockPos> destroyed)
    {
        return !destroyed.contains(pos.above())
            || !destroyed.contains(pos.below())
            || !destroyed.contains(pos.north())
            || !destroyed.contains(pos.south())
            || !destroyed.contains(pos.east())
            || !destroyed.contains(pos.west());
    }
}
