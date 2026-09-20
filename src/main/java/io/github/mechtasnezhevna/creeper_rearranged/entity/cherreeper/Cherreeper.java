package io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
 * when the cherreeper is hurt. While calm it randomly alternates between standing and sitting, each
 * state lasting a few seconds. It cannot breed and its blast is a plain vanilla creeper explosion.
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
        this.goalSelector.addGoal(3, new SittingAwareMeleeAttackGoal());
        this.goalSelector.addGoal(4, new SittingAwareRandomStrollGoal());
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
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
        this.playSound(SoundEvents.HONEY_DRINK, 1.0F, 1.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.HEART,
                this.getX(), this.getY() + 1.0, this.getZ(),
                7, 0.4, 0.4, 0.4, 0.1
            );
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

    // -------------------------------------------------------------- syncing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_TAMED, false);
        builder.define(DATA_SITTING, false);
        builder.define(DATA_MANUAL_SIT, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_TAMED, this.isTamed());
        tag.putBoolean(TAG_SITTING, this.isSitting());
        tag.putBoolean(TAG_MANUAL_SIT, this.isManualSitting());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.setTamed(tag.getBoolean(TAG_TAMED));
        this.setSitting(tag.getBoolean(TAG_SITTING));
        this.setManualSitting(tag.getBoolean(TAG_MANUAL_SIT));
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
