package io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
 * <p>Pollen-carrying bees are attracted to the nest on its head; when one comes close it loses its
 * pollen and the nest gains one honey level. At {@value #MAX_HONEY_LEVEL} levels the hive is full:
 * its explosion deals half damage, scatters honey blocks through the blast crater, and slows
 * affected creatures. Honey level is tracked in NBT under {@code HoneyLevel}.
 */
public class Honeeper extends VariantCreeper implements GeoEntity
{
    private static final EntityDataAccessor<Integer> DATA_HONEY_LEVEL =
        SynchedEntityData.defineId(Honeeper.class, EntityDataSerializers.INT);

    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "honeeper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/honeeper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.honeeper.idle";
    public static final String ANIMATION_MOVE = "animation.honeeper.move";
    public static final String ANIMATION_IDLE_FULL_HONEY = "animation.honeeper.idleh";
    public static final String ANIMATION_MOVE_FULL_HONEY = "animation.honeeper.moveh";

    /** Highest honey level of the worn nest; reaching it means the hive is full. */
    public static final int MAX_HONEY_LEVEL = 3;
    /** Slowness applied to explosion victims of a full-honey honeeper (Slowness I for 5 seconds). */
    public static final int SLOWNESS_DURATION_TICKS = 5 * 20;
    public static final int SLOWNESS_AMPLIFIER = 0;

    private static final int BEE_SCAN_INTERVAL = 10;
    /** How far (blocks) pollen-carrying bees are lured toward the honeeper's head. */
    private static final double ATTRACT_RADIUS = 4.0;
    private static final float HONEY_EXPLOSION_DAMAGE_FACTOR = 0.5F;
    /** Per-destroyed-block chance that a full-honey explosion leaves honey in its place. */
    private static final float HONEY_BLOCK_REPLACE_CHANCE = 0.5F;
    private static final String TAG_HONEY_LEVEL = "HoneyLevel";
    private static final int ANIMATION_TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    /**
     * Positions holding real (non-air) blocks that this honeeper's blast is about to destroy,
     * recorded on the server during {@code ExplosionEvent.Detonate} before those blocks are broken.
     * {@code Explosion.getToBlow()} also lists pure-air cells, so only this set may turn into honey.
     */
    private Set<BlockPos> destroyedBlockPositions;

    public Honeeper(EntityType<? extends Honeeper> entityType, Level level)
    {
        super(entityType, level);
    }

    /** Current honey level of the worn nest, 0..{@value #MAX_HONEY_LEVEL}. */
    public int getHoneyLevel()
    {
        return this.entityData.get(DATA_HONEY_LEVEL);
    }

    /** Sets the honey level, clamped to the valid range; synced data drives the full-honey state. */
    public void setHoneyLevel(int honeyLevel)
    {
        this.entityData.set(DATA_HONEY_LEVEL, Math.max(0, Math.min(MAX_HONEY_LEVEL, honeyLevel)));
    }

    public boolean isFullHoney()
    {
        return this.getHoneyLevel() >= MAX_HONEY_LEVEL;
    }

    /** Records which positions the imminent blast will really destroy (see {@link #destroyedBlockPositions}). */
    public void recordDestroyedBlocks(Collection<BlockPos> positions)
    {
        this.destroyedBlockPositions = new HashSet<>(positions);
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
        builder.define(DATA_HONEY_LEVEL, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_HONEY_LEVEL, this.getHoneyLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setHoneyLevel(tag.getInt(TAG_HONEY_LEVEL));
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide || !this.isAlive() || this.isFullHoney()) {
            return;
        }
        if (this.tickCount % BEE_SCAN_INTERVAL != 0) {
            return;
        }

        // A pollen-carrying bee that reaches the nest mouth drops its pollen off and fills the nest
        // one level. At most one delivery per scan so refilling takes several bee visits.
        Bee nectarBee = null;
        for (Bee bee : this.level().getEntitiesOfClass(Bee.class, this.overheadBox())) {
            if (bee.isAlive() && bee.hasNectar()) {
                nectarBee = bee;
                break;
            }
        }
        if (nectarBee != null) {
            nectarBee.dropOffNectar();
            this.setHoneyLevel(this.getHoneyLevel() + 1);
            if (this.isFullHoney()) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.BEEHIVE_DRIP, SoundSource.NEUTRAL, 1.0F, 1.0F);
                return;
            }
        }

        // Attract remaining pollen-carrying bees nearby: lure them toward the nest opening.
        if (!this.isFullHoney()) {
            for (Bee bee : this.level().getEntitiesOfClass(Bee.class, this.attractBox())) {
                if (bee.isAlive() && bee.hasNectar()) {
                    bee.getNavigation().moveTo(this.getX(), this.getY() + 2.2, this.getZ(), 1.0);
                }
            }
        }
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

    private AABB attractBox()
    {
        return new AABB(
            this.getX() - ATTRACT_RADIUS,
            this.getY() - 1.0,
            this.getZ() - ATTRACT_RADIUS,
            this.getX() + ATTRACT_RADIUS,
            this.getY() + 4.0,
            this.getZ() + ATTRACT_RADIUS
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

        // Only blocks the blast actually destroyed (recorded at Detonate time from originally
        // non-air cells, and still air now) independently have a 50% chance to become honey.
        // Pure-air cells are skipped, and mobGriefing=false/KEEP explosions fail the isAir() check.
        if (this.destroyedBlockPositions == null) {
            return;
        }
        Level level = this.level();
        for (BlockPos pos : this.destroyedBlockPositions) {
            if (!level.getBlockState(pos).isAir() || level.random.nextFloat() >= HONEY_BLOCK_REPLACE_CHANCE) {
                continue;
            }
            level.setBlock(pos, Blocks.HONEY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
        this.destroyedBlockPositions = null;
    }
}
