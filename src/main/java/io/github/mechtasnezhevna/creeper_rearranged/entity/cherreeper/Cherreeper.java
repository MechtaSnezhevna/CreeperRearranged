package io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
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
 * Cherreeper - a docile creeper variant wearing a cherry blossom hat.
 *
 * <p>It is neutral: it never picks a target on its own and only turns hostile when it is attacked
 * itself or when a player angers bees nearby (the cherreeper shares the angered bee's target).
 * Once tamed with a honey bottle it never attacks players again; the same bottle feeds it (restores
 * {@value #HONEY_HEAL_AMOUNT} HP), and a right click toggles a forced sitting pose that is kept even
 * when the cherreeper is hurt. While calm and still wild it randomly alternates between standing and
 * sitting, each state lasting a few seconds; a tamed one keeps the pose its owner last commanded and
 * never switches on its own, and while standing it follows its owner around like a cat or dog. It
 * cannot breed and its blast is a plain vanilla creeper explosion.
 */
public class Cherreeper extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "cherreeper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/cherreeper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.cherreeper.idle";
    public static final String ANIMATION_MOVE = "animation.cherreeper.move";
    public static final String ANIMATION_SIT = "animation.cherreeper.sit";
    /** Custom name (case-insensitive) that swaps the texture to {@code cherreeper_color}. */
    public static final String COLOR_TEXTURE_NAME = "color";

    private static final EntityDataAccessor<Boolean> DATA_TAMED =
        SynchedEntityData.defineId(Cherreeper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SITTING =
        SynchedEntityData.defineId(Cherreeper.class, EntityDataSerializers.BOOLEAN);
    /** Sitting was forced by a player; such a cherreeper never stands up on its own. */
    private static final EntityDataAccessor<Boolean> DATA_MANUAL_SIT =
        SynchedEntityData.defineId(Cherreeper.class, EntityDataSerializers.BOOLEAN);
    /** Display name of the player who tamed this cherreeper, synced for the Jade tooltip. */
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
        SynchedEntityData.defineId(Cherreeper.class, EntityDataSerializers.STRING);
    /** UUID of the player who tamed this cherreeper; empty while untamed. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(Cherreeper.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int ANIMATION_TRANSITION_TICKS = 5;
    /** How often (ticks) the server scans nearby bees for freshly angered ones. */
    private static final int BEE_SCAN_INTERVAL = 20;
    /** Scan radius (blocks) around a cherreeper for angry bees to share their target. */
    private static final double BEE_ANGER_SCAN_RADIUS = 12.0;
    /** Health restored by one honey bottle on a tamed cherreeper. */
    private static final float HONEY_HEAL_AMOUNT = 6.0F;
    private static final int MIN_IDLE_TICKS = 4 * 20;
    private static final int MAX_IDLE_TICKS = 12 * 20;
    private static final int MIN_SIT_TICKS = 5 * 20;
    private static final int MAX_SIT_TICKS = 15 * 20;
    private static final String TAG_TAMED = "Tamed";
    private static final String TAG_SITTING = "Sitting";
    private static final String TAG_MANUAL_SIT = "ManualSit";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    /** Speed used while walking back to the owner, matching a wolf or cat. */
    private static final double FOLLOW_SPEED = 1.0;
    /** Follow starts once the owner is farther than this (blocks). */
    private static final float FOLLOW_START_DISTANCE = 10.0F;
    /** Follow stops once the owner is closer than this (blocks). */
    private static final float FOLLOW_STOP_DISTANCE = 2.0F;
    /** Squared distance at which the cherreeper teleports to the owner instead of walking. */
    private static final double TELEPORT_WHEN_DISTANCE_IS_SQ = 144.0;
    private static final int TELEPORT_MAX_ATTEMPTS = 10;
    private static final int TELEPORT_MIN_HORIZONTAL_DISTANCE = 2;
    private static final int TELEPORT_MAX_HORIZONTAL_DISTANCE = 3;
    private static final int TELEPORT_MAX_VERTICAL_DISTANCE = 1;
    /** Vanilla advancement id granted on tame: "Best Friends Forever" (zh: 永恒的伙伴). */
    private static final ResourceLocation TAME_ADVANCEMENT =
        ResourceLocation.withDefaultNamespace("husbandry/tame_an_animal");
    /** Criterion key inside {@link #TAME_ADVANCEMENT} that marks the tame. */
    private static final String TAME_ADVANCEMENT_CRITERION = "tamed_animal";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    /** Server-only countdown until the next random idle/sit state change. */
    private int nextStateTicks;

    public Cherreeper(EntityType<? extends Cherreeper> entityType, Level level)
    {
        super(entityType, level);
        this.nextStateTicks = this.random.nextIntBetweenInclusive(MIN_IDLE_TICKS, MAX_IDLE_TICKS);
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // The vanilla swell goal: only lights the fuse while a target is within 3 blocks, so a calm
        // cherreeper never detonates on its own.
        this.goalSelector.addGoal(2, new SwellGoal(this));
        // Like a vanilla creeper an untamed cherreeper runs from cats and ocelots; a tamed one has
        // nothing to fear, and a sitting one never moves at all.
        this.goalSelector.addGoal(3, new AvoidEntityGoal<Ocelot>(this, Ocelot.class, 6.0F, 1.0, 1.2, ocelot -> !this.isTamed() && !this.isSitting()));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<Cat>(this, Cat.class, 6.0F, 1.0, 1.2, cat -> !this.isTamed() && !this.isSitting()));
        this.goalSelector.addGoal(4, new SittingAwareMeleeAttackGoal());
        this.goalSelector.addGoal(5, new CherreeperFollowOwnerGoal(FOLLOW_SPEED, FOLLOW_START_DISTANCE, FOLLOW_STOP_DISTANCE));
        this.goalSelector.addGoal(6, new SittingAwareRandomStrollGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Only revenge targets: a player who attacks it (or an angry bee's target, set in tick()).
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** MeleeAttackGoal moves the cherreeper to its target; a sitting one stays put. */
    private final class SittingAwareMeleeAttackGoal extends MeleeAttackGoal
    {
        private SittingAwareMeleeAttackGoal()
        {
            super(Cherreeper.this, 1.0, false);
        }

        @Override
        public boolean canUse()
        {
            return !Cherreeper.this.isSitting() && super.canUse();
        }
    }

    private final class SittingAwareRandomStrollGoal extends WaterAvoidingRandomStrollGoal
    {
        private SittingAwareRandomStrollGoal()
        {
            super(Cherreeper.this, 0.8);
        }

        @Override
        public boolean canUse()
        {
            return !Cherreeper.this.isSitting() && super.canUse();
        }
    }

    /**
     * Follows the tamed owner like a wolf or cat: once a standing tamed cherreeper drifts more than
     * {@value Cherreeper#FOLLOW_START_DISTANCE} blocks away from its owner it walks back, and it
     * teleports instead of walking when the gap exceeds 12 blocks. Sitting, riding, leashed and
     * spectator-owner situations never move at all.
     */
    private final class CherreeperFollowOwnerGoal extends Goal
    {
        private final PathNavigation navigation;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        @Nullable
        private Player owner;
        private int timeToRecalcPath;
        private float oldWaterCost;

        private CherreeperFollowOwnerGoal(double speedModifier, float startDistance, float stopDistance)
        {
            this.navigation = Cherreeper.this.getNavigation();
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            Player player = Cherreeper.this.getOwner();
            if (player == null) {
                return false;
            }
            if (Cherreeper.this.unableToMoveToOwner()) {
                return false;
            }
            if (Cherreeper.this.distanceToSqr(player) < (double) (this.startDistance * this.startDistance)) {
                return false;
            }
            this.owner = player;
            return true;
        }

        @Override
        public boolean canContinueToUse()
        {
            if (this.navigation.isDone()) {
                return false;
            }
            return !Cherreeper.this.unableToMoveToOwner()
                && !(Cherreeper.this.distanceToSqr(this.owner) <= (double) (this.stopDistance * this.stopDistance));
        }

        @Override
        public void start()
        {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = Cherreeper.this.getPathfindingMalus(PathType.WATER);
            Cherreeper.this.setPathfindingMalus(PathType.WATER, 0.0F);
        }

        @Override
        public void stop()
        {
            this.owner = null;
            this.navigation.stop();
            Cherreeper.this.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick()
        {
            if (this.owner == null) {
                return;
            }
            boolean teleport = Cherreeper.this.distanceToSqr(this.owner) >= TELEPORT_WHEN_DISTANCE_IS_SQ;
            if (!teleport) {
                Cherreeper.this.getLookControl().setLookAt(this.owner, 10.0F, (float) Cherreeper.this.getMaxHeadXRot());
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                if (teleport) {
                    Cherreeper.this.teleportToOwner();
                } else {
                    this.navigation.moveTo(this.owner, this.speedModifier);
                }
            }
        }
    }

    // ------------------------------------------------------------ tame & sit

    public boolean isTamed()
    {
        return this.entityData.get(DATA_TAMED);
    }

    public void setTamed(boolean tamed)
    {
        this.entityData.set(DATA_TAMED, tamed);
        if (tamed) {
            this.setPersistenceRequired();
        }
    }

    public boolean isSitting()
    {
        return this.entityData.get(DATA_SITTING);
    }

    public void setSitting(boolean sitting)
    {
        this.entityData.set(DATA_SITTING, sitting);
    }

    public boolean isManualSitting()
    {
        return this.entityData.get(DATA_MANUAL_SIT);
    }

    public void setManualSitting(boolean manualSitting)
    {
        this.entityData.set(DATA_MANUAL_SIT, manualSitting);
    }

    /** Display name of the taming player; empty while untamed. */
    public String getOwnerName()
    {
        return this.entityData.get(DATA_OWNER_NAME);
    }

    public void setOwnerName(String ownerName)
    {
        this.entityData.set(DATA_OWNER_NAME, ownerName);
    }

    @Nullable
    public UUID getOwnerUuid()
    {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUuid(@Nullable UUID uuid)
    {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    /** The player who tamed this cherreeper, or {@code null} while untamed or the owner is offline. */
    @Nullable
    public Player getOwner()
    {
        UUID uuid = this.getOwnerUuid();
        if (uuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(uuid);
    }

    /** A tamed cherreeper never attacks the player, whatever provokes it. */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (this.isTamed() && target instanceof Player) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (this.level().isClientSide) {
            boolean flag = stack.is(Items.HONEY_BOTTLE) || this.isTamed() || stack.is(ItemTags.CREEPER_IGNITERS);
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (stack.is(Items.HONEY_BOTTLE)) {
            if (!this.isTamed()) {
                this.tameWith(player);
                stack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(HONEY_HEAL_AMOUNT);
                stack.consume(1, player);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HONEY_DRINK, this.getSoundSource(), 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // Full health: keep the bottle and do not toggle the sitting pose.
            return InteractionResult.PASS;
        }

        if (this.isTamed()) {
            this.setManualSitting(!this.isManualSitting());
            if (this.isManualSitting()) {
                this.setSitting(true);
                this.getNavigation().stop();
                this.jumping = false;
                this.setTarget(null);
            } else {
                this.setSitting(false);
                this.nextStateTicks = this.random.nextIntBetweenInclusive(MIN_IDLE_TICKS, MAX_IDLE_TICKS);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ItemTags.CREEPER_IGNITERS)) {
            // Vanilla creeper ignition, kept for untamed cherreeper only.
            SoundEvent sound = stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
            this.level().playSound(player, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            this.ignite();
            if (!stack.isDamageableItem()) {
                stack.shrink(1);
            } else {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void tameWith(Player player)
    {
        this.setTamed(true);
        this.setOwnerName(player.getName().getString());
        this.setOwnerUuid(player.getUUID());
        // The pose it happens to be in right now becomes the owner's command: a sitting cherreeper
        // is locked into the manual sit and will never stand up by itself, a standing one stays up.
        this.setManualSitting(this.isSitting());
        this.playSound(SoundEvents.HONEY_DRINK, 1.0F, 1.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.HEART,
                this.getX(), this.getY() + 1.0, this.getZ(),
                7, 0.4, 0.4, 0.4, 0.1
            );
            if (player instanceof ServerPlayer serverPlayer) {
                // The cherreeper is a monster, so the vanilla tame_animal trigger (which only takes
                // an Animal) never fires; grant the advancement directly instead.
                AdvancementHolder holder = serverLevel.getServer().getAdvancements().get(TAME_ADVANCEMENT);
                if (holder != null) {
                    serverPlayer.getAdvancements().award(holder, TAME_ADVANCEMENT_CRITERION);
                }
            }
        }
    }

    // -------------------------------------------------------------- ticking

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.angerFromBees();

        // A player-forced sitting pose is absolute: it survives damage, targets and everything else.
        if (this.isManualSitting()) {
            this.setSitting(true);
            this.getNavigation().stop();
            this.nextStateTicks = 0;
            return;
        }
        // Chasing (or otherwise having a target) means business: stand up and walk.
        if (this.getTarget() != null) {
            if (this.isSitting()) {
                this.setSitting(false);
            }
            this.nextStateTicks = 0;
            return;
        }

        // Only a wild cherreeper idly alternates between standing and sitting on its own; a tamed
        // one keeps the pose its owner last commanded and never switches by itself.
        if (this.isTamed()) {
            return;
        }

        // Random idle/sit state machine, each state held for a random duration.
        if (this.nextStateTicks > 0) {
            this.nextStateTicks--;
            return;
        }
        if (this.isSitting()) {
            this.setSitting(false);
            this.nextStateTicks = this.random.nextIntBetweenInclusive(MIN_IDLE_TICKS, MAX_IDLE_TICKS);
        } else {
            this.setSitting(true);
            this.nextStateTicks = this.random.nextIntBetweenInclusive(MIN_SIT_TICKS, MAX_SIT_TICKS);
        }
    }

    /**
     * Shares the anger of nearby bees: when a bee within {@value #BEE_ANGER_SCAN_RADIUS} blocks is
     * freshly angry at a player (e.g. the player hit the bee or robbed its hive), the cherreeper
     * adopts that player as its own target.
     */
    private void angerFromBees()
    {
        if (this.isTamed() || this.getTarget() != null || this.tickCount % BEE_SCAN_INTERVAL != 0) {
            return;
        }
        AABB scanBox = this.getBoundingBox().inflate(BEE_ANGER_SCAN_RADIUS);
        for (Bee bee : this.level().getEntitiesOfClass(Bee.class, scanBox)) {
            if (!bee.isAlive() || !bee.isAngry()) {
                continue;
            }
            UUID angerTarget = bee.getPersistentAngerTarget();
            if (angerTarget == null) {
                continue;
            }
            Entity entity = ((ServerLevel) this.level()).getEntity(angerTarget);
            if (entity instanceof Player player) {
                this.setTarget(player);
                return;
            }
        }
    }

    // ------------------------------------------------------------- following

    /**
     * Whether walking is forbidden right now: the cherreeper was ordered to sit, is riding, is
     * leashed, or its owner is watching in spectator mode.
     */
    private boolean unableToMoveToOwner()
    {
        return this.isSitting() || this.isPassenger() || this.isLeashed()
            || this.getOwner() != null && this.getOwner().isSpectator();
    }

    private void teleportToOwner()
    {
        Player owner = this.getOwner();
        if (owner != null) {
            this.teleportToAroundBlockPos(owner.blockPosition());
        }
    }

    private void teleportToAroundBlockPos(BlockPos blockPos)
    {
        for (int i = 0; i < TELEPORT_MAX_ATTEMPTS; i++) {
            int j = this.random.nextIntBetweenInclusive(-TELEPORT_MAX_HORIZONTAL_DISTANCE, TELEPORT_MAX_HORIZONTAL_DISTANCE);
            int k = this.random.nextIntBetweenInclusive(-TELEPORT_MAX_HORIZONTAL_DISTANCE, TELEPORT_MAX_HORIZONTAL_DISTANCE);
            if (Math.abs(j) >= TELEPORT_MIN_HORIZONTAL_DISTANCE || Math.abs(k) >= TELEPORT_MIN_HORIZONTAL_DISTANCE) {
                int l = this.random.nextIntBetweenInclusive(-TELEPORT_MAX_VERTICAL_DISTANCE, TELEPORT_MAX_VERTICAL_DISTANCE);
                if (this.maybeTeleportTo(blockPos.getX() + j, blockPos.getY() + l, blockPos.getZ() + k)) {
                    return;
                }
            }
        }
    }

    private boolean maybeTeleportTo(int x, int y, int z)
    {
        BlockPos blockPos = new BlockPos(x, y, z);
        if (!this.canTeleportTo(blockPos)) {
            return false;
        }
        this.moveTo((double) x + 0.5, y, (double) z + 0.5, this.getYRot(), this.getXRot());
        this.getNavigation().stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos blockPos)
    {
        PathType pathType = WalkNodeEvaluator.getPathTypeStatic(this, blockPos);
        if (pathType != PathType.WALKABLE) {
            return false;
        }
        BlockState blockState = this.level().getBlockState(blockPos.below());
        if (blockState.getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos offset = blockPos.subtract(this.blockPosition());
        return this.level().noCollision(this, this.getBoundingBox().move(offset));
    }

    // -------------------------------------------------------------- syncing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_TAMED, false);
        builder.define(DATA_SITTING, false);
        builder.define(DATA_MANUAL_SIT, false);
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_TAMED, this.isTamed());
        tag.putBoolean(TAG_SITTING, this.isSitting());
        tag.putBoolean(TAG_MANUAL_SIT, this.isManualSitting());
        tag.putString(TAG_OWNER_NAME, this.getOwnerName());
        if (this.getOwnerUuid() != null) {
            tag.putUUID(TAG_OWNER_UUID, this.getOwnerUuid());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setTamed(tag.getBoolean(TAG_TAMED));
        this.setSitting(tag.getBoolean(TAG_SITTING));
        this.setManualSitting(tag.getBoolean(TAG_MANUAL_SIT));
        this.setOwnerName(tag.getString(TAG_OWNER_NAME));
        if (tag.hasUUID(TAG_OWNER_UUID)) {
            this.setOwnerUuid(tag.getUUID(TAG_OWNER_UUID));
        }
        this.nextStateTicks = this.random.nextIntBetweenInclusive(MIN_IDLE_TICKS, MAX_IDLE_TICKS);
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

    private PlayState animationPredicate(AnimationState<Cherreeper> state)
    {
        String animation;
        if (this.isSitting()) {
            animation = ANIMATION_SIT;
        } else {
            animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        }
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
