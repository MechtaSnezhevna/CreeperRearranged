package io.github.mechtasnezhevna.creeper_rearranged.entity.creepaler;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * Creepaler - a pale creeper variant that freezes while it is being watched.
 *
 * <p>It sleeps by default, wearing its dark texture, and only wakes when a player watches it from
 * within {@value #ACTIVATION_RANGE} blocks. An active creepaler freezes completely while a player's
 * gaze is on it - it cannot move, turn, attack, mount anything or be pushed, and the renderer
 * stops all animation - and falls back asleep once nobody is watching and it has no target. While
 * nobody looks at it it sprints at players at about 8 m/s and detonates like any creeper variant.
 * It never despawns in daylight like the creaking it is modelled on; its loot changes with the day
 * and the weather instead.
 */
public class Creepaler extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "creepaler_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/creepaler.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.kulibai.idle";
    public static final String ANIMATION_MOVE = "animation.kulibai.move";

    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
        SynchedEntityData.defineId(Creepaler.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FROZEN =
        SynchedEntityData.defineId(Creepaler.class, EntityDataSerializers.BOOLEAN);
    /** Yaw captured when the freeze starts, synced so the client renders the same statue pose. */
    private static final EntityDataAccessor<Float> DATA_FROZEN_YAW =
        SynchedEntityData.defineId(Creepaler.class, EntityDataSerializers.FLOAT);

    private static final int ANIMATION_TRANSITION_TICKS = 5;
    /** Distance (blocks) within which a player's gaze freezes an active creepaler. */
    private static final double OBSERVATION_RANGE = 32.0;
    /** Distance (blocks) within which a player's gaze wakes an inactive creepaler. */
    private static final double ACTIVATION_RANGE = 12.0;
    /** Maximum angle (radians) between the player's gaze and the line to the creepaler - 60 degrees. */
    private static final float OBSERVATION_ANGLE = (float) Math.toRadians(60.0);
    private static final String TAG_ACTIVE = "CreepalerActive";

    /**
     * Blocks that never count as blocking the creepaler's view, matching the creaking: glass,
     * stained glass, tinted glass, iron bars and powder snow. Copper bars and copper grates do not
     * exist in 1.21.1.
     */
    private static final Set<Block> VISION_TRANSPARENT_BLOCKS = Set.of(
        Blocks.GLASS,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS,
        Blocks.TINTED_GLASS,
        Blocks.IRON_BARS,
        Blocks.POWDER_SNOW
    );

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public Creepaler(EntityType<? extends Creepaler> entityType, Level level)
    {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }
    // --------------------------------------------------------- observation

    /**
     * Whether the given player counts as watching this creepaler, following the creaking's rules:
     * the player must be in survival or adventure, must not share the creepaler's team, the line
     * between their eyes must be free of solid blocks within {@code range} blocks (glass, stained
     * glass, tinted glass, iron bars and powder snow do not count as blocking) and the angle
     * between the player's gaze and that line must be under 60 degrees. A player wearing a carved
     * pumpkin is never counted as watching.
     */
    private static boolean isPlayerLookingAtCreepaler(ServerLevel level, Creepaler creepaler, Player player, double range)
    {
        if (player.isSpectator() || player.isCreative()) {
            return false;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
            return false;
        }
        if (creepaler.getTeam() != null && creepaler.getTeam() == player.getTeam()) {
            return false;
        }
        Vec3 creepalerEye = creepaler.getEyePosition();
        Vec3 playerEye = player.getEyePosition();
        if (playerEye.distanceToSqr(creepalerEye) > range * range) {
            return false;
        }
        if (!hasLineOfSight(level, creepalerEye, playerEye, creepaler)) {
            return false;
        }
        Vec3 gaze = player.getLookAngle();
        Vec3 toCreepaler = creepalerEye.subtract(playerEye).normalize();
        return Math.acos(Mth.clamp(gaze.dot(toCreepaler), -1.0, 1.0)) <= OBSERVATION_ANGLE;
    }

    /**
     * Raycast from the creepaler's eye to the player's eye. Vision-transparent blocks are skipped
     * and the ray continues past them; any other block with a collision shape blocks the gaze.
     */
    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Vec3 to, Creepaler creepaler)
    {
        Vec3 start = from;
        Vec3 direction = to.subtract(from).normalize();
        for (int i = 0; i < 64; i++) {
            BlockHitResult hit = level.clip(
                new ClipContext(start, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, creepaler)
            );
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!VISION_TRANSPARENT_BLOCKS.contains(state.getBlock())) {
                return false;
            }
            Vec3 past = hit.getLocation().add(direction.scale(0.05));
            if (past.distanceToSqr(to) < 0.01) {
                return true;
            }
            start = past;
        }
        return false;
    }

    /** First player that is currently watching this creepaler, or {@code null}. */
    @Nullable
    private Player findObserver(ServerLevel level)
    {
        double range = this.isActive() ? OBSERVATION_RANGE : ACTIVATION_RANGE;
        for (Player player : level.players()) {
            if (isPlayerLookingAtCreepaler(level, this, player, range)) {
                return player;
            }
        }
        return null;
    }

    // -------------------------------------------------------------- ticking

    @Override
    public void tick()
    {
        super.tick();
        if (this.isFrozen()) {
            // Hold the exact pose that was captured when the freeze started, on both sides.
            float yaw = this.entityData.get(DATA_FROZEN_YAW);
            this.setYRot(yaw);
            this.yRotO = yaw;
            this.yBodyRot = yaw;
            this.yBodyRotO = yaw;
            this.yHeadRot = yaw;
            this.yHeadRotO = yaw;
        }
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        Player observer = this.findObserver(serverLevel);
        if (this.isActive()) {
            if (observer != null) {
                // Watched: freeze in place - no movement, no turning, no fuse. The target is kept
                // so the chase resumes the moment the player looks away; the frozen locks below
                // (travel, yaw, getSwellDir, push, canRide, doHurtTarget) hold the statue anyway.
                this.getNavigation().stop();
                this.setFrozen(true);
            } else if (this.getTarget() == null) {
                // Nobody is watching and there is nothing to chase: fall back asleep.
                this.deactivate();
            } else {
                this.setFrozen(false);
            }
        } else if (observer != null) {
            // A player watches from within 12 blocks: wake up and stalk them.
            this.setActive(true);
            this.playSound(SOUND_ACTIVATE, 1.0F, 1.0F);
        }
    }

    /** Quietly clears the frozen flag and falls back asleep with the deactivate sound. */
    private void deactivate()
    {
        if (this.isFrozen()) {
            this.entityData.set(DATA_FROZEN, false);
        }
        this.setActive(false);
        this.playSound(SOUND_DEACTIVATE, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------ active

    public boolean isActive()
    {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active)
    {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isFrozen()
    {
        return this.entityData.get(DATA_FROZEN);
    }

    /** Enters or leaves the frozen statue state; the freeze/unfreeze sound plays on the server. */
    public void setFrozen(boolean frozen)
    {
        if (this.entityData.get(DATA_FROZEN) == frozen) {
            return;
        }
        this.entityData.set(DATA_FROZEN, frozen);
        if (frozen) {
            this.entityData.set(DATA_FROZEN_YAW, this.getYRot());
            this.playSound(SOUND_FREEZE, 1.0F, 1.0F);
        } else {
            this.playSound(SOUND_UNFREEZE, 1.0F, 1.0F);
        }
    }

    // ------------------------------------------------------------- frozen

    @Override
    public void travel(Vec3 travelVector)
    {
        if (this.isFrozen() || !this.isActive()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    /** A frozen creepaler cannot be pushed around by other entities. */
    @Override
    public void push(Entity entity)
    {
        if (!this.isFrozen()) {
            super.push(entity);
        }
    }

    /** A frozen creepaler cannot mount anything. */
    @Override
    protected boolean canRide(Entity entity)
    {
        return !this.isFrozen() && super.canRide(entity);
    }

    /** A sleeping creepaler never picks up a target. A frozen one may keep or regain a target so
     * it resumes the chase once unwatched; the frozen state itself still blocks movement, turning,
     * melee attacks and the fuse, so the statue behaviour is unaffected. */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && !this.isActive()) {
            return;
        }
        super.setTarget(target);
    }

    /** A frozen creepaler cannot melee attack, even if it keeps a target while watched. */
    @Override
    public boolean doHurtTarget(Entity target)
    {
        if (this.isFrozen()) {
            return false;
        }
        return super.doHurtTarget(target);
    }

    /** A frozen creepaler's fuse is held at -1, so it cannot swell or detonate while watched. */
    @Override
    public int getSwellDir()
    {
        return this.isFrozen() ? -1 : super.getSwellDir();
    }

    /** Flint and steel cannot prime a frozen creepaler. */
    @Override
    public void ignite()
    {
        if (!this.isFrozen()) {
            super.ignite();
        }
    }

    // --------------------------------------------------------------- sounds

    /** Sound events registered in {@code assets/creeper_rearranged/sounds.json} under {@code mob.creepaler.*}. */
    private static final SoundEvent SOUND_AMBIENT = sound("idle");
    private static final SoundEvent SOUND_ACTIVATE = sound("activate");
    private static final SoundEvent SOUND_DEACTIVATE = sound("deactivate");
    private static final SoundEvent SOUND_FREEZE = sound("freeze");
    private static final SoundEvent SOUND_UNFREEZE = sound("unfreeze");
    private static final SoundEvent SOUND_DEATH = sound("death");
    private static final SoundEvent SOUND_STEP = sound("step");
    private static final SoundEvent SOUND_SPAWN = sound("spawn");

    private static SoundEvent sound(String name)
    {
        return SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "mob.creepaler." + name)
        );
    }

    /** Only a sleeping creepaler makes the creaking idle sounds. */
    @Override
    protected SoundEvent getAmbientSound()
    {
        return this.isActive() ? null : SOUND_AMBIENT;
    }

    /** The creepaler is hurt with the generic hurt sound; the creaking attack sounds are not used. */
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource)
    {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SOUND_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state)
    {
        this.playSound(SOUND_STEP, 0.15F, 1.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
    )
    {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.playSound(SOUND_SPAWN, 1.0F, 1.0F);
        return data;
    }

    // -------------------------------------------------------------- syncing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_FROZEN, false);
        builder.define(DATA_FROZEN_YAW, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_ACTIVE, this.isActive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setActive(tag.getBoolean(TAG_ACTIVE));
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

    /**
     * A watched creepaler is a statue: the controller stops, so the model holds its neutral pose
     * without any animation. Otherwise it idles or walks like every other creeper variant.
     */
    private PlayState animationPredicate(AnimationState<Creepaler> state)
    {
        if (this.isFrozen()) {
            return PlayState.STOP;
        }
        String animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
