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
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    /** HP at which the phase-two transition triggers (half of 300). */
    private static final float PHASE_TWO_TRIGGER_HEALTH = 150.0F;
    /** The transition charge before the phase-two blast: a creeper's fuse length (1.5 s). */
    private static final int TRANSITION_CHARGE_TICKS = 30;
    /** The transition blast matches a charged creeper's (radius 6). */
    private static final float TRANSITION_EXPLOSION_RADIUS = 6.0F;
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
    /** Vertical spring per block of height difference toward the target's height. */
    private static final double HEIGHT_TRACK_STRENGTH = 0.1;
    /** Phase two tracks height 1.5x as fast. */
    private static final double PHASE_TWO_HEIGHT_TRACK_STRENGTH = 0.15;
    /** Vertical speed cap (blocks/tick); the wither rises at about the same rate. */
    private static final double MAX_VERTICAL_SPEED = 0.5;
    /** Phase two can climb and dive 1.5x as fast. */
    private static final double PHASE_TWO_MAX_VERTICAL_SPEED = 0.75;
    /** Vertical velocity decay per tick: settles the boss at the target's height without overshoot. */
    private static final double HEIGHT_TRACK_DAMPING = 0.6;
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
    /** Countdown of the short un-interruptible charge before the phase-two transition blast. */
    private static final EntityDataAccessor<Integer> DATA_TRANSITION_TICKS =
        SynchedEntityData.defineId(Withper.class, EntityDataSerializers.INT);
    /** True while a charge burns back down after the target left the blast range: the boss keeps
     *  moving while the charge bar drains, instead of being frozen in the charging state. */
    private static final EntityDataAccessor<Boolean> DATA_DISSIPATING =
        SynchedEntityData.defineId(Withper.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_WAKE_TICKS = "WithperWakeTicks";
    private static final String TAG_CHARGE_TICKS = "WithperChargeTicks";
    private static final String TAG_PHASE_TWO = "WithperPhaseTwo";
    private static final String TAG_TRANSITION_TICKS = "WithperTransitionTicks";
    private static final String TAG_DISSIPATING = "WithperDissipating";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS
        )
        .setDarkenScreen(true);
    /**
     * The charge bar: a separate white, title-less, notched bar that only appears while charging.
     * It is shown for normal charges (building and draining back down) and for the short
     * phase-transition charge, and is kept stacked directly beneath the health bar (see
     * {@link #updateBossBar()}).
     */
    private final ServerBossEvent chargeEvent = new ServerBossEvent(
            Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10
        );
    /** Whether the charge bar is currently shown; gates the one-time bar reordering at charge start. */
    private boolean chargeBarVisible;

    public Withper(EntityType<? extends Withper> entityType, Level level)
    {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 50;
        this.chargeEvent.setVisible(false);
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
        builder.define(DATA_TRANSITION_TICKS, 0);
        builder.define(DATA_DISSIPATING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_WAKE_TICKS, this.getWakeTicks());
        tag.putInt(TAG_CHARGE_TICKS, this.getChargeTicks());
        tag.putBoolean(TAG_PHASE_TWO, this.isPhaseTwo());
        tag.putInt(TAG_TRANSITION_TICKS, this.getTransitionTicks());
        tag.putBoolean(TAG_DISSIPATING, this.isDissipating());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setWakeTicks(tag.getInt(TAG_WAKE_TICKS));
        this.setChargeTicks(tag.getInt(TAG_CHARGE_TICKS));
        this.setPhaseTwo(tag.getBoolean(TAG_PHASE_TWO));
        this.setTransitionTicks(tag.getInt(TAG_TRANSITION_TICKS));
        this.setDissipating(tag.getBoolean(TAG_DISSIPATING));
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
        this.chargeEvent.addPlayer(player);
        // A player who joins mid-charge gets both bars in the right order (health, then charge),
        // so no reordering is needed for them.
        if (this.isCharging() || this.isDissipating() || this.isTransitioning()) {
            this.chargeBarVisible = true;
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player)
    {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
        this.chargeEvent.removePlayer(player);
    }

    /**
     * While the boss wakes it must not hunt: any target the target goals pick up during the wake
     * is dropped again, so it neither chases nor charges the moment the wake ends - it re-acquires
     * a target like a freshly spawned boss.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && this.isWaking()) {
            return;
        }
        super.setTarget(target);
    }

    /** Called by the summoning structure: 4 seconds of invulnerable wake-up. */
    public void makeInvulnerable()
    {
        this.setWakeTicks(WAKE_TICKS);
        this.setTarget(null);
        this.setHealth(this.getMaxHealth() / 3.0F);
        this.bossEvent.setProgress(0.0F);
    }

    /**
     * The chase drift: slowly pulled toward the target's XZ and toward its altitude through a
     * damped spring - it approaches the target and settles at its height instead of hovering
     * above or bobbing up and down, while still following through low openings. Phase two pulls
     * harder. Nothing drifts while the boss wakes, charges or transitions.
     */
    @Override
    public void aiStep()
    {
        super.aiStep();
        if (this.level().isClientSide || this.isWaking() || this.isCharging() || this.isTransitioning()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 movement = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
        double y = movement.y;
        double heightStrength = this.isPhaseTwo() ? PHASE_TWO_HEIGHT_TRACK_STRENGTH : HEIGHT_TRACK_STRENGTH;
        double heightCap = this.isPhaseTwo() ? PHASE_TWO_MAX_VERTICAL_SPEED : MAX_VERTICAL_SPEED;
        double heightDifference = target.getY() - this.getY();
        y = (y + heightDifference * heightStrength) * HEIGHT_TRACK_DAMPING;
        y = Mth.clamp(y, -heightCap, heightCap);
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
     * Reaching 150 HP interrupts any charge and starts the phase-two transition: a short
     * un-interruptible 30-tick charge (a creeper's fuse length) while the boss is locked and
     * shielded, then a charged-creeper blast, single-headed phase two and its wiskelpers. A charge
     * whose target flees beyond its 6-block range, breaks line of sight or dies burns down at the
     * same rate it built up, and while it drains the boss is not frozen - it keeps moving and the
     * charge bar empties naturally. The bars follow the wither: the main bar shows wake progress
     * while waking and health otherwise, and the separate white charge bar appears only while
     * charging.
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

        if (this.isTransitioning()) {
            // Un-interruptible: it ignores its target and its own invulnerability lock absorbs any
            // hit, so the 30-tick charge always finishes and the transition blast always goes off.
            int remaining = this.getTransitionTicks() - 1;
            this.setTransitionTicks(remaining);
            this.getNavigation().stop();
            if (remaining <= 0) {
                this.finishPhaseTransition();
            }
            this.updateBossBar();
            return;
        }

        super.customServerAiStep();

        if (!this.isPhaseTwo() && this.getHealth() <= PHASE_TWO_TRIGGER_HEALTH) {
            this.startPhaseTransition();
            this.updateBossBar();
            return;
        }

        LivingEntity target = this.getTarget();
        boolean targetInRange = target != null
            && target.isAlive()
            && target.attackable()
            && this.distanceToSqr(target) <= CHARGE_RANGE_SQ
            && this.hasLineOfSight(target);
        if (this.isCharging() || this.isDissipating()) {
            if (targetInRange) {
                // The target is back in blast range: resume building the charge from wherever it
                // drained to.
                this.setDissipating(false);
                int remaining = this.getChargeTicks() - 1;
                this.setChargeTicks(remaining);
                this.getNavigation().stop();
                if (remaining <= 0) {
                    this.detonate();
                    return;
                }
            } else {
                // No reachable target while a charge is up: burn the charge back down. The boss is
                // in the normal (dissipating) state, so it keeps moving while the charge bar drains.
                this.dissipateCharge();
            }
        } else if (targetInRange) {
            this.startCharging();
        }

        this.updateBossBar();
    }

    /**
     * The main purple bar always shows health; the white charge bar appears only while charging
     * (building, draining back down or in the phase transition). When the charge bar is about to
     * show, the health bar is removed and re-added first: the client orders boss bars by ADD
     * arrival, so re-adding the health bar moves it to the end of the list and the charge bar
     * lands directly beneath it instead of at the bottom of every boss bar the player has.
     */
    private void updateBossBar()
    {
        this.bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        boolean charging = this.isCharging() || this.isDissipating() || this.isTransitioning();
        if (charging && !this.chargeBarVisible) {
            this.bossEvent.setVisible(false);
            this.bossEvent.setVisible(true);
        }
        this.chargeBarVisible = charging;
        this.chargeEvent.setVisible(charging);
        if (charging) {
            int maxTicks;
            int ticks;
            if (this.isTransitioning()) {
                maxTicks = TRANSITION_CHARGE_TICKS;
                ticks = this.getTransitionTicks();
            } else {
                maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
                ticks = this.getChargeTicks();
            }
            this.chargeEvent.setProgress(1.0F - (float) ticks / (float) maxTicks);
        }
    }

    /**
     * Kicks off the phase-two transition the moment the boss reaches 150 HP: the ongoing charge
     * (if any) is cleared, health locks at 150 (hits are capped on the way in and fully absorbed
     * while transitioning, so the bar only ever falls smoothly to 150) and the short
     * un-interruptible transition charge starts.
     */
    private void startPhaseTransition()
    {
        this.setHealth(PHASE_TWO_TRIGGER_HEALTH);
        this.setChargeTicks(0);
        this.setTransitionTicks(TRANSITION_CHARGE_TICKS);
        this.setDissipating(false);
    }

    /** The transition charge ran out: a charged-creeper blast, then unlock, single head and wiskelpers. */
    private void finishPhaseTransition()
    {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), TRANSITION_EXPLOSION_RADIUS, Level.ExplosionInteraction.MOB);
        this.setTransitionTicks(0);
        this.enterPhaseTwo();
    }

    private void startCharging()
    {
        this.setDissipating(false);
        this.setChargeTicks(this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS);
    }

    /**
     * A charge whose target fled, hid or died burns down at the same rate it built up, in the
     * dissipating state: the boss is not frozen - it moves and hunts normally while the charge
     * bar drains. Reaching the top of the charge means it is fully gone: the boss stops draining
     * (charge ticks snap back to 0) and resumes hunting, so an interrupted charge never freezes
     * its AI.
     */
    private void dissipateCharge()
    {
        int maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
        int next = this.getChargeTicks() + 1;
        if (next >= maxTicks) {
            this.setChargeTicks(0);
            this.setDissipating(false);
        } else {
            this.setChargeTicks(next);
            this.setDissipating(true);
        }
    }

    /** The charge ran out: blow up, then either enter phase two or - after the second blast - die. */
    private void detonate()
    {
        boolean phaseTwo = this.isPhaseTwo();
        float radius = phaseTwo ? PHASE_TWO_EXPLOSION_RADIUS : PHASE_ONE_EXPLOSION_RADIUS;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.MOB);
        this.setChargeTicks(0);
        this.setDissipating(false);
        if (phaseTwo) {
            this.scriptedDeath();
        } else {
            // The self-detonation beat the player to the punch (health still above 150): the full
            // phase-one blast goes off, then the boss drops to the 150-HP line and turns
            // single-headed with its wiskelpers.
            this.setHealth(PHASE_TWO_TRIGGER_HEALTH);
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
     * The second blast is the boss's last act: it releases its boss bars and discards, exactly
     * like a creeper that blew itself up. It drops nothing - loot only comes from actually killing
     * it, so letting it self-detonate is never a reward.
     */
    private void scriptedDeath()
    {
        this.dead = true;
        this.bossEvent.removeAllPlayers();
        this.chargeEvent.removeAllPlayers();
        this.discard();
    }

    @Override
    protected float getVariantExplosionRadius()
    {
        return this.isPhaseTwo() ? PHASE_TWO_EXPLOSION_RADIUS : PHASE_ONE_EXPLOSION_RADIUS;
    }

    /**
     * Swelling grows 0 to 1 across the whole charge, driving the creeper-style inflation and the
     * white fuse flash on the renderer - and shrinks back to 0 while the charge dissipates, so the
     * boss deflates naturally as the charge bar drains.
     */
    @Override
    public float getSwelling(float partialTick)
    {
        if (this.isCharging() || this.isDissipating()) {
            int maxTicks = this.isPhaseTwo() ? PHASE_TWO_CHARGE_TICKS : PHASE_ONE_CHARGE_TICKS;
            return Mth.clamp((float) (maxTicks - this.getChargeTicks() + partialTick) / (float) maxTicks, 0.0F, 1.0F);
        }
        if (this.isTransitioning()) {
            return Mth.clamp(
                (float) (TRANSITION_CHARGE_TICKS - this.getTransitionTicks() + partialTick) / (float) TRANSITION_CHARGE_TICKS,
                0.0F,
                1.0F
            );
        }
        return 0.0F;
    }

    /** The boss only detonates through its own charge machine; flint and steel must not short-circuit it. */
    @Override
    public void ignite()
    {
    }

    /**
     * The boss can't be lit with a creeper igniter (flint and steel or fire charge), so those
     * interactions do nothing at all - no spark sound, no swing, no durability loss - instead of
     * the vanilla creeper behaviour of spending 1 durability on a no-op ignite.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (player.getItemInHand(hand).is(ItemTags.CREEPER_IGNITERS)) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setSwellDir(int direction)
    {
        if (direction > 0) {
            return;
        }
        super.setSwellDir(direction);
    }

    /**
     * Mirrors the wither: immune to its damage tag and the wither's own attacks, while waking, and
     * while the phase transition locks it (the wither-style shield makes it deflect projectiles
     * too). In phase one the boss can neither die to nor be dropped below the 150-HP line by a
     * normal hit: a killing blow is capped so it always survives, and whatever remains below the
     * line is snapped back to exactly 150 in the same tick. The intermediate value is never sent
     * to the client, so the health bar only ever falls smoothly to 150 and never jumps back up -
     * and overkill can't skip the transition or its blast.
     */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (source.is(DamageTypeTags.WITHER_IMMUNE_TO) || source.getEntity() instanceof WitherBoss) {
            return false;
        } else if ((this.isWaking() || this.isTransitioning()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else if (!this.isPhaseTwo() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (amount >= this.getHealth()) {
                amount = Math.max(this.getHealth() - 1.0F, 0.0F);
            }
            boolean hurt = super.hurt(source, amount);
            if (hurt && !this.isDeadOrDying() && this.getHealth() < PHASE_TWO_TRIGGER_HEALTH) {
                this.setHealth(PHASE_TWO_TRIGGER_HEALTH);
            }
            return hurt;
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

    /** Wake progress 0 to 1, used by the renderer to fade in the summoning shield. */
    public float getWakeProgress()
    {
        return 1.0F - (float) this.getWakeTicks() / (float) WAKE_TICKS;
    }

    /**
     * Whether the boss is frozen in the building charge. A charge that is burning back down after
     * the target left the blast range is {@link #isDissipating()} instead: the boss keeps moving
     * while the charge bar drains.
     */
    public boolean isCharging()
    {
        return this.getChargeTicks() > 0 && !this.isDissipating();
    }

    /** Whether a charge is burning back down after the target left the blast range. */
    public boolean isDissipating()
    {
        return this.entityData.get(DATA_DISSIPATING);
    }

    /** Whether the short un-interruptible charge before the phase-two blast is running. */
    public boolean isTransitioning()
    {
        return this.getTransitionTicks() > 0;
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

    public int getTransitionTicks()
    {
        return this.entityData.get(DATA_TRANSITION_TICKS);
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

    private void setTransitionTicks(int ticks)
    {
        this.entityData.set(DATA_TRANSITION_TICKS, Math.max(ticks, 0));
    }

    private void setDissipating(boolean dissipating)
    {
        this.entityData.set(DATA_DISSIPATING, dissipating);
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
        if (this.isCharging() || this.isTransitioning()) {
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
            return Withper.this.isWaking() || Withper.this.isCharging() || Withper.this.isTransitioning();
        }
    }
}
