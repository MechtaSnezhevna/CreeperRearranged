package io.github.mechtasnezhevna.creeper_rearranged.entity.creepot;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Creepot - a creeper variant that sleeps inside a terracotta pot.
 *
 * <p>It is found as the {@code sleeping_creepot} item, buried in suspicious sand and suspicious
 * gravel and brushed out with archaeology. The item places a sleeping creepot entity, which stays
 * asleep until a player wakes it: a right click wakes it into a neutral state, an attack (left
 * click) wakes it hostile on the spot - a creative-mode attacker wakes it neutral instead. A
 * sleeping creepot is a statue that faces the nearest cardinal direction - it does not move, drift
 * or turn. A neutral creepot never hunts on its own and never detonates on its own; once hostile it
 * chases players at half a vanilla creeper's walking speed.
 *
 * <p>While hostile, a creepot whose target is farther than {@value #ROLL_DISTANCE} blocks rolls
 * after it instead of walking, sprinting at {@value #ROLL_SPEED_MODIFIER}x its own walk speed -
 * 2.25x a vanilla creeper's speed. A roll does not stop at the 16-block boundary: it keeps rolling
 * until the target is close enough for the 3-block explosion recognition range - where it stands
 * up and primes its fuse. A rolling creepot never hops on its own, but the moment an obstacle
 * stops it it jumps the single-block step and rolls on; a roll that stays blocked for
 * {@value #ROLL_STUCK_TICKS} ticks stops and re-checks its sight on the target - a target it can
 * no longer see is dropped so the target goal re-acquires it, a visible one keeps the chase going
 * and every later jump-requiring spot is jumped through even though the distance calls for
 * rolling. If the explosion is cancelled or dodged it re-checks the distance and rolls again once
 * the target is far enough. Its follow range covers {@value #ROLL_DISTANCE} blocks with room to
 * spare, and it never drops a target it has seen just because the line of sight closes mid-roll,
 * so the roll chase never stalls. Its blast is 0.75x a vanilla creeper's (radius
 * {@value #EXPLOSION_RADIUS}).
 *
 * <p>Every wake-up - hostile or neutral - plays the wake animation, then rolls a one-time
 * {@value #POT_CHANCE} chance to play the POT animation once; it never plays it again. A creepot
 * that grew in place of a trial chamber decorated pot is born holding that pot's loot in its
 * storage slot.
 *
 * <p>Every creepot carries a decorated-pot-style single storage slot: a right click with an item
 * puts one of it in when there is room - an empty slot accepts anything, a filled slot only more
 * of the same item up to its stack size - and waking a sleeping creepot with a right click puts
 * the item in at the same time. The slot's contents drop alongside the normal loot when the
 * creepot is defeated; gunpowder, bricks and sherds never occupy the slot.
 */
public class Creepot extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "creepot_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/creepot.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.creepot.idle";
    public static final String ANIMATION_MOVE = "animation.creepot.move";
    public static final String ANIMATION_SLEEP = "animation.creepot.sleep";
    public static final String ANIMATION_WAKE = "animation.creepot.wake";
    public static final String ANIMATION_POT = "animation.creepot.POT";
    public static final String ANIMATION_SCROLL = "animation.creepot.scroll";

    /** Animation states the synced {@link #DATA_ANIM} field can hold. */
    public static final int ANIM_NONE = 0;
    public static final int ANIM_WAKE = 1;
    public static final int ANIM_POT = 2;

    /** Blast radius: 0.75x the vanilla creeper's 3.0. */
    private static final float EXPLOSION_RADIUS = 2.25F;
    /** Walk speed: 0.5x the vanilla creeper's 0.25. */
    private static final double WALK_SPEED = 0.125;
    /** Roll chase speed as a multiple of the creepot's own walk attribute. */
    private static final double ROLL_SPEED_MODIFIER = 4.5;
    /** A target farther than this (blocks) makes the creepot roll instead of walking. */
    private static final double ROLL_DISTANCE = 16.0;
    private static final double ROLL_DISTANCE_SQ = ROLL_DISTANCE * ROLL_DISTANCE;
    /** Inside this radius (3 blocks) the creepot stands up to swell; rolling stops only here. */
    private static final double SWELL_RANGE_SQ = 9.0;
    /** A blocked roll gives up after this many ticks, stops and re-checks sight on the target. */
    private static final int ROLL_STUCK_TICKS = 30;
    /** Wake animation length in ticks (2 seconds). */
    private static final int WAKE_TICKS = 40;
    /** POT animation length in ticks (3 seconds). */
    private static final int POT_TICKS = 60;
    /** Chance to play the one-time POT animation after the wake animation finishes. */
    private static final float POT_CHANCE = 0.25F;
    /** How far the creepot keeps locking onto players, so the 16-block roll has room to trigger. */
    private static final double FOLLOW_RANGE = 32.0;
    /** GeckoLib crossfade between animation states. */
    private static final int ANIMATION_TRANSITION_TICKS = 5;

    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
        SynchedEntityData.defineId(Creepot.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE =
        SynchedEntityData.defineId(Creepot.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ROLLING =
        SynchedEntityData.defineId(Creepot.class, EntityDataSerializers.BOOLEAN);
    /** Current one-shot animation: {@link #ANIM_NONE}, {@link #ANIM_WAKE} or {@link #ANIM_POT}. */
    private static final EntityDataAccessor<Integer> DATA_ANIM =
        SynchedEntityData.defineId(Creepot.class, EntityDataSerializers.INT);
    /** Whether this creepot grew in place of a trial chamber decorated pot. */
    private static final EntityDataAccessor<Boolean> DATA_TRIAL =
        SynchedEntityData.defineId(Creepot.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_SLEEPING = "Sleeping";
    private static final String TAG_HOSTILE = "Hostile";
    private static final String TAG_ANIM = "AnimState";
    private static final String TAG_POT_ROLLED = "PotRolled";
    private static final String TAG_TRIAL = "TrialChamber";
    private static final String TAG_STORED_ITEM = "StoredItem";

    /** Extra loot table rolled when a trial-chamber creepot is defeated. */
    private static final ResourceKey<LootTable> TRIAL_CHAMBER_POT_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("pots/trial_chambers/corridor")
    );

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    /** Server-side countdown for the current {@link #DATA_ANIM} animation. */
    private int animTicks;
    /** Server-side: the one-time POT roll has already been made. */
    private boolean potRolled;
    /** Server-side: how long the current roll has been blocked with no usable path. */
    private int rollStuckTicks;
    /** The decorated-pot-style single storage slot. */
    private ItemStack storedItem = ItemStack.EMPTY;

    public Creepot(EntityType<? extends Creepot> entityType, Level level)
    {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
            .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(4, new CreepotRollGoal(this));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new CreepotTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_SLEEPING, true);
        builder.define(DATA_HOSTILE, false);
        builder.define(DATA_ROLLING, false);
        builder.define(DATA_ANIM, ANIM_NONE);
        builder.define(DATA_TRIAL, false);
    }
    /**
     * Drives the state machine. A sleeping creepot stands still, keeps no target and cannot swell;
     * an animating one plays out its one-shot wake/POT sequence without moving or swelling;
     * otherwise the vanilla creeper AI - chase, fuse, explosion - runs untouched.
     */
    @Override
    public void tick()
    {
        if (this.isSleeping()) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
        this.updateAnimTicks();
        super.tick();
        if (this.isSleeping()) {
            this.lockSleepYaw();
        }
        if (this.isSleeping() || this.isAnimating()) {
            this.getNavigation().stop();
        }
    }

    /**
     * Counts down the current one-shot animation on the server. The wake animation runs for
     * {@value #WAKE_TICKS} ticks; when it finishes the one-time {@value #POT_CHANCE} roll decides
     * whether the POT animation ({@value #POT_TICKS} ticks) follows, and {@link #potRolled} is
     * marked so it can never happen again. While an animation plays the creepot stands still.
     */
    private void updateAnimTicks()
    {
        if (this.level().isClientSide || this.getAnimState() == ANIM_NONE) {
            return;
        }
        if (--this.animTicks > 0) {
            this.getNavigation().stop();
            return;
        }
        if (this.getAnimState() == ANIM_WAKE && !this.potRolled) {
            this.potRolled = true;
            if (this.random.nextFloat() < POT_CHANCE) {
                this.setAnim(ANIM_POT, POT_TICKS);
                return;
            }
        }
        this.setAnim(ANIM_NONE, 0);
    }

    /** Wakes the creepot: a right-click wake is neutral, an attack wake is hostile. */
    private void wakeUp(boolean hostile)
    {
        this.setSleeping(false);
        this.setHostile(hostile);
        if (!hostile) {
            this.setTarget(null);
        }
        this.setSwellDir(-1);
        this.setAnim(ANIM_WAKE, WAKE_TICKS);
    }

    private void setAnim(int state, int ticks)
    {
        this.entityData.set(DATA_ANIM, state);
        this.animTicks = ticks;
    }

    /**
     * A right click on a sleeping creepot wakes it neutral - whatever the hand holds, one item goes
     * into the storage slot when there is room, exactly like a decorated pot. Once awake, a right
     * click with an item puts one into the storage when possible; when the storage is full or holds
     * a different item, the vanilla creeper interactions (lighting the fuse with flint and steel)
     * work normally.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (this.isSleeping()) {
            if (!this.level().isClientSide) {
                this.wakeUp(false);
                this.insertIntoStorage(player, player.getItemInHand(hand), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.insertIntoStorage(player, player.getItemInHand(hand), false)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Mirrors the decorated pot's insert interaction: an empty slot accepts one item, a slot holding
     * the same item grows until its stack is full, and anything else is left alone (an empty hand
     * plays the fail sound only while the creepot is waking). Returns whether the click was
     * consumed by the storage.
     */
    private boolean insertIntoStorage(Player player, ItemStack held, boolean failSoundOnEmptyHand)
    {
        if (held.isEmpty()) {
            if (failSoundOnEmptyHand && !this.level().isClientSide) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return false;
        }
        ItemStack stored = this.storedItem;
        if (!stored.isEmpty()
            && (!ItemStack.isSameItemSameComponents(stored, held) || stored.getCount() >= stored.getMaxStackSize())) {
            return false;
        }
        if (!this.level().isClientSide) {
            Item item = held.getItem();
            player.awardStat(Stats.ITEM_USED.get(item));
            ItemStack consumed = held.consumeAndReturn(1, player);
            if (stored.isEmpty()) {
                this.storedItem = consumed;
            } else {
                this.storedItem.grow(1);
            }
            float pitch = 1.0F + 0.7F * ((float) this.storedItem.getCount() / (float) this.storedItem.getMaxStackSize());
            this.level().playSound(null, this.blockPosition(), SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, pitch);
        }
        return true;
    }

    /** An attack on a sleeping creepot wakes it; a creative-mode attacker wakes it neutral instead. */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (this.isSleeping()
            && !this.level().isClientSide
            && source.getEntity() instanceof LivingEntity attacker) {
            boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
            this.wakeUp(!creative);
            if (!creative) {
                this.setTarget(attacker);
            }
        }
        return super.hurt(source, amount);
    }

    /** A sleeping or neutral creepot never keeps a target; once hostile it can be given one. */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        if (target != null && !this.isHostile()) {
            return;
        }
        super.setTarget(target);
    }

    /** A sleeping or animating creepot holds its fuse at -1: no swelling, no detonation. */
    @Override
    public int getSwellDir()
    {
        return this.isSleeping() || this.isAnimating() ? -1 : super.getSwellDir();
    }

    /** A sleeping or animating creepot cannot build up a fuse; the swell only starts once it stands up. */
    @Override
    public void setSwellDir(int swellDir)
    {
        if (swellDir > 0 && (this.isSleeping() || this.isAnimating())) {
            return;
        }
        super.setSwellDir(swellDir);
    }

    /** A sleeping creepot cannot be lit; flint and steel wakes it neutral instead. */
    @Override
    public void ignite()
    {
        if (!this.isSleeping()) {
            super.ignite();
        }
    }

    /** The blast is 0.75x a vanilla creeper's. */
    @Override
    protected float getVariantExplosionRadius()
    {
        return EXPLOSION_RADIUS;
    }

    /** A sleeping or animating creepot stands still: no walking, no gravity-driven drift. */
    @Override
    public void travel(Vec3 travelVector)
    {
        if (this.isSleeping() || this.isAnimating()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.isRolling() && !this.horizontalCollision && this.rollStuckTicks <= 0) {
            this.setJumping(false);
        }
        super.travel(travelVector);
    }

    /** A rolling creepot never hops on its own; once an obstacle stops it, it jumps it through. */
    @Override
    public void jumpFromGround()
    {
        if (!this.isRolling() || this.horizontalCollision || this.rollStuckTicks > 0) {
            super.jumpFromGround();
        }
    }

    /** Snaps a horizontal rotation to the nearest cardinal direction - due north, south, east or west. */
    private static float snapToCardinalYaw(float yaw)
    {
        return Math.round(Mth.wrapDegrees(yaw) / 90.0F) * 90.0F;
    }

    /** A sleeping creepot is a statue: it faces its nearest cardinal direction and never turns. */
    private void lockSleepYaw()
    {
        float yaw = snapToCardinalYaw(this.getYRot());
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
    }

    // ------------------------------------------------------------- state

    /** Whether this creepot is still a dormant pot. */
    public boolean isSleeping()
    {
        return this.entityData.get(DATA_SLEEPING);
    }

    /** Whether this creepot was woken by violence and hunts players. */
    public boolean isHostile()
    {
        return this.entityData.get(DATA_HOSTILE);
    }

    /** Whether this creepot is currently rolling after a far-away target. */
    public boolean isRolling()
    {
        return this.entityData.get(DATA_ROLLING);
    }

    /** Current one-shot animation: {@link #ANIM_NONE}, {@link #ANIM_WAKE} or {@link #ANIM_POT}. */
    public int getAnimState()
    {
        return this.entityData.get(DATA_ANIM);
    }

    /** Whether the wake or POT animation is still playing. */
    public boolean isAnimating()
    {
        return this.getAnimState() != ANIM_NONE;
    }

    /** Whether this creepot grew in place of a trial chamber decorated pot. */
    public boolean isTrial()
    {
        return this.entityData.get(DATA_TRIAL);
    }

    /** Marks the creepot as a dormant pot or awake. */
    public void setSleeping(boolean sleeping)
    {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    /** Marks the creepot as neutral or hostile. */
    public void setHostile(boolean hostile)
    {
        this.entityData.set(DATA_HOSTILE, hostile);
    }

    private void setRolling(boolean rolling)
    {
        this.entityData.set(DATA_ROLLING, rolling);
    }

    /** Marks this creepot as a trial-chamber stand-in that drops the chamber pot's loot. */
    public void setTrial(boolean trial)
    {
        this.entityData.set(DATA_TRIAL, trial);
    }

    /** Whether a hostile target stands far enough (>16 blocks) to start rolling. */
    private boolean hasRollTarget()
    {
        LivingEntity target = this.getTarget();
        return target != null
            && target.isAlive()
            && !this.isSleeping()
            && !this.isAnimating()
            && this.distanceToSqr(target) > ROLL_DISTANCE_SQ;
    }

    /** Keeps rolling until the target is close enough (<=3 blocks) for the swell to take over. */
    private boolean keepsRolling()
    {
        LivingEntity target = this.getTarget();
        return target != null
            && target.isAlive()
            && !this.isSleeping()
            && !this.isAnimating()
            && this.distanceToSqr(target) > SWELL_RANGE_SQ;
    }

    // ------------------------------------------------------------- save/load

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_SLEEPING, this.isSleeping());
        tag.putBoolean(TAG_HOSTILE, this.isHostile());
        tag.putInt(TAG_ANIM, this.getAnimState());
        tag.putBoolean(TAG_POT_ROLLED, this.potRolled);
        tag.putBoolean(TAG_TRIAL, this.isTrial());
        Tag storedTag = this.storedItem.saveOptional(this.registryAccess());
        if (storedTag != null) {
            tag.put(TAG_STORED_ITEM, storedTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setSleeping(tag.getBoolean(TAG_SLEEPING));
        this.setHostile(tag.getBoolean(TAG_HOSTILE));
        int animState = tag.getInt(TAG_ANIM);
        this.setAnim(animState, animState == ANIM_NONE ? 0 : 1);
        this.potRolled = tag.getBoolean(TAG_POT_ROLLED);
        this.setTrial(tag.getBoolean(TAG_TRIAL));
        this.storedItem = ItemStack.parseOptional(this.registryAccess(), tag.getCompound(TAG_STORED_ITEM));
    }

    // ------------------------------------------------------------- extra loot

    /**
     * The standard drops - gunpowder, bricks and sherds - never touch the storage slot; the slot's
     * item drops alongside them when the creepot is defeated.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (!this.storedItem.isEmpty()) {
            this.spawnAtLocation(this.storedItem);
            this.storedItem = ItemStack.EMPTY;
        }
    }

    /**
     * Rolls the trial chamber corridor pot's loot once, at spawn, straight into the storage slot -
     * a trial-chamber creepot is born holding whatever its decorated pot would have held.
     */
    public void fillTrialLoot()
    {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LootParams lootParams = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withOptionalParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.CHEST);
        List<ItemStack> stacks = serverLevel.getServer()
            .reloadableRegistries()
            .getLootTable(TRIAL_CHAMBER_POT_LOOT)
            .getRandomItems(lootParams, this.getLootTableSeed());
        if (!stacks.isEmpty()) {
            this.storedItem = stacks.get(0);
        }
    }

    // ------------------------------------------------------------- rolling

    /**
     * Rolls after a hostile target instead of walking: it starts once the target is farther than
     * {@value Creepot#ROLL_DISTANCE} blocks and keeps rolling until the target is close enough
     * (3 blocks) for the swell to take over - it never stops just because the target crossed back
     * under 16 blocks. A blocked roll keeps rolling (and hops a single-block step the moment it
     * bumps one); a roll that stays blocked for {@value Creepot#ROLL_STUCK_TICKS} ticks stops,
     * re-checks sight on the target and drops a lost one so the target goal re-acquires it. The
     * distance is re-checked every tick so a dodge that sends the target far away resumes the roll.
     */
    /**
     * Finds players only while they are in sight; once found, a rolling creepot keeps its target
     * even when the line of sight closes, so a long roll chase never stalls. The moment the roll
     * stops, the roll goal re-checks sight itself and drops a lost target, which makes this goal
     * re-acquire it.
     */
    private static class CreepotTargetGoal extends NearestAttackableTargetGoal<Player>
    {
        private final Creepot creepot;

        private CreepotTargetGoal(Creepot creepot)
        {
            super(creepot, Player.class, 10, true, false, player -> creepot.isHostile());
            this.creepot = creepot;
        }

        @Override
        public boolean canContinueToUse()
        {
            return this.creepot.isRolling() || super.canContinueToUse();
        }
    }

    private static class CreepotRollGoal extends Goal
    {
        private final Creepot creepot;

        private CreepotRollGoal(Creepot creepot)
        {
            this.creepot = creepot;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse()
        {
            return this.creepot.hasRollTarget();
        }

        @Override
        public boolean canContinueToUse()
        {
            if (!this.creepot.keepsRolling()) {
                return false;
            }
            if (this.creepot.rollStuckTicks > ROLL_STUCK_TICKS) {
                return false;
            }
            // A roll that has run out of path while the target is still far away is blocked:
            // count the blocked ticks so an obstacle that can neither be jumped nor routed around
            // eventually stops the roll and re-checks sight on the target.
            if (this.creepot.getNavigation().isDone()) {
                this.creepot.rollStuckTicks++;
            }
            return true;
        }

        @Override
        public void start()
        {
            this.creepot.rollStuckTicks = 0;
            this.moveToTarget();
        }

        @Override
        public void tick()
        {
            this.moveToTarget();
        }

        @Override
        public void stop()
        {
            this.creepot.setRolling(false);
            this.creepot.getNavigation().stop();
            this.creepot.rollStuckTicks = 0;
            // Whatever stopped the roll, sight decides whether the chase continues: a target that
            // can no longer be seen is dropped so the target goal re-acquires it; a visible one is
            // kept and the next jump-requiring spot is jumped through even though the distance
            // calls for rolling.
            LivingEntity target = this.creepot.getTarget();
            if (target != null && !this.creepot.getSensing().hasLineOfSight(target)) {
                this.creepot.setTarget(null);
            }
        }

        private void moveToTarget()
        {
            LivingEntity target = this.creepot.getTarget();
            if (target != null) {
                boolean pathing = this.creepot.getNavigation().moveTo(target, ROLL_SPEED_MODIFIER);
                if (pathing || !this.creepot.getNavigation().isDone()) {
                    this.creepot.rollStuckTicks = 0;
                }
                this.creepot.setRolling(true);
            }
        }
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
     * Picks the animation: sleep while dormant, the one-shot wake and POT animations while they
     * play, scroll while rolling, and walk or idle otherwise.
     */
    private PlayState animationPredicate(AnimationState<Creepot> state)
    {
        if (this.isSleeping()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SLEEP));
            return PlayState.CONTINUE;
        }
        if (this.getAnimState() == ANIM_WAKE) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(ANIMATION_WAKE));
            return PlayState.CONTINUE;
        }
        if (this.getAnimState() == ANIM_POT) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(ANIMATION_POT));
            return PlayState.CONTINUE;
        }
        if (this.isRolling()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SCROLL));
            return PlayState.CONTINUE;
        }
        String animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
