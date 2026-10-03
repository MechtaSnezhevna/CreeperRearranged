package io.github.mechtasnezhevna.creeper_rearranged.entity.withper;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.wiskelper.Wiskelper;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Withper (凋苦灵) - a wither-flavoured creeper boss.
 *
 * <p>It is summoned like the wither, except that the soul sand directly under the middle skull is
 * a {@code block_of_creeper_soul}. It flies, always hunts the nearest player and never fires
 * wither skulls: instead it drifts close and charges for 15 seconds, then blows itself up with a
 * blast twice as wide as a charged creeper's and enters phase two. In phase two only the middle
 * head remains, three wiskelpers are summoned, it moves faster and charges 30 seconds before a
 * blast three times as wide as a charged creeper's - which is also its last act: the second blast
* kills it, dropping 2 nether stars and 16 gunpowder. It has the wither's 300 HP, a purple
 * boss bar, the wither's invulnerability rules and a 4-second invulnerable wake: like the wither
 * it starts that wake at a third of its health and heals back to full while playing the summoning
 * animation. While charging, the boss bar switches to a notched overlay that shows charge progress.
 */
public class Withper extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "withper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/withper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.withper.idle";
    public static final String ANIMATION_MOVE = "animation.withper.move";
    public static final String ANIMATION_EXPLOSION = "animation.withper.explosion";
    public static final String ANIMATION_PHASE_TWO_IDLE = "animation.withper.sdidle";
    public static final String ANIMATION_PHASE_TWO_MOVE = "animation.withper.sdmove";
    public static final String ANIMATION_PHASE_TWO_EXPLOSION = "animation.withper.sdexplosion";
    public static final String ANIMATION_WAKE = "animation.withper.wake";

    private static final int ANIMATION_TRANSITION_TICKS = 5;
    /** The wake lasts 4 seconds, matching the non-looping {@link #ANIMATION_WAKE} animation. */
    private static final int WAKE_TICKS = 80;
    /** Boom at the end of the wake, mirroring the wither's summoning blast. */
    private static final float WAKE_EXPLOSION_RADIUS = 7.0F;
    /** Phase one charge: 15 seconds. */
    private static final int PHASE_ONE_CHARGE_TICKS = 300;
    /** Phase two charge: 30 seconds. */
    private static final int PHASE_TWO_CHARGE_TICKS = 600;
    /** Phase one blast: 2x a charged creeper's (radius 12). */
    private static final float PHASE_ONE_EXPLOSION_RADIUS = 12.0F;
    /** Phase two blast: 3x a charged creeper's (radius 18). */
    private static final float PHASE_TWO_EXPLOSION_RADIUS = 18.0F;
    /** The charge starts once the target is this close (Euclidean, in blocks). */
    private static final double CHARGE_RANGE = 6.0;
    private static final double CHARGE_RANGE_SQ = CHARGE_RANGE * CHARGE_RANGE;
    /** Wiskelpers summoned when phase two starts. */
    private static final int PHASE_TWO_SUMMONED_WISKELPERS = 3;
    /** Distance (blocks) at which the summoned wiskelpers appear around the boss. */
    private static final double SUMMON_RING_RADIUS = 2.5;
    /** Horizontal pull per tick; phase two is 1.5x as strong. */
    private static final double PHASE_ONE_DRIFT_STRENGTH = 0.3;
    private static final double PHASE_TWO_DRIFT_STRENGTH = 0.45;
    /** Vertical pull toward the target's height (the wither uses the same 0.3). */
    private static final double RISE_STRENGTH = 0.3;
    /** The boss hovers about this many blocks above its target's feet. */
    private static final double HOVER_HEIGHT_ABOVE_TARGET = 2.0;
    /** Horizontal pull switches off once this close (squared distance). */
    private static final double HORIZONTAL_DRIFT_MIN_DISTANCE_SQ = 9.0;
    /** Phase two moves 1.5x as fast in both the air and on the ground. */
    private static final double PHASE_TWO_SPEED_MULTIPLIER = 0.5;
    private static final ResourceLocation PHASE_TWO_SPEED_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "withper_phase_two_speed");

    private static final EntityDataAccessor<Integer> DATA_WAKE_TICKS =
        SynchedEntityData.defineId(Withper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS =
        SynchedEntityData.defineId(Withper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
        SynchedEntityData.defineId(Withper.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_WAKE_TICKS = "WithperWakeTicks";
    private static final String TAG_CHARGE_TICKS = "WithperChargeTicks";
    private static final String TAG_PHASE_TWO = "WithperPhaseTwo";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS
        )
        .setDarkenScreen(true);

    public Withper(EntityType<? extends Withper> entityType, Level level)
    {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.6F)
            .add(Attributes.FLYING_SPEED, 0.6F)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new WithperHoldGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // The nearest player always has the first claim on the boss's attention; retaliation
        // against other attackers only kicks in while no player is in reach.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, LivingEntity::attackable));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_WAKE_TICKS, 0);
        builder.define(DATA_CHARGE_TICKS, 0);
        builder.define(DATA_PHASE_TWO, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_WAKE_TICKS, this.getWakeTicks());
        tag.putInt(TAG_CHARGE_TICKS, this.getChargeTicks());
        tag.putBoolean(TAG_PHASE_TWO, this.isPhaseTwo());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setWakeTicks(tag.getInt(TAG_WAKE_TICKS));
        this.setChargeTicks(tag.getInt(TAG_CHARGE_TICKS));
        this.setPhaseTwo(tag.getBoolean(TAG_PHASE_TWO));
        if (this.isPhaseTwo()) {
            this.applyPhaseTwoSpeed();
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name)
    {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player)
    {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player)
    {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /** Called by the summoning structure: 4 seconds of invulnerable wake-up. */
    public void makeInvulnerable()
    {
        this.setWakeTicks(WAKE_TICKS);
        this.setHealth(this.getMaxHealth() / 3.0F);
        this.bossEvent.setProgress(0.0F);
    }

    /**
     * The wither-style chase drift: slowly pulled toward the target's XZ and rising to hover just
     * above its height. Phase two pulls harder. Nothing drifts while the boss wakes or charges.
     */
    @Override
    public void aiStep()
    {
        super.aiStep();
        if (this.level().isClientSide || this.isWaking() || this.isCharging()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 movement = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
        double y = movement.y;
        if (this.getY() < target.getY() + HOVER_HEIGHT_ABOVE_TARGET) {
            y = Math.max(0.0, y);
            y += RISE_STRENGTH - y * 0.6F;
        }
        movement = new Vec3(movement.x, y, movement.z);
        Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
        if (toTarget.horizontalDistanceSqr() > HORIZONTAL_DRIFT_MIN_DISTANCE_SQ) {
            Vec3 direction = toTarget.normalize();
            double strength = this.isPhaseTwo() ? PHASE_TWO_DRIFT_STRENGTH : PHASE_ONE_DRIFT_STRENGTH;
            movement = movement.add(direction.x * strength - movement.x * 0.6, 0.0, direction.z * strength - movement.z * 0.6);
        }
        this.setDeltaMovement(movement);
        if (movement.horizontalDistanceSqr() > 0.05) {
            this.setYRot((float) Mth.atan2(movement.z, movement.x) * (180.0F / (float) Math.PI) - 90.0F);
        }
    }

    /**
     * Drives the boss state machine on the server: the wake countdown and its summoning blast,
     * then the hunt - drift close, charge for 15 (phase one) or 30 (phase two) seconds, detonate.
     * A charge whose target flees beyond its 6-block range, breaks line of sight or dies burns
     * down at the same rate it built up, and the boss bar mirrors the wither: wake progress while
     * waking, health otherwise, and a notched charge progress while charging.
     */
    @Override
    protected void customServerAiStep()
    {
        if (this.isWaking()) {
            int wakeTicks = this.getWakeTicks() - 1;
            this.setWakeTicks(wakeTicks);
            this.bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            this.bossEvent.setProgress(1.0F - (float) wakeTicks / WAKE_TICKS);
            if (wakeTicks <= 0) {
                this.level().explode(this, this.getX(), this.getEyeY(), this.getZ(), WAKE_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.MOB);
                if (!this.isSilent()) {
                    this.level().globalLevelEvent(1023, this.blockPosition(), 0);
                }
            }
            if (this.tickCount % 4 == 0) {
                this.heal(10.0F);
            }
            return;
        }

        super.customServerAiStep();

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !target.attackable()) {
            if (this.isCharging()) {
                this.dissipateCharge();
            }
        } else if (this.isCharging()) {
            if (!this.hasLineOfSight(target) || this.distanceToSqr(target) > CHARGE_RANGE_SQ) {
                this.dissipateCharge();
            } else {
                int remaining = this.getChargeTicks() - 1;
                this.setChargeTicks(remaining);
                this.getNavigation().stop();
                if (remaining <= 0) {
                    this.detonate();
                    return;
                }
            }
        } else if (this.distanceToSqr(target) <= CHARGE_RANGE_SQ && this.hasLineOfSight(target)) {
            this.startCharging();
        }

        this.updateBossBar();
    }

    /** The boss bar shows charge progress (notched overlay) while charging, health otherwise. */
    private void updateBossBar()
    {
        if (this.isCharging()) {
            int maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
            this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
            this.bossEvent.setProgress(1.0F - (float) this.getChargeTicks() / (float) maxTicks);
        } else {
            this.bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    private void startCharging()
    {
        this.setChargeTicks(this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS);
    }

    /** A charge whose target fled, hid or died burns down at the same rate it built up. */
    private void dissipateCharge()
    {
        int maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
        this.setChargeTicks(Math.min(this.getChargeTicks() + 1, maxTicks));
    }

    /** The charge ran out: blow up, then either enter phase two or - after the second blast - die. */
    private void detonate()
    {
        boolean phaseTwo = this.isPhaseTwo();
        float radius = phaseTwo ? PHASE_TWO_EXPLOSION_RADIUS : PHASE_ONE_EXPLOSION_RADIUS;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.MOB);
        this.setChargeTicks(0);
        if (phaseTwo) {
            this.scriptedDeath();
        } else {
            this.enterPhaseTwo();
        }
    }

    private void enterPhaseTwo()
    {
        this.setPhaseTwo(true);
        this.applyPhaseTwoSpeed();
        this.summonWiskelpers();
    }

    private void applyPhaseTwoSpeed()
    {
        if (this.level().isClientSide
            || this.getAttribute(Attributes.FLYING_SPEED).getModifier(PHASE_TWO_SPEED_MODIFIER_ID) != null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(
            PHASE_TWO_SPEED_MODIFIER_ID,
            PHASE_TWO_SPEED_MULTIPLIER,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.getAttribute(Attributes.FLYING_SPEED).addTransientModifier(modifier);
        this.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
    }

    /** Summons three persistent wiskelpers around the boss, sharing its current target. */
    private void summonWiskelpers()
    {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = this.getTarget();
        for (int i = 0; i < PHASE_TWO_SUMMONED_WISKELPERS; i++) {
            double angle = i * 2.0 * Math.PI / PHASE_TWO_SUMMONED_WISKELPERS + this.random.nextDouble() * 0.4;
            Wiskelper wiskelper = new Wiskelper(ModEntities.WISKELPER.get(), serverLevel);
            wiskelper.moveTo(
                this.getX() + Math.cos(angle) * SUMMON_RING_RADIUS,
                this.getY(),
                this.getZ() + Math.sin(angle) * SUMMON_RING_RADIUS,
                this.getYRot(),
                0.0F
            );
            wiskelper.setPersistenceRequired();
            if (target != null) {
                wiskelper.setTarget(target);
            }
            serverLevel.addFreshEntity(wiskelper);
        }
    }

    /**
     * The second blast is the boss's last act: it drops its loot (2 nether stars and 16 gunpowder
     * through the loot table), releases the boss bar and discards, exactly like a creeper that
     * blew itself up.
     */
    private void scriptedDeath()
    {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.dead = true;
            this.dropAllDeathLoot(serverLevel, this.damageSources().generic());
            this.dropExperience(this.getKillCredit());
        }
        this.bossEvent.removeAllPlayers();
        this.discard();
    }

    @Override
    protected float getVariantExplosionRadius()
    {
        return this.isPhaseTwo() ? PHASE_TWO_EXPLOSION_RADIUS : PHASE_ONE_EXPLOSION_RADIUS;
    }

    /**
     * Swelling grows 0 to 1 across the whole charge, driving the creeper-style inflation and the
     * white fuse flash on the renderer.
     */
    @Override
    public float getSwelling(float partialTick)
    {
        if (this.isCharging()) {
            int maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
            return Mth.clamp((float) (maxTicks - this.getChargeTicks() + partialTick) / (float) maxTicks, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    /** The boss only detonates through its own charge machine; flint and steel must not short-circuit it. */
    @Override
    public void ignite()
    {
    }

    @Override
    public void setSwellDir(int direction)
    {
        if (direction > 0) {
            return;
        }
        super.setSwellDir(direction);
    }

    /** Mirrors the wither: immune to its damage tag, to the wither's own attacks and while waking. */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (source.is(DamageTypeTags.WITHER_IMMUNE_TO) || source.getEntity() instanceof WitherBoss) {
            return false;
        } else if (this.isWaking() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void checkDespawn()
    {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source)
    {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect)
    {
        return effect.is(MobEffects.WITHER) ? false : super.canBeAffected(effect);
    }

    @Override
    protected boolean canRide(Entity vehicle)
    {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowSpawn)
    {
        return false;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier)
    {
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.WITHER_DEATH;
    }

    public boolean isWaking()
    {
        return this.getWakeTicks() > 0;
    }

    public boolean isCharging()
    {
        return this.getChargeTicks() > 0;
    }

    public boolean isPhaseTwo()
    {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    public int getWakeTicks()
    {
        return this.entityData.get(DATA_WAKE_TICKS);
    }

    public int getChargeTicks()
    {
        return this.entityData.get(DATA_CHARGE_TICKS);
    }

    private void setWakeTicks(int ticks)
    {
        this.entityData.set(DATA_WAKE_TICKS, ticks);
    }

    private void setChargeTicks(int ticks)
    {
        this.entityData.set(DATA_CHARGE_TICKS, Math.max(ticks, 0));
    }

    private void setPhaseTwo(boolean phaseTwo)
    {
        this.entityData.set(DATA_PHASE_TWO, phaseTwo);
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
     * Plays the one-shot wake while summoning, the explosion pose while charging, and otherwise
     * loops the idle/move pair - the phase-two variants (sd*) only show the middle head.
     */
    private PlayState animationPredicate(AnimationState<Withper> state)
    {
        if (this.isWaking()) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(ANIMATION_WAKE));
            return PlayState.CONTINUE;
        }
        if (this.isCharging()) {
            state.getController().setAnimation(
                RawAnimation.begin().thenLoop(this.isPhaseTwo() ? ANIMATION_PHASE_TWO_EXPLOSION : ANIMATION_EXPLOSION)
            );
            return PlayState.CONTINUE;
        }
        if (this.isPhaseTwo()) {
            state.getController().setAnimation(
                RawAnimation.begin().thenLoop(state.isMoving() ? ANIMATION_PHASE_TWO_MOVE : ANIMATION_PHASE_TWO_IDLE)
            );
        } else {
            state.getController().setAnimation(
                RawAnimation.begin().thenLoop(state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE)
            );
        }
        return PlayState.CONTINUE;
    }

    /** Freezes the boss in place - no movement, jumping or looking - while it wakes or charges. */
    class WithperHoldGoal extends Goal
    {
        WithperHoldGoal()
        {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            return Withper.this.isWaking() || Withper.this.isCharging();
        }
    }
}
