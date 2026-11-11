package io.github.mechtasnezhevna.creeper_rearranged.entity.sculkper;

import com.mojang.logging.LogUtils;
import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Sculkper - a deep-dark creeper variant grown out of sculk.
 *
 * <p>It is a passive listener rather than an attacker: it never seeks out players and never swells
 * on its own, and every action it takes - including its explosion - is silent to vibration
 * listeners, and wardens never treat it as a target. It listens for vibrations within
 * {@value #VIBRATION_RANGE} blocks like a warden, stares at the source of a living creature's
 * vibration for {@value #LOOK_AT_SOURCE_TICKS} ticks, and after {@value #VIBRATIONS_TO_SHRIEK}
 * vibrations it shrieks for {@value #SHRIEK_TICKS} ticks and summons a warden within
 * {@value #WARDEN_SUMMON_RANGE} blocks (twice with a 0.2% chance). It only detonates when it is
 * attacked: it locks onto its attacker, walks up to it and
 * charges for {@value #CHARGE_TICKS} ticks before a power-4 blast that damages entities but never
 * destroys blocks. When it has been left alone for {@value #REST_IDLE_THRESHOLD} ticks it may sit
 * down for {@value #REST_DURATION} ticks and regenerate.
 */
public class Sculkper extends VariantCreeper implements GeoEntity, VibrationSystem
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "sculkper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/sculkper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.sculkper.idle";
    public static final String ANIMATION_MOVE = "animation.sculkper.move";
    public static final String ANIMATION_SENSING = "animation.sculkper.sensing";
    public static final String ANIMATION_SHRIEKING = "animation.sculkper.shrieking";
    public static final String ANIMATION_SIT = "animation.sculkper.sit";

    private static final int ANIMATION_TRANSITION_TICKS = 5;

    /** Blast radius: power 4, the same as a block of TNT. */
    private static final float EXPLOSION_RADIUS = 4.0F;
    /** Fuse length in ticks (5 seconds) once the sculkper starts charging. */
    public static final int CHARGE_TICKS = 100;
    /** Horizontal radius (blocks) at which the sculkper starts charging towards its attacker. */
    public static final double SWELL_RANGE_SQUARED = 9.0;
    /** Distance (squared) beyond which the fuse aborts, mirroring a vanilla creeper. */
    private static final double SWELL_ABORT_RANGE_SQUARED = 49.0;

    /** Vibration listener radius, matching the warden's 16 blocks. */
    private static final int VIBRATION_RANGE = 16;
    /** How long (ticks) the same vibration burst is coalesced into one signal. */
    private static final int VIBRATION_COOLDOWN_TICKS = 40;
    /** Vibrations required before the sculkper shrieks. */
    private static final int VIBRATIONS_TO_SHRIEK = 3;
    /** Shriek duration in ticks (5 seconds), like a sculk shrieker's. */
    private static final int SHRIEK_TICKS = 100;
    /** Seconds the sculkper keeps staring at a detected vibration's source. */
    private static final int LOOK_AT_SOURCE_TICKS = 300;
    /** Radius (blocks) in which a warden is summoned around the shrieking sculkper. */
    private static final int WARDEN_SUMMON_RANGE = 24;
    /** Chance (per shriek) that two wardens are summoned at once: 0.2%. */
    private static final float DOUBLE_WARDEN_CHANCE = 0.002F;

    /** Idle time (5 minutes) before the sculkper may sit down and rest. */
    private static final int REST_IDLE_THRESHOLD = 6000;
    /** Rest duration in ticks (1 minute). */
    private static final int REST_DURATION = 1200;
    /** Percent chance the sculkper actually rests once it has been idle long enough. */
    private static final int REST_CHANCE_PERCENT = 55;
    /** Regeneration I duration in ticks (15 seconds) granted by resting. */
    private static final int REST_REGENERATION_TICKS = 300;

    private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS =
        SynchedEntityData.defineId(Sculkper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RESTING =
        SynchedEntityData.defineId(Sculkper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHRIEKING =
        SynchedEntityData.defineId(Sculkper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SENSING =
        SynchedEntityData.defineId(Sculkper.class, EntityDataSerializers.BOOLEAN);

    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
    private final VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;

    /** Server-only: ticks left on the vibration burst cooldown. */
    private int vibrationCooldown;
    /** Server-only: vibrations detected since the last shriek. */
    private int vibrationCount;
    /** Server-only: ticks left in the current shriek. */
    private int shriekTicks;
    /** Server-only: ticks the sculkper has been left alone. */
    private int idleTicks;
    /** Server-only: ticks left in the current rest. */
    private int restTicks;
    /** Server-only: ticks left of staring at the last vibration's source. */
    private int lookingTicks;
    @Nullable
    private LivingEntity lookingAt;
    /**
     * Server-only: whether the current fuse was lit with flint and steel. Like a vanilla creeper's
     * ignited fuse it always burns to the end, while a chase fuse aborts when the target escapes.
     */
    private boolean ignitedCharge;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public Sculkper(EntityType<? extends Sculkper> entityType, Level level)
    {
        super(entityType, level);
        this.vibrationUser = new SculkperVibrationUser();
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }

    /** Max health 50 (25 hearts); otherwise the vanilla creeper profile. */
    public static AttributeSupplier.Builder createAttributes()
    {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH, 50.0);
    }

    /**
     * A sculkper never seeks out players and never swells because one is nearby: the only target it
     * can acquire comes from being hurt, so the charge goal below keys off that. Everything else
     * mirrors a vanilla creeper's goal set (it still avoids cats and ocelots, for example).
     */
    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new SculkperRestGoal());
        this.goalSelector.addGoal(1, new SculkperHoldGoal());
        this.goalSelector.addGoal(2, new FloatGoal(this));
        this.goalSelector.addGoal(3, new SculkperSwellGoal(this));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE_TICKS, 0);
        builder.define(DATA_RESTING, false);
        builder.define(DATA_SHRIEKING, false);
        builder.define(DATA_SENSING, false);
    }

    @Override
    public void tick()
    {
        if (this.level() instanceof ServerLevel serverLevel) {
            VibrationSystem.Ticker.tick(serverLevel, this.vibrationData, this.vibrationUser);
            if (this.vibrationCooldown > 0) {
                this.vibrationCooldown--;
            }
        }
        super.tick();
    }

    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();
        if (this.isShrieking()) {
            this.tickShriek();
        } else {
            this.tickCharge();
            this.tickRest();
            this.tickLooking();
        }
    }

    // ---------- Explosion ----------

    /** Power 4, like TNT, but the blast never destroys blocks. */
    @Override
    protected float getVariantExplosionRadius()
    {
        return EXPLOSION_RADIUS;
    }

    @Override
    protected Explosion createVariantExplosion()
    {
        return this.level()
            .explode(this, this.getX(), this.getY(), this.getZ(), this.getVariantExplosionRadius(), Level.ExplosionInteraction.NONE);
    }

    /**
     * The charge progress drives the renderer's swelling inflate and white fuse flash, like a
     * vanilla creeper's own swelling curve - just stretched over the full 5-second fuse.
     */
    @Override
    public float getSwelling(float partialTick)
    {
        if (this.isCharging()) {
            return Mth.clamp((float) (CHARGE_TICKS - this.getChargeTicks() + partialTick) / (float) CHARGE_TICKS, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    /**
     * Flint and steel still primes a sculkper, but through its own 5-second charge. Like a vanilla
     * creeper's ignited fuse, a lit fuse always burns to the end even when no target is nearby.
     */
    @Override
    public void ignite()
    {
        if (!this.level().isClientSide && !this.isShrieking()) {
            this.ignitedCharge = true;
            this.startCharging();
        }
    }

    public boolean isCharging()
    {
        return this.getChargeTicks() > 0;
    }

    private int getChargeTicks()
    {
        return this.entityData.get(DATA_CHARGE_TICKS);
    }

    private void setChargeTicks(int ticks)
    {
        this.entityData.set(DATA_CHARGE_TICKS, ticks);
    }

    /** Starts the 5-second fuse; called by the swell goal once its target is close enough. */
    void startCharging()
    {
        if (this.isCharging() || this.isShrieking()) {
            return;
        }
        this.setChargeTicks(CHARGE_TICKS);
        this.setResting(false);
        this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
    }

    /**
     * Counts the fuse down. A fuse lit with flint and steel burns unconditionally - like a vanilla
     * creeper's ignited fuse - while a chase fuse aborts, like a vanilla creeper whose victim walks
     * out of range, as soon as the locked target is no longer close enough.
     */
    private void tickCharge()
    {
        if (!this.isCharging()) {
            return;
        }
        this.getNavigation().stop();
        LivingEntity target = this.getTarget();
        if (this.ignitedCharge
            || (target != null
                && target.isAlive()
                && this.distanceToSqr(target) <= SWELL_ABORT_RANGE_SQUARED
                && this.getSensing().hasLineOfSight(target))) {
            if (target != null) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            this.setChargeTicks(this.getChargeTicks() - 1);
            if (this.getChargeTicks() <= 0) {
                this.triggerVariantExplosion();
            }
        } else {
            this.setChargeTicks(0);
            this.ignitedCharge = false;
        }
    }

    // ---------- Resting ----------

    public boolean isResting()
    {
        return this.entityData.get(DATA_RESTING);
    }

    private void setResting(boolean resting)
    {
        this.entityData.set(DATA_RESTING, resting);
    }

    private void startResting()
    {
        this.setResting(true);
        this.restTicks = REST_DURATION;
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REST_REGENERATION_TICKS, 0));
    }

    private void tickRest()
    {
        if (this.isResting()) {
            this.getNavigation().stop();
            this.restTicks--;
            if (this.restTicks <= 0 || this.getTarget() != null || this.isShrieking()) {
                this.setResting(false);
                this.idleTicks = 0;
            }
            return;
        }
        if (this.getTarget() == null && !this.isCharging() && !this.isShrieking()) {
            this.idleTicks++;
            if (this.idleTicks >= REST_IDLE_THRESHOLD) {
                this.idleTicks = 0;
                if (this.random.nextInt(100) < REST_CHANCE_PERCENT) {
                    this.startResting();
                }
            }
        } else {
            this.idleTicks = 0;
        }
    }

    /** Freezes the sculkper in place - no movement, jumping or looking away - while it rests. */
    private final class SculkperRestGoal extends Goal
    {
        private SculkperRestGoal()
        {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            return Sculkper.this.isResting();
        }

        @Override
        public void tick()
        {
            Sculkper.this.getNavigation().stop();
        }
    }

    // ---------- Shrieking and warden summoning ----------

    public boolean isShrieking()
    {
        return this.entityData.get(DATA_SHRIEKING);
    }

    private void setShrieking(boolean shrieking)
    {
        this.entityData.set(DATA_SHRIEKING, shrieking);
    }

    private void startShrieking()
    {
        this.setShrieking(true);
        this.shriekTicks = SHRIEK_TICKS;
        this.setChargeTicks(0);
        this.ignitedCharge = false;
        this.setResting(false);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0F, 1.0F);
    }

    /** Shrieks for 5 seconds and summons the warden(s) as the shriek dies out. */
    private void tickShriek()
    {
        this.getNavigation().stop();
        this.shriekTicks--;
        if (this.shriekTicks <= 0) {
            this.setShrieking(false);
            this.summonWardens();
        }
    }

    /** Summons one warden within 24 blocks - or two, with a 0.2% chance. */
    private void summonWardens()
    {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = serverLevel.random.nextFloat() < DOUBLE_WARDEN_CHANCE ? 2 : 1;
        for (int i = 0; i < count; i++) {
            SpawnUtil.trySpawnMob(
                EntityType.WARDEN,
                MobSpawnType.TRIGGERED,
                serverLevel,
                this.blockPosition(),
                20,
                WARDEN_SUMMON_RANGE,
                6,
                SpawnUtil.Strategy.ON_TOP_OF_COLLIDER
            );
        }
    }

    /** Freezes the sculkper in place while it charges or shrieks. */
    private final class SculkperHoldGoal extends Goal
    {
        private SculkperHoldGoal()
        {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            return Sculkper.this.isCharging() || Sculkper.this.isShrieking();
        }
    }

    // ---------- Vibration sensing ----------

    @Override
    public VibrationSystem.Data getVibrationData()
    {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser()
    {
        return this.vibrationUser;
    }

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> listenerConsumer)
    {
        if (this.level() instanceof ServerLevel serverLevel) {
            listenerConsumer.accept(this.dynamicGameEventListener, serverLevel);
        }
    }

    public boolean isSensing()
    {
        return this.entityData.get(DATA_SENSING);
    }

    private void setSensing(boolean sensing)
    {
        this.entityData.set(DATA_SENSING, sensing);
    }

    /** Stares at the source of a detected vibration for 15 seconds. */
    private void startLookingAt(LivingEntity entity)
    {
        this.lookingAt = entity;
        this.lookingTicks = LOOK_AT_SOURCE_TICKS;
        this.setSensing(true);
    }

    private void tickLooking()
    {
        if (this.lookingTicks <= 0) {
            this.lookingAt = null;
            if (this.isSensing()) {
                this.setSensing(false);
            }
            return;
        }
        this.lookingTicks--;
        if (this.lookingAt != null && this.lookingAt.isAlive()) {
            this.getLookControl().setLookAt(this.lookingAt, 30.0F, 30.0F);
        } else {
            this.lookingAt = null;
            this.lookingTicks = 0;
            if (this.isSensing()) {
                this.setSensing(false);
            }
        }
    }

    private void onVibrationReceived(@Nullable Entity entity, @Nullable Entity projectileOwner)
    {
        this.vibrationCooldown = VIBRATION_COOLDOWN_TICKS;
        LivingEntity source = null;
        if (projectileOwner instanceof LivingEntity living) {
            source = living;
        } else if (entity instanceof LivingEntity living) {
            source = living;
        }
        if (source != null) {
            this.startLookingAt(source);
        }
        this.vibrationCount++;
        if (this.vibrationCount >= VIBRATIONS_TO_SHRIEK) {
            this.vibrationCount = 0;
            this.startShrieking();
        }
    }

    /**
     * A sculkper's own actions - walking, being hurt, priming its fuse, exploding - never produce
     * vibrations. Both of the warden's tricks are combined: the entity never posts game events of
     * its own, and any leftover event is filtered out by {@link #dampensVibrations()}.
     */
    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, @Nullable Entity entity)
    {
    }

    @Override
    public boolean dampensVibrations()
    {
        return true;
    }

    /**
     * Warden-style vibration user: listens within 16 blocks, coalesces bursts with a short cooldown,
     * and hands each received signal to the sculkper.
     */
    private final class SculkperVibrationUser implements VibrationSystem.User
    {
        private final PositionSource positionSource = new EntityPositionSource(Sculkper.this, Sculkper.this.getEyeHeight());

        @Override
        public int getListenerRadius()
        {
            return VIBRATION_RANGE;
        }

        @Override
        public PositionSource getPositionSource()
        {
            return this.positionSource;
        }

        @Override
        public TagKey<GameEvent> getListenableEvents()
        {
            return GameEventTags.WARDEN_CAN_LISTEN;
        }

        @Override
        public boolean canTriggerAvoidVibration()
        {
            return true;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, GameEvent.Context context)
        {
            return !Sculkper.this.isNoAi()
                && !Sculkper.this.isDeadOrDying()
                && Sculkper.this.vibrationCooldown <= 0
                && level.getWorldBorder().isWithinBounds(pos);
        }

        @Override
        public void onReceiveVibration(
            ServerLevel level,
            BlockPos pos,
            Holder<GameEvent> gameEvent,
            @Nullable Entity entity,
            @Nullable Entity projectileOwner,
            float distance
        )
        {
            if (!Sculkper.this.isDeadOrDying()) {
                Sculkper.this.onVibrationReceived(entity, projectileOwner);
            }
        }
    }

    // ---------- Persistence ----------

    @Override
    public void addAdditionalSaveData(CompoundTag compound)
    {
        super.addAdditionalSaveData(compound);
        compound.putInt("ChargeTicks", this.getChargeTicks());
        compound.putBoolean("Resting", this.isResting());
        compound.putBoolean("Shrieking", this.isShrieking());
        compound.putBoolean("Sensing", this.isSensing());
        compound.putInt("RestTicks", this.restTicks);
        compound.putInt("IdleTicks", this.idleTicks);
        compound.putInt("ShriekTicks", this.shriekTicks);
        compound.putInt("VibrationCount", this.vibrationCount);
        compound.putInt("VibrationCooldown", this.vibrationCooldown);
        compound.putBoolean("IgnitedCharge", this.ignitedCharge);
        RegistryOps<Tag> registryOps = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        VibrationSystem.Data.CODEC
            .encodeStart(registryOps, this.vibrationData)
            .resultOrPartial(error -> LOGGER.error("Failed to encode vibration listener for Sculkper: '{}'", error))
            .ifPresent(tag -> compound.put("listener", tag));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound)
    {
        super.readAdditionalSaveData(compound);
        this.setChargeTicks(compound.getInt("ChargeTicks"));
        this.setResting(compound.getBoolean("Resting"));
        this.setShrieking(compound.getBoolean("Shrieking"));
        this.setSensing(compound.getBoolean("Sensing"));
        this.restTicks = compound.getInt("RestTicks");
        this.idleTicks = compound.getInt("IdleTicks");
        this.shriekTicks = compound.getInt("ShriekTicks");
        this.vibrationCount = compound.getInt("VibrationCount");
        this.vibrationCooldown = compound.getInt("VibrationCooldown");
        this.ignitedCharge = compound.getBoolean("IgnitedCharge");
        if (compound.contains("listener", 10)) {
            RegistryOps<Tag> registryOps = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            VibrationSystem.Data.CODEC
                .parse(registryOps, compound.getCompound("listener"))
                .resultOrPartial(error -> LOGGER.error("Failed to parse vibration listener for Sculkper: '{}'", error))
                .ifPresent(data -> this.vibrationData = data);
        }
    }

    // ---------- Animation ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, ANIMATION_CONTROLLER, ANIMATION_TRANSITION_TICKS, this::animationPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.animatableCache;
    }

    /**
     * Shriek and sit are one-shot visual states, then the sculkper looks around while sensing and
     * otherwise walks or idles like every other creeper variant. The 5-second fuse is drawn by the
     * renderer's swelling inflate on top of whichever of these animations is playing.
     */
    private PlayState animationPredicate(AnimationState<Sculkper> state)
    {
        if (this.isShrieking()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SHRIEKING));
            return PlayState.CONTINUE;
        }
        if (this.isResting()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SIT));
            return PlayState.CONTINUE;
        }
        if (this.isSensing()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SENSING));
            return PlayState.CONTINUE;
        }
        String animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
