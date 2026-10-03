package io.github.mechtasnezhevna.creeper_rearranged.entity.wiskelper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Wiskelper - a nether-fortress creeper variant made of wither-skeleton bone.
 *
 * <p>It plays like a vanilla creeper but its blast is only 0.8 as wide, and every now and then it
 * stops to absorb soul power: for the duration of the absorb animation every creature within
 * {@value #ABSORB_RANGE} blocks - except wither skeletons, other wiskelpers and the wither itself,
 * the blacklist - is struck by a 5-second wither. It replaces 2% of the natural wither skeleton
 * spawns in nether fortresses (see {@code CreeperHooks#onFinalizeSpawn}).
 */
public class Wiskelper extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "wiskelper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/wiskelper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.wiskelper.idle";
    public static final String ANIMATION_MOVE = "animation.wiskelper.move";
    public static final String ANIMATION_ABSORB = "animation.wiskelper.absorb";

    private static final EntityDataAccessor<Boolean> DATA_ABSORBING =
        SynchedEntityData.defineId(Wiskelper.class, EntityDataSerializers.BOOLEAN);

    private static final int ANIMATION_TRANSITION_TICKS = 5;
    /** Blast radius: 0.8 x the vanilla creeper's 3.0. */
    private static final float EXPLOSION_RADIUS = 2.4F;
    /** Radius (blocks) of the soul absorption's wither aura. */
    private static final double ABSORB_RANGE = 8.0;
    /** Base time (ticks) between two absorption bursts. */
    private static final int ABSORB_INTERVAL = 240;
    /** Extra random ticks added to each absorption gap. */
    private static final int ABSORB_INTERVAL_SPREAD = 120;
    /** How long (ticks) one absorption lasts; matches the 3-second absorb animation. */
    private static final int ABSORB_DURATION = 60;
    /** How often (ticks) the wither aura is re-applied while absorbing. */
    private static final int ABSORB_REAPPLY_INTERVAL = 10;
    /** Wither duration (ticks) per application: 5 seconds. */
    private static final int WITHER_DURATION = 100;
    /**
     * Entity kinds the absorption leaves alone (the blacklist). Wiskelpers are always immune on
     * top of this list; {@code 凋苦灵} is checked separately at runtime because its registry holder
     * must not be resolved while the registries are still being populated.
     */
    private static final Set<EntityType<?>> ABSORB_BLACKLIST = Set.of(
        EntityType.WITHER_SKELETON,
        EntityType.WITHER
    );
    private static final String TAG_ABSORB_COOLDOWN = "WiskelperAbsorbCooldown";
    private static final String TAG_ABSORBING_TICKS = "WiskelperAbsorbingTicks";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private int absorbCooldown = -1;
    private int absorbingTicks;

    public Wiskelper(EntityType<? extends Wiskelper> entityType, Level level)
    {
        super(entityType, level);
    }

    @Override
    protected float getVariantExplosionRadius()
    {
        return EXPLOSION_RADIUS;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide) {
            this.tickAbsorption();
        }
    }

    /**
     * Drives the periodic soul absorption on the server. The first absorb waits a random 0-12
     * seconds, later bursts come every 12-18 seconds and last {@value #ABSORB_DURATION} ticks,
     * during which the wither aura is re-applied every {@value #ABSORB_REAPPLY_INTERVAL} ticks.
     */
    private void tickAbsorption()
    {
        if (this.absorbingTicks > 0) {
            this.absorbingTicks--;
            this.setAbsorbing(this.absorbingTicks > 0);
            if (this.absorbingTicks % ABSORB_REAPPLY_INTERVAL == 0) {
                this.applyWitherAura();
            }
            return;
        }
        if (this.absorbCooldown < 0) {
            this.absorbCooldown = this.random.nextInt(ABSORB_INTERVAL);
        }
        if (--this.absorbCooldown > 0) {
            return;
        }
        this.absorbingTicks = ABSORB_DURATION;
        this.absorbCooldown = ABSORB_INTERVAL + this.random.nextInt(ABSORB_INTERVAL_SPREAD);
        this.setAbsorbing(true);
        this.applyWitherAura();
    }

    /** Strikes every nearby creature that is not on the absorption blacklist with wither. */
    private void applyWitherAura()
    {
        this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(ABSORB_RANGE),
            entity -> !this.isImmuneToAbsorption(entity)
        ).forEach(entity -> entity.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION), this));
    }

    /** Whether the absorption leaves the given entity alone: wiskelpers, the blacklist and 凋苦灵. */
    private boolean isImmuneToAbsorption(Entity entity)
    {
        return entity.getType() == this.getType()
            || ABSORB_BLACKLIST.contains(entity.getType())
            || entity.getType() == ModEntities.WITHPER.get();
    }

    /** Whether the absorb animation should play; synced from the server. */
    public boolean isAbsorbing()
    {
        return this.entityData.get(DATA_ABSORBING);
    }

    private void setAbsorbing(boolean absorbing)
    {
        this.entityData.set(DATA_ABSORBING, absorbing);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_ABSORBING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_ABSORB_COOLDOWN, this.absorbCooldown);
        tag.putInt(TAG_ABSORBING_TICKS, this.absorbingTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.absorbCooldown = tag.getInt(TAG_ABSORB_COOLDOWN);
        this.absorbingTicks = tag.getInt(TAG_ABSORBING_TICKS);
        this.setAbsorbing(this.absorbingTicks > 0);
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
     * Plays the soul absorption once while absorbing - the soul block rises and spins, the TNT on
     * the back vanishes - and otherwise loops the walk or idle animation like a vanilla creeper.
     */
    private PlayState animationPredicate(AnimationState<Wiskelper> state)
    {
        if (this.isAbsorbing()) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(ANIMATION_ABSORB));
            return PlayState.CONTINUE;
        }
        String animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
