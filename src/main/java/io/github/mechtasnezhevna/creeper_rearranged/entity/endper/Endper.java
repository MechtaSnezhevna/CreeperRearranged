package io.github.mechtasnezhevna.creeper_rearranged.entity.endper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
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
 * Endper - an enderman-tainted creeper variant carrying an ender pearl.
 *
 * <p>It never hunts on its own: while calm it just wanders (and shuns rain and water). Staring at it
 * (same rules as an enderman: carved pumpkin hides you, sneaking/invisibility and worn armour shrink
 * the provoking distance) or hurting it makes it scream, growl and sprint at the target, detonating
 * 0.5 seconds after it closes in. Its blast is 1.5x a vanilla creeper's. It does not teleport when
 * attacked, but below 3 hearts it has a 1/4 chance per setback to escape with an enderman-style
 * teleport, which spends its pearl (the model's pearl bone is hidden afterwards and it stops
 * dropping one). Water and rain never hurt it.
 */
public class Endper extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "endper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/endper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.endper.idle";
    public static final String ANIMATION_WALK = "animation.endper.walk";
    public static final String ANIMATION_ANGRY = "animation.endper.nangry";
    public static final String ANIMATION_RUN = "animation.endper.run";

    /** Total health: 24 HP = 12 hearts. */
    public static final double MAX_HEALTH = 24.0D;
    /** Below this (3 hearts) the endper may spend its pearl to flee. */
    public static final float FLEE_HEALTH_THRESHOLD = 6.0F;

    private static final EntityDataAccessor<Boolean> DATA_ANGRY =
        SynchedEntityData.defineId(Endper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STARED_AT =
        SynchedEntityData.defineId(Endper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_PEARL =
        SynchedEntityData.defineId(Endper.class, EntityDataSerializers.BOOLEAN);

    /** Vanilla enderman angry speed bonus (attribute +0.15, ADD_VALUE). */
    private static final ResourceLocation SPEED_MODIFIER_ATTACKING_ID =
        ResourceLocation.withDefaultNamespace("attacking");
    private static final AttributeModifier SPEED_MODIFIER_ATTACKING =
        new AttributeModifier(SPEED_MODIFIER_ATTACKING_ID, 0.15F, AttributeModifier.Operation.ADD_VALUE);

    private static final int STARE_SOUND_COOLDOWN = 400;
    private static final int ANIMATION_TRANSITION_TICKS = 5;
    /** Per-damage / periodic chance to teleport away at low health. */
    private static final float FLEE_CHANCE = 0.25F;
    private static final int FLEE_CHECK_INTERVAL = 20;
    /** Ambient weather/water avoidance scan cadence. */
    private static final int SHELTER_SCAN_INTERVAL = 40;
    /**
     * swell advances by this many ticks-worth per tick, so the 30-tick vanilla fuse completes in
     * 10 ticks (0.5 s): the plan-approved "0.5 秒内完成自爆".
     */
    private static final int SWELL_DIR_CHARGE = 3;
    private static final String TAG_HAS_PEARL = "EndperHasPearl";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private int lastStareSound = Integer.MIN_VALUE;
    private int fleeCheckCooldown;

    public Endper(EntityType<? extends Endper> entityType, Level level)
    {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.3F)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new EndperChargeGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new EndperLookForPlayerGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    /** True while chasing a target; drives the angry animations and the scream ambience. */
    public boolean isAngry()
    {
        return this.entityData.get(DATA_ANGRY);
    }

    /** Whether the endper still carries the pearl it can spend to escape. */
    public boolean hasPearl()
    {
        return this.entityData.get(DATA_HAS_PEARL);
    }

    public void setHasPearl(boolean hasPearl)
    {
        this.entityData.set(DATA_HAS_PEARL, hasPearl);
    }

    public boolean hasBeenStaredAt()
    {
        return this.entityData.get(DATA_STARED_AT);
    }

    public void setBeingStaredAt()
    {
        this.entityData.set(DATA_STARED_AT, true);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target instanceof Goat) {
            return;
        }
        boolean wasAngry = this.isAngry();
        boolean wasStaredAt = this.hasBeenStaredAt();
        super.setTarget(target);

        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (target == null) {
            this.entityData.set(DATA_ANGRY, false);
            this.entityData.set(DATA_STARED_AT, false);
            if (movementSpeed != null) {
                movementSpeed.removeModifier(SPEED_MODIFIER_ATTACKING_ID);
            }
        } else {
            this.entityData.set(DATA_ANGRY, true);
            if (movementSpeed != null && !movementSpeed.hasModifier(SPEED_MODIFIER_ATTACKING_ID)) {
                movementSpeed.addTransientModifier(SPEED_MODIFIER_ATTACKING);
            }
            // Becoming angry without having been stared at means it was hurt: scream once, loudly.
            if (!wasAngry && !wasStaredAt && !this.level().isClientSide) {
                this.level().playSound(
                    null, this.getX(), this.getEyeY(), this.getZ(),
                    SoundEvents.ENDERMAN_SCREAM, this.getSoundSource(), 2.5F, 1.0F
                );
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGRY, false);
        builder.define(DATA_STARED_AT, false);
        builder.define(DATA_HAS_PEARL, true);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor)
    {
        // Vanilla enderman plays the staring "creepy" sound on the client once the angry state
        // arrives while the player has been staring at it.
        if (DATA_ANGRY.equals(accessor) && this.hasBeenStaredAt() && this.level().isClientSide) {
            this.playStareSound();
        }
        super.onSyncedDataUpdated(accessor);
    }

    /** Cooldown-gated enderman stare sound, broadcast locally on the client like vanilla. */
    public void playStareSound()
    {
        if (this.tickCount >= this.lastStareSound + STARE_SOUND_COOLDOWN) {
            this.lastStareSound = this.tickCount;
            if (!this.isSilent()) {
                this.level().playLocalSound(
                    this.getX(), this.getEyeY(), this.getZ(),
                    SoundEvents.ENDERMAN_STARE, this.getSoundSource(), 2.5F, 1.0F, false
                );
            }
        }
    }

    /**
     * Enderman gaze test, identical to vanilla: a player in the narrow 0.025-rad cone aimed at the
     * endper's eye provokes it, unless a carved pumpkin hides the player's face. Distance shrinkage
     * for sneaking/invisibility/armour is applied by the target goal's {@link TargetingConditions}.
     */
    boolean isLookingAtMe(Player player)
    {
        ItemStack helmet = player.getInventory().armor.get(3);
        if (helmet.is(Blocks.CARVED_PUMPKIN.asItem())) {
            return false;
        }
        Vec3 lookDirection = player.getViewVector(1.0F).normalize();
        Vec3 toEndper = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double distance = toEndper.length();
        toEndper = toEndper.normalize();
        double dot = lookDirection.dot(toEndper);
        return dot > 1.0 - 0.025 / distance ? player.hasLineOfSight(this) : false;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        // While chasing below 3 hearts the endper keeps rolling its 1/4 escape chance.
        if (this.isAngry() && this.getHealth() < FLEE_HEALTH_THRESHOLD && this.hasPearl()) {
            if (--this.fleeCheckCooldown <= 0) {
                this.fleeCheckCooldown = FLEE_CHECK_INTERVAL;
                if (this.random.nextFloat() < FLEE_CHANCE) {
                    this.tryFlee();
                }
            }
        }

        // Calm endpers try to leave water and get out of the rain.
        if (!this.isAngry() && this.tickCount % SHELTER_SCAN_INTERVAL == 0) {
            this.seekShelter();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        boolean result = super.hurt(source, amount);
        if (!this.level().isClientSide && this.isAlive() && this.getHealth() < FLEE_HEALTH_THRESHOLD
            && this.hasPearl() && this.random.nextFloat() < FLEE_CHANCE) {
            this.tryFlee();
        }
        return result;
    }

    /** Blast is 1.5x a vanilla creeper (4.5). */
    @Override
    protected float getVariantExplosionRadius()
    {
        return 4.5F;
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return this.isAngry() ? SoundEvents.ENDERMAN_SCREAM : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_HAS_PEARL, this.hasPearl());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        // Newly spawned endpers always carry a pearl; missing NBT means it has not been spent.
        this.setHasPearl(!tag.contains(TAG_HAS_PEARL) || tag.getBoolean(TAG_HAS_PEARL));
    }

    /** Spends the pearl on a successful enderman-style teleport, drops the current target. */
    private void tryFlee()
    {
        if (this.level().isClientSide || !this.isAlive() || !this.hasPearl()) {
            return;
        }
        if (this.enderTeleport()) {
            this.setHasPearl(false);
            this.setTarget(null);
        }
    }

    /** Copy of vanilla {@code EnderMan.teleport()}: random point within 32 blocks. */
    private boolean enderTeleport()
    {
        if (!this.level().isClientSide() && this.isAlive()) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
            double y = this.getY() + (double) (this.random.nextInt(64) - 32);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
            return this.teleport(x, y, z);
        }
        return false;
    }

    /** Copy of vanilla {@code EnderMan.teleport(double, double, double)} incl. sounds. */
    private boolean teleport(double x, double y, double z)
    {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(pos).blocksMotion()) {
            pos.move(Direction.DOWN);
        }
        BlockState blockState = this.level().getBlockState(pos);
        boolean canStand = blockState.blocksMotion();
        boolean landingInWater = blockState.getFluidState().is(FluidTags.WATER);
        if (canStand && !landingInWater) {
            Vec3 oldPos = this.position();
            boolean success = this.randomTeleport(x, y, z, true);
            if (success) {
                this.level().gameEvent(GameEvent.TELEPORT, oldPos, GameEvent.Context.of(this));
                if (!this.isSilent()) {
                    this.level().playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }
            return success;
        }
        return false;
    }

    /** Walks a calm, wet endper toward a dry / sheltered spot. */
    private void seekShelter()
    {
        boolean inWater = this.isInWater();
        boolean exposedToRain = !inWater && this.level().isRaining() && this.level().canSeeSky(this.blockPosition());
        if (!inWater && !exposedToRain) {
            return;
        }
        BlockPos target = this.findShelterSpot(inWater);
        if (target != null) {
            this.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }
    }

    @Nullable
    private BlockPos findShelterSpot(boolean fromWater)
    {
        BlockPos origin = this.blockPosition();
        Level level = this.level();
        for (int attempt = 0; attempt < 24; attempt++) {
            int baseY = origin.getY() + this.random.nextInt(7) - 2;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                origin.getX() + this.random.nextInt(25) - 12,
                baseY,
                origin.getZ() + this.random.nextInt(25) - 12
            );
            for (int dy = 0; dy < 6; dy++) {
                pos.setY(baseY + dy);
                if (pos.getY() < level.getMinBuildHeight() + 1 || pos.getY() > level.getMaxBuildHeight()) {
                    continue;
                }
                BlockState below = level.getBlockState(pos.below());
                if (!level.getBlockState(pos).isAir()
                    || !below.blocksMotion()
                    || below.getFluidState().is(FluidTags.WATER)
                    || level.getFluidState(pos).is(FluidTags.WATER)
                    || level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                    continue;
                }
                if (fromWater || !level.canSeeSky(pos)) {
                    return pos.immutable();
                }
            }
        }
        return null;
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
     * Calm endpers idle or walk; angry ones freeze into the snarling pose ({@code nangry}) or sprint
     * ({@code run}). The pearl is hidden on the client via the renderer, not with a dedicated
     * animation, so no animation branch is needed for the spent-pearl state.
     */
    private PlayState animationPredicate(AnimationState<Endper> state)
    {
        boolean angry = this.isAngry();
        String animation = angry
            ? (state.isMoving() ? ANIMATION_RUN : ANIMATION_ANGRY)
            : (state.isMoving() ? ANIMATION_WALK : ANIMATION_IDLE);
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }

    /**
     * Primes the fuse only while angrily closing in on a visible target. Once ignited, the swell
     * advances 3 ticks per tick so the 30-tick vanilla fuse burns out in 0.5 s; walking away more
     * than 7 blocks or losing sight defuses it again.
     */
    private static class EndperChargeGoal extends Goal
    {
        private final Endper endper;

        EndperChargeGoal(Endper endper)
        {
            this.endper = endper;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            LivingEntity target = this.endper.getTarget();
            return this.endper.getSwellDir() > 0
                || target != null && this.endper.distanceToSqr(target) < 9.0;
        }

        @Override
        public void start()
        {
            this.endper.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick()
        {
            return true;
        }

        @Override
        public void tick()
        {
            LivingEntity target = this.endper.getTarget();
            if (target == null || !this.endper.isAngry()) {
                this.endper.setSwellDir(-1);
            } else if (this.endper.distanceToSqr(target) > 49.0) {
                this.endper.setSwellDir(-1);
            } else if (!this.endper.getSensing().hasLineOfSight(target)) {
                this.endper.setSwellDir(-1);
            } else {
                this.endper.setSwellDir(SWELL_DIR_CHARGE);
            }
        }
    }

    /**
     * Port of the vanilla enderman stare goal (minus its teleport tricks). A player must hold its
     * gaze for 5 ticks for the endper to lock on; afterwards the endper keeps the target while it
     * stays within its follow range, exactly like a vanice enderman that stopped being stared at.
     */
    private static class EndperLookForPlayerGoal extends NearestAttackableTargetGoal<Player>
    {
        private final Endper endper;
        @Nullable
        private Player pendingTarget;
        private int aggroTime;
        private final TargetingConditions startAggroTargetConditions;
        private final java.util.function.Predicate<LivingEntity> isAngerInducing;

        EndperLookForPlayerGoal(Endper endper)
        {
            super(endper, Player.class, 10, false, false, null);
            this.endper = endper;
            this.isAngerInducing = entity -> entity instanceof Player player && endper.isLookingAtMe(player)
                && !endper.hasIndirectPassenger(entity);
            this.startAggroTargetConditions = TargetingConditions.forCombat()
                .range(this.getFollowDistance())
                .selector(this.isAngerInducing);
        }

        @Override
        public boolean canUse()
        {
            this.pendingTarget = this.endper.level().getNearestPlayer(this.startAggroTargetConditions, this.endper);
            return this.pendingTarget != null;
        }

        @Override
        public void start()
        {
            this.aggroTime = this.adjustedTickDelay(5);
            this.endper.setBeingStaredAt();
        }

        @Override
        public void stop()
        {
            this.pendingTarget = null;
            super.stop();
        }

        @Override
        public boolean canContinueToUse()
        {
            if (this.pendingTarget != null) {
                if (!this.isAngerInducing.test(this.pendingTarget)) {
                    return false;
                }
                this.endper.lookAt(this.pendingTarget, 10.0F, 10.0F);
                return true;
            }
            if (this.target != null && this.endper.hasIndirectPassenger(this.target)) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void tick()
        {
            if (this.endper.getTarget() == null) {
                this.setTarget(null);
            }
            if (this.pendingTarget != null) {
                if (--this.aggroTime <= 0) {
                    this.target = this.pendingTarget;
                    this.pendingTarget = null;
                    super.start();
                }
            } else {
                super.tick();
            }
        }
    }
}
