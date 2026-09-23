package io.github.mechtasnezhevna.creeper_rearranged.entity.phanper;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * Phanper - a phantom that carries a block of TNT and dives at players like its undead cousin,
 * except that a successful dive ends in a creeper-style self-destruction instead of a bite.
 *
 * <p>The vanilla phantom's flight internals (move control, circling and swoop goals, dive state)
 * are all package-private, so they are copied here and adjusted: a dive commits to the player's
 * position at the moment it starts, and the phanper explodes only if it catches up to the player;
 * a player who dodges away makes the dive miss, after which the phanper simply climbs back into
 * the sky for another try. Unlike phantoms it never burns in daylight and during the day it only
 * hunts players who attacked it first. Drops are gunpowder only - no phantom membrane (see the
 * loot table). A naturally spawned phantom has a 1/3 chance to be replaced by a phanper, and each
 * night has a 1/13 chance to summon one above a random player (see {@code CreeperHooks}).
 */
public class Phanper extends FlyingMob implements Enemy, GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "phanper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/phanper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.phanper.idle";

    /** Copy of the vanilla phantom's flap constants, driving the flap sound and particles. */
    public static final float FLAP_DEGREES_PER_TICK = 7.448451F;
    public static final int TICKS_PER_FLAP = Mth.ceil(24.166098F);

    /** Blast radius of the dive explosion, matching a vanilla creeper. */
    private static final float EXPLOSION_RADIUS = 3.0F;
    /** A dive that does not connect within this many ticks counts as missed. */
    private static final int MAX_SWEEP_TICKS = 100;
    private static final int ANIMATION_TRANSITION_TICKS = 5;
    private static final String TAG_PROVOKED = "Provoked";

    private static final EntityDataAccessor<Integer> ID_SIZE =
        SynchedEntityData.defineId(Phanper.class, EntityDataSerializers.INT);

    Vec3 moveTargetPoint = Vec3.ZERO;
    BlockPos anchorPoint = BlockPos.ZERO;
    AttackPhase attackPhase = AttackPhase.CIRCLE;

    /** Position the running dive commits to; a player who leaves it makes the dive miss. */
    @Nullable
    private Vec3 diveTarget;
    private int sweepTicks;
    /** True once a player hurt the phanper, which lets it hunt during the day. */
    private boolean provokedByPlayer;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public Phanper(EntityType<? extends Phanper> entityType, Level level)
    {
        super(entityType, level);
        this.xpReward = 5;
        this.moveControl = new PhanperMoveControl(this);
        this.lookControl = new PhanperLookControl(this);
    }

    /** The vanilla phantom runs on the plain monster attribute set. */
    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes();
    }

    @Override
    public boolean isFlapping()
    {
        return (this.getUniqueFlapTickOffset() + this.tickCount) % TICKS_PER_FLAP == 0;
    }

    @Override
    protected BodyRotationControl createBodyControl()
    {
        return new PhanperBodyRotationControl(this);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new PhanperAttackStrategyGoal());
        this.goalSelector.addGoal(2, new PhanperSweepAttackGoal());
        this.goalSelector.addGoal(3, new PhanperCircleAroundAnchorGoal());
        this.targetSelector.addGoal(1, new PhanperLookForPlayerGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(ID_SIZE, 0);
    }

    public void setPhantomSize(int size)
    {
        this.entityData.set(ID_SIZE, Mth.clamp(size, 0, 64));
    }

    private void updatePhantomSizeInfo()
    {
        this.refreshDimensions();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6 + this.getPhantomSize());
    }

    public int getPhantomSize()
    {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        if (ID_SIZE.equals(key)) {
            this.updatePhantomSizeInfo();
        }
        super.onSyncedDataUpdated(key);
    }

    public int getUniqueFlapTickOffset()
    {
        return this.getId() * 3;
    }

    @Override
    protected boolean shouldDespawnInPeaceful()
    {
        return true;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide) {
            float f = Mth.cos(
                (float) (this.getUniqueFlapTickOffset() + this.tickCount) * FLAP_DEGREES_PER_TICK * (float) (Math.PI / 180.0) + (float) Math.PI
            );
            float f1 = Mth.cos(
                (float) (this.getUniqueFlapTickOffset() + this.tickCount + 1) * FLAP_DEGREES_PER_TICK * (float) (Math.PI / 180.0) + (float) Math.PI
            );
            if (f > 0.0F && f1 <= 0.0F) {
                this.level().playLocalSound(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.PHANTOM_FLAP,
                    this.getSoundSource(),
                    0.95F + this.random.nextFloat() * 0.05F,
                    0.95F + this.random.nextFloat() * 0.05F,
                    false
                );
            }

            float f2 = this.getBbWidth() * 1.48F;
            float f3 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f2;
            float f4 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * f2;
            float f5 = (0.3F + f * 0.45F) * this.getBbHeight() * 2.5F;
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() + f3, this.getY() + f5, this.getZ() + f4, 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() - f3, this.getY() + f5, this.getZ() - f4, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
    ) {
        this.anchorPoint = this.blockPosition().above(5);
        this.setPhantomSize(0);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AX")) {
            this.anchorPoint = new BlockPos(tag.getInt("AX"), tag.getInt("AY"), tag.getInt("AZ"));
        }
        this.setPhantomSize(tag.getInt("Size"));
        this.provokedByPlayer = tag.getBoolean(TAG_PROVOKED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("AX", this.anchorPoint.getX());
        tag.putInt("AY", this.anchorPoint.getY());
        tag.putInt("AZ", this.anchorPoint.getZ());
        tag.putInt("Size", this.getPhantomSize());
        tag.putBoolean(TAG_PROVOKED, this.provokedByPlayer);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance)
    {
        return true;
    }

    @Override
    public SoundSource getSoundSource()
    {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource)
    {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected float getSoundVolume()
    {
        return 1.0F;
    }

    @Override
    public boolean canAttackType(EntityType<?> entityType)
    {
        return true;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose)
    {
        int size = this.getPhantomSize();
        return super.getDefaultDimensions(pose).scale(1.0F + 0.15F * size);
    }

    /** Phanpers fly through daylight unharmed. */
    @Override
    protected boolean isSunBurnTick()
    {
        return false;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount)
    {
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            this.provokedByPlayer = true;
        }
        super.actuallyHurt(source, amount);
    }

    /**
     * The dive connected: mark dead, detonate like a creeper (respecting the mobGriefing rule) and
     * discard without drops - exactly like a creeper that blew itself up.
     */
    private void explodePhanper()
    {
        if (this.level().isClientSide) {
            return;
        }
        this.dead = true;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_RADIUS, Level.ExplosionInteraction.MOB);
        this.triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
        this.discard();
    }

    /** Whether a player attacked the phanper at some point, the only thing that lets it hunt by day. */
    private boolean isProvoked()
    {
        return this.provokedByPlayer;
    }

    // ------------------------------------------------------------- animation

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

    private PlayState animationPredicate(AnimationState<Phanper> state)
    {
        state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_IDLE));
        return PlayState.CONTINUE;
    }

    private enum AttackPhase
    {
        CIRCLE,
        SWOOP;
    }

    /**
     * Port of the vanilla phantom player-targeting goal with one extra rule: while the level is in
     * daylight the phanper only hunts a player after that player attacked it first. Once provoked it
     * stays provoked (the flag survives saving), so night and day behaviour only differ in who it
     * will pick as a target.
     */
    class PhanperLookForPlayerGoal extends Goal
    {
        private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);
        private int nextScanTick = reducedTickDelay(20);

        @Override
        public boolean canUse()
        {
            if (Phanper.this.level().isDay() && !Phanper.this.isProvoked()) {
                return false;
            }
            if (this.nextScanTick > 0) {
                this.nextScanTick--;
                return false;
            }
            this.nextScanTick = reducedTickDelay(60);
            List<Player> players = Phanper.this.level()
                .getNearbyPlayers(this.attackTargeting, Phanper.this, Phanper.this.getBoundingBox().inflate(16.0, 64.0, 16.0));
            if (!players.isEmpty()) {
                players.sort(Comparator.<Player, Double>comparing(Entity::getY).reversed());
                for (Player player : players) {
                    if (Phanper.this.canAttack(player, TargetingConditions.DEFAULT)) {
                        Phanper.this.setTarget(player);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse()
        {
            if (Phanper.this.level().isDay() && !Phanper.this.isProvoked()) {
                return false;
            }
            LivingEntity target = Phanper.this.getTarget();
            return target != null ? Phanper.this.canAttack(target, TargetingConditions.DEFAULT) : false;
        }
    }

    /** Port of the vanilla phantom's strategy goal, deciding when a circle turns into a swoop. */
    class PhanperAttackStrategyGoal extends Goal
    {
        private int nextSweepTick;

        @Override
        public boolean canUse()
        {
            LivingEntity target = Phanper.this.getTarget();
            return target != null ? Phanper.this.canAttack(target, TargetingConditions.DEFAULT) : false;
        }

        @Override
        public void start()
        {
            this.nextSweepTick = this.adjustedTickDelay(10);
            Phanper.this.attackPhase = AttackPhase.CIRCLE;
            this.setAnchorAboveTarget();
        }

        @Override
        public void stop()
        {
            Phanper.this.anchorPoint = Phanper.this.level()
                .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Phanper.this.anchorPoint)
                .above(10 + Phanper.this.random.nextInt(20));
        }

        @Override
        public void tick()
        {
            if (Phanper.this.attackPhase == AttackPhase.CIRCLE) {
                this.nextSweepTick--;
                if (this.nextSweepTick <= 0) {
                    Phanper.this.attackPhase = AttackPhase.SWOOP;
                    this.setAnchorAboveTarget();
                    this.nextSweepTick = this.adjustedTickDelay((8 + Phanper.this.random.nextInt(4)) * 20);
                    Phanper.this.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + Phanper.this.random.nextFloat() * 0.1F);
                }
            }
        }

        private void setAnchorAboveTarget()
        {
            Phanper.this.anchorPoint = Phanper.this.getTarget().blockPosition().above(20 + Phanper.this.random.nextInt(20));
            if (Phanper.this.anchorPoint.getY() < Phanper.this.level().getSeaLevel()) {
                Phanper.this.anchorPoint = new BlockPos(
                    Phanper.this.anchorPoint.getX(), Phanper.this.level().getSeaLevel() + 1, Phanper.this.anchorPoint.getZ()
                );
            }
        }
    }

    /** Port of the vanilla phantom's circling goal that keeps the phanper orbiting its anchor. */
    class PhanperCircleAroundAnchorGoal extends PhanperMoveTargetGoal
    {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        @Override
        public boolean canUse()
        {
            return Phanper.this.getTarget() == null || Phanper.this.attackPhase == AttackPhase.CIRCLE;
        }

        @Override
        public void start()
        {
            this.distance = 5.0F + Phanper.this.random.nextFloat() * 10.0F;
            this.height = -4.0F + Phanper.this.random.nextFloat() * 9.0F;
            this.clockwise = Phanper.this.random.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        @Override
        public void tick()
        {
            if (Phanper.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.height = -4.0F + Phanper.this.random.nextFloat() * 9.0F;
            }

            if (Phanper.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
                this.distance++;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.clockwise = -this.clockwise;
                }
            }

            if (Phanper.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = Phanper.this.random.nextFloat() * 2.0F * (float) Math.PI;
                this.selectNext();
            }

            if (this.touchingTarget()) {
                this.selectNext();
            }

            if (Phanper.this.moveTargetPoint.y < Phanper.this.getY() && !Phanper.this.level().isEmptyBlock(Phanper.this.blockPosition().below(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }

            if (Phanper.this.moveTargetPoint.y > Phanper.this.getY() && !Phanper.this.level().isEmptyBlock(Phanper.this.blockPosition().above(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }
        }

        private void selectNext()
        {
            if (BlockPos.ZERO.equals(Phanper.this.anchorPoint)) {
                Phanper.this.anchorPoint = Phanper.this.blockPosition();
            }

            this.angle = this.angle + this.clockwise * 15.0F * (float) (Math.PI / 180.0);
            Phanper.this.moveTargetPoint = Vec3.atLowerCornerOf(Phanper.this.anchorPoint)
                .add(this.distance * Mth.cos(this.angle), -4.0F + this.height, this.distance * Mth.sin(this.angle));
        }
    }

    /**
     * The dive itself. Replaces the vanilla phantom's biting sweep: the phanper locks the dive onto
     * the player's position at take-off, detonates only when it actually reaches the player, and
     * treats anything else - the player dodging away, hitting a wall or getting hurt mid-dive - as a
     * miss that simply flies back into the sky without exploding.
     */
    class PhanperSweepAttackGoal extends PhanperMoveTargetGoal
    {
        private static final int CAT_SEARCH_TICK_DELAY = 20;
        private boolean isScaredOfCat;
        private int catSearchTick;

        @Override
        public boolean canUse()
        {
            return Phanper.this.getTarget() != null && Phanper.this.attackPhase == AttackPhase.SWOOP;
        }

        @Override
        public boolean canContinueToUse()
        {
            LivingEntity target = Phanper.this.getTarget();
            if (target == null) {
                return false;
            }
            if (!target.isAlive()) {
                return false;
            }
            if (target instanceof Player player && (target.isSpectator() || player.isCreative())) {
                return false;
            }
            if (!this.canUse()) {
                return false;
            }
            if (Phanper.this.tickCount > this.catSearchTick) {
                this.catSearchTick = Phanper.this.tickCount + CAT_SEARCH_TICK_DELAY;
                List<Cat> cats = Phanper.this.level()
                    .getEntitiesOfClass(Cat.class, Phanper.this.getBoundingBox().inflate(16.0), EntitySelector.ENTITY_STILL_ALIVE);
                for (Cat cat : cats) {
                    cat.hiss();
                }
                this.isScaredOfCat = !cats.isEmpty();
            }
            return !this.isScaredOfCat;
        }

        @Override
        public void start()
        {
            LivingEntity target = Phanper.this.getTarget();
            if (target != null) {
                // Commit the dive to where the player is right now; moving away dodges it.
                Phanper.this.diveTarget = new Vec3(target.getX(), target.getY(0.5), target.getZ());
            }
            Phanper.this.sweepTicks = 0;
            Phanper.this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        }

        @Override
        public void stop()
        {
            Phanper.this.setTarget(null);
            Phanper.this.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void tick()
        {
            LivingEntity target = Phanper.this.getTarget();
            if (target == null) {
                return;
            }
            Phanper.this.moveTargetPoint = Phanper.this.diveTarget != null
                ? Phanper.this.diveTarget
                : new Vec3(target.getX(), target.getY(0.5), target.getZ());
            if (Phanper.this.getBoundingBox().inflate(0.3F).intersects(target.getBoundingBox())) {
                Phanper.this.explodePhanper();
            } else if (Phanper.this.horizontalCollision
                || Phanper.this.hurtTime > 0
                || Phanper.this.moveTargetPoint.distanceToSqr(Phanper.this.getX(), Phanper.this.getY(), Phanper.this.getZ()) < 4.0
                || ++Phanper.this.sweepTicks > MAX_SWEEP_TICKS) {
                // Missed: the player dodged, so no explosion; circle back up into the sky.
                Phanper.this.attackPhase = AttackPhase.CIRCLE;
            }
        }
    }

    /** Port of the vanilla phantom's {@code PhantomMoveTargetGoal} base. */
    abstract class PhanperMoveTargetGoal extends Goal
    {
        PhanperMoveTargetGoal()
        {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        protected boolean touchingTarget()
        {
            return Phanper.this.moveTargetPoint.distanceToSqr(Phanper.this.getX(), Phanper.this.getY(), Phanper.this.getZ()) < 4.0;
        }
    }

    class PhanperBodyRotationControl extends BodyRotationControl
    {
        PhanperBodyRotationControl(Mob mob)
        {
            super(mob);
        }

        @Override
        public void clientTick()
        {
            Phanper.this.yHeadRot = Phanper.this.yBodyRot;
            Phanper.this.yBodyRot = Phanper.this.getYRot();
        }
    }

    class PhanperLookControl extends LookControl
    {
        PhanperLookControl(Mob mob)
        {
            super(mob);
        }

        @Override
        public void tick()
        {
        }
    }

    class PhanperMoveControl extends MoveControl
    {
        private float speed = 0.1F;

        PhanperMoveControl(Mob mob)
        {
            super(mob);
        }

        @Override
        public void tick()
        {
            if (Phanper.this.horizontalCollision) {
                Phanper.this.setYRot(Phanper.this.getYRot() + 180.0F);
                this.speed = 0.1F;
            }

            double d0 = Phanper.this.moveTargetPoint.x - Phanper.this.getX();
            double d1 = Phanper.this.moveTargetPoint.y - Phanper.this.getY();
            double d2 = Phanper.this.moveTargetPoint.z - Phanper.this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            if (Math.abs(d3) > 1.0E-5F) {
                double d4 = 1.0 - Math.abs(d1 * 0.7F) / d3;
                d0 *= d4;
                d2 *= d4;
                d3 = Math.sqrt(d0 * d0 + d2 * d2);
                double d5 = Math.sqrt(d0 * d0 + d2 * d2 + d1 * d1);
                float f = Phanper.this.getYRot();
                float f1 = (float) Mth.atan2(d2, d0);
                float f2 = Mth.wrapDegrees(Phanper.this.getYRot() + 90.0F);
                float f3 = Mth.wrapDegrees(f1 * (180.0F / (float) Math.PI));
                Phanper.this.setYRot(Mth.approachDegrees(f2, f3, 4.0F) - 90.0F);
                Phanper.this.yBodyRot = Phanper.this.getYRot();
                if (Mth.degreesDifferenceAbs(f, Phanper.this.getYRot()) < 3.0F) {
                    this.speed = Mth.approach(this.speed, 1.8F, 0.005F * (1.8F / this.speed));
                } else {
                    this.speed = Mth.approach(this.speed, 0.2F, 0.025F);
                }

                float f4 = (float) (-(Mth.atan2(-d1, d3) * 180.0F / (float) Math.PI));
                Phanper.this.setXRot(f4);
                float f5 = Phanper.this.getYRot() + 90.0F;
                double d6 = this.speed * Mth.cos(f5 * (float) (Math.PI / 180.0)) * Math.abs(d0 / d5);
                double d7 = this.speed * Mth.sin(f5 * (float) (Math.PI / 180.0)) * Math.abs(d2 / d5);
                double d8 = this.speed * Mth.sin(f4 * (float) (Math.PI / 180.0)) * Math.abs(d1 / d5);
                Vec3 vec3 = Phanper.this.getDeltaMovement();
                Phanper.this.setDeltaMovement(vec3.add(new Vec3(d6, d8, d7).subtract(vec3).scale(0.2)));
            }
        }
    }
}
