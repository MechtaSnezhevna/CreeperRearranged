package io.github.mechtasnezhevna.creeper_rearranged.entity.creepop;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.WaterTransparentExplosionDamageCalculator;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Creepop - a creeper variant that drifts through the ocean inside a bubble of TNT.
 *
 * <p>It lives under water: it breathes there (the {@code minecraft:can_breathe_under_water} entity
 * type tag), swims with an amphibious navigator and never takes drowning damage. Its blast is
 * computed by {@link WaterTransparentExplosionDamageCalculator}, so it keeps the full power of a
 * vanilla creeper blast under water instead of having the water swallow it - the water blocks
 * themselves are left alone.
 *
 * <p>Its bubble is only punctured by pointed weapons: a sword, a trident (the tag
 * {@code creeper_rearranged:creepop_poppers} by default) or an arrow / thrown trident pops it
 * harmlessly and drops the underwater TNT it was carrying. Every other melee attack on it is
 * shrugged off (no damage at all) and lights the bubble's real fuse instead, so the blast that
 * answers it comes with the vanilla swelling animation and white flash.
 *
 * <p>Lured out of the water it is harmless: it cannot light its real fuse, loses the ability to
 * chase anything and drifts through the air like a soap bubble in the wind. After
 * {@value #AIR_LIFETIME_TICKS} ticks (fifteen seconds) - the last {@value #BURST_DURATION_TICKS} of
 * which it spends swelling <em>without</em> the white flash - it bursts harmlessly: no blast, no
 * damage, only the underwater TNT drop. Touching water refills the whole fifteen seconds at once,
 * while rain or snow falling on the bubble pops it on the spot, even one bobbing on the surface.
 */
public class Creepop extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "creepop_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/creepop.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.creepop.idle";
    public static final String ANIMATION_MOVE = "animation.creepop.move";
    public static final String ANIMATION_FLOATING = "animation.creepop.floating";

    /** How long the harmless out-of-water burst swells before it pops, matching the vanilla fuse. */
    public static final int BURST_DURATION_TICKS = 30;
    /**
     * Ticks the fuse lit by a wrong-tool hit burns before the bubble detonates, matching the vanilla
     * fuse. This is what gives that blast a swelling animation and the white flash warning.
     */
    private static final int ATTACK_FUSE_TICKS = 30;
    /** Natural spawn chance: the same 1-in-40 roll the drowned uses, {@code random.nextInt(40) == 0}. */
    private static final int NATURAL_SPAWN_ROLL = 40;
    /** Underwater TNT dropped when the bubble is popped. */
    public static final int UNDERWATER_TNT_DROP_COUNT = 1;

    /**
     * Weapons that prick the bubble open. Ships as {@code #minecraft:swords} plus the trident, so a
     * modpack adding a spear of its own only has to append it to this tag.
     */
    public static final TagKey<Item> POPPING_WEAPONS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "creepop_poppers")
    );

    /**
     * How long a bubble that is out of water survives: fifteen seconds. Touching water refills it
     * completely; the last {@link #BURST_DURATION_TICKS} ticks are the swelling warning.
     */
    public static final int AIR_LIFETIME_TICKS = 20 * 15;
    private static final int ANIMATION_TRANSITION_TICKS = 5;

    /** A drift heading is kept for 40 to 99 ticks, which is what makes the direction so sticky. */
    private static final int DRIFT_HEADING_MIN_TICKS = 40;
    private static final int DRIFT_HEADING_RANDOM_TICKS = 60;
    /** Only this share of fresh headings also pushes the bubble up or down. */
    private static final float DRIFT_VERTICAL_CHANCE = 0.15F;
    /** Top speed of the air drift, in blocks per tick. */
    private static final double DRIFT_SPEED = 0.045;
    /** How fast the current velocity turns towards the heading; small means a lot of inertia. */
    private static final double DRIFT_STEERING = 0.015;

    private static final EntityDataAccessor<Boolean> DATA_BURSTING =
        SynchedEntityData.defineId(Creepop.class, EntityDataSerializers.BOOLEAN);
    /** Whether the real fuse lit by a melee hit is burning. */
    private static final EntityDataAccessor<Boolean> DATA_DETONATING =
        SynchedEntityData.defineId(Creepop.class, EntityDataSerializers.BOOLEAN);
    /**
     * Ticks of air left before the bubble bursts on its own. Synced so the Jade tooltip can show
     * the countdown on the client.
     */
    private static final EntityDataAccessor<Integer> DATA_AIR_TICKS =
        SynchedEntityData.defineId(Creepop.class, EntityDataSerializers.INT);

    /** Water costs this blast no power and water blocks are left untouched. */
    private static final ExplosionDamageCalculator WATER_DAMAGE_CALCULATOR =
        new WaterTransparentExplosionDamageCalculator(false);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    /** Harmless-burst counter, the mirror image of vanilla's private {@code Creeper#swell} field. */
    private int burst;
    private int oldBurst;
    /** Attack-lit fuse counter; the same idea as {@link #burst}, but this one ends in a blast. */
    private int fuse;
    private int oldFuse;
    /** Current air-drift velocity, and the heading it slowly steers towards. */
    private Vec3 drift = Vec3.ZERO;
    private Vec3 driftTarget = Vec3.ZERO;
    /** Ticks left before a fresh drift heading is rolled. */
    private int driftTicks;

    public Creepop(EntityType<? extends Creepop> entityType, Level level)
    {
        super(entityType, level);
        this.moveControl = new CreepopMoveControl(this);
    }

    /**
     * Swims and walks. The amphibious navigator can path through water <em>and</em> over land, which
     * is what lets a player lure a creepop out of the ocean by simply walking up the beach.
     */
    @Override
    protected PathNavigation createNavigation(Level level)
    {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_BURSTING, false);
        builder.define(DATA_DETONATING, false);
        builder.define(DATA_AIR_TICKS, AIR_LIFETIME_TICKS);
    }

    /**
     * Like a water mob, the bubble is not shoved around by flowing water. The fluid-type aware
     * overload is NeoForge's non-deprecated hook; the argument-less one is deprecated.
     */
    @Override
    public boolean isPushedByFluid(FluidType type)
    {
        return false;
    }

    /**
     * Out of water the bubble loses interest in everything: it can no longer chase - or even keep -
     * a target, so not only the target goals but also the revenge its {@code HurtByTargetGoal}
     * would take are dropped. In water the vanilla creeper behaviour is untouched.
     */
    @Override
    public void setTarget(LivingEntity target)
    {
        super.setTarget(this.isInWater() ? target : null);
    }

    /**
     * Out of water the bubble is not steered by its AI at all: the drift takes over, so the walk
     * input is thrown away and the bubble cannot follow a path or a player.
     */
    @Override
    public void travel(Vec3 input)
    {
        super.travel(this.isInWater() ? input : Vec3.ZERO);
    }

    @Override
    public void tick()
    {
        if (!this.level().isClientSide) {
            this.updateAirTimer();
        }

        this.oldBurst = this.burst;
        if (this.isBursting() && this.burst < BURST_DURATION_TICKS) {
            this.burst++;
        }

        this.oldFuse = this.fuse;
        if (this.isDetonating() && this.fuse < ATTACK_FUSE_TICKS) {
            this.fuse++;
        }

        super.tick();

        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        if (this.isExposedToPrecipitation()) {
            // Rain or snow punctures the bubble at once, even one bobbing on the surface.
            this.burstHarmlessly();
        } else if (this.isDetonating() && this.fuse >= ATTACK_FUSE_TICKS) {
            this.triggerVariantExplosion();
        } else if (this.getAirTicks() <= 0) {
            this.burstHarmlessly();
        } else if (!this.isInWater()) {
            this.applyAirDrift();
        }
    }

    /**
     * Runs the fifteen-second air timer. Water refills the bubble completely the moment it is
     * touched; the last {@link #BURST_DURATION_TICKS} ticks of air make it swell (without the white
     * flash) as the warning before it bursts. A lit real fuse freezes the timer, because that fuse
     * ends in a blast rather than in the harmless burst.
     */
    private void updateAirTimer()
    {
        if (this.isDeadOrDying()) {
            return;
        }
        if (this.isInWater()) {
            if (this.getAirTicks() != AIR_LIFETIME_TICKS) {
                this.setAirTicks(AIR_LIFETIME_TICKS);
            }
            this.drift = Vec3.ZERO;
            this.driftTicks = 0;
            if (this.isBursting()) {
                this.setBursting(false);
                this.burst = 0;
            }
            return;
        }
        if (this.isDetonating()) {
            return;
        }
        // A target picked up while it was still in water is dropped the moment it leaves it.
        this.setTarget(null);

        this.setAirTicks(Math.max(this.getAirTicks() - 1, 0));
        if (this.getAirTicks() <= BURST_DURATION_TICKS && !this.isBursting()) {
            this.setBursting(true);
            this.getNavigation().stop();
        }
    }

    /**
     * Floats the bubble through the air the way a soap bubble rides the wind: a heading is kept for
     * a long while and the velocity only creeps towards it, so the direction is hard to change.
     * Vertical headings are rare and weak. Writing the velocity outright also cancels gravity, which
     * is what keeps the bubble hovering instead of dropping like a stone.
     */
    private void applyAirDrift()
    {
        if (--this.driftTicks <= 0) {
            this.driftTicks = DRIFT_HEADING_MIN_TICKS + this.random.nextInt(DRIFT_HEADING_RANDOM_TICKS);
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double y = this.random.nextFloat() < DRIFT_VERTICAL_CHANCE
                ? (this.random.nextDouble() - 0.5) * 0.5
                : 0.0;
            this.driftTarget = new Vec3(Math.cos(angle), y, Math.sin(angle)).normalize().scale(DRIFT_SPEED);
        }

        this.drift = this.drift.scale(1.0 - DRIFT_STEERING).add(this.driftTarget.scale(DRIFT_STEERING));
        this.setDeltaMovement(this.drift);
    }

    /**
     * Whether rain or snow is falling straight onto the bubble. Vanilla's {@code Level#isRainingAt}
     * only accepts {@code RAIN}, so the check is repeated here with {@code SNOW} allowed as well.
     * Anything under water or under a block fails the sky test, so only a bubble that really is
     * exposed to the weather pops.
     */
    private boolean isExposedToPrecipitation()
    {
        Level level = this.level();
        if (!level.isRaining()) {
            return false;
        }
        BlockPos pos = this.blockPosition();
        if (!level.canSeeSky(pos)
            || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        }
        return level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.NONE;
    }

    /**
     * Out of water the bubble never lights its real fuse: vanilla's {@code Creeper#tick} would call
     * the private {@code explodeCreeper()} once the fuse burns out. Swallowing the positive swell
     * direction keeps that from happening while the harmless burst drives the swelling animation.
     */
    @Override
    public void setSwellDir(int direction)
    {
        if (direction > 0 && !this.isInWater()) {
            return;
        }
        super.setSwellDir(direction);
    }

    /** Both the harmless burst and the attack-lit fuse drive the swelling animation. */
    @Override
    public float getSwelling(float partialTick)
    {
        if (this.isDetonating()) {
            return Mth.lerp(partialTick, (float)this.oldFuse, (float)this.fuse) / (float)(ATTACK_FUSE_TICKS - 2);
        }
        if (this.isBursting()) {
            return Mth.lerp(partialTick, (float)this.oldBurst, (float)this.burst) / (float)(BURST_DURATION_TICKS - 2);
        }
        return super.getSwelling(partialTick);
    }

    /**
     * Only the harmless burst skips the white flash. A fuse that really does end in a blast - the
     * attack-lit one, or the vanilla fuse while it is in water - keeps the vanilla warning.
     */
    @Override
    public boolean showsFuseWhiteFlash()
    {
        return !this.isBursting();
    }

    /** Blasts through water, keeping a charged creepop's doubled radius. */
    @Override
    protected Explosion createVariantExplosion()
    {
        return this.level()
            .explode(
                this,
                Explosion.getDefaultDamageSource(this.level(), this),
                WATER_DAMAGE_CALCULATOR,
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getVariantExplosionRadius(),
                false,
                Level.ExplosionInteraction.MOB
            );
    }

    @Override
    protected float getVariantExplosionRadius()
    {
        return super.getVariantExplosionRadius() * (this.isPowered() ? 2.0F : 1.0F);
    }

    /**
     * Pointed weapons prick the bubble open and arrows do the same; every other melee attack cannot
     * really hurt it and lights the real fuse instead.
     */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        // Already popped or detonated (a second arrow can land in the same tick) - no second drop.
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (this.isInvulnerableTo(source) || this.isDeadOrDying()) {
            return super.hurt(source, amount);
        }

        if (source.getDirectEntity() instanceof AbstractArrow) {
            // Arrows, spectral arrows and thrown tridents all pierce the bubble.
            this.pop();
            return false;
        }

        // Only a real player swing counts: thorns retaliation also has the player as its direct
        // entity, but the player did not attack, so it must not set the bubble off.
        if (source.getDirectEntity() instanceof Player player && source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            if (player.getMainHandItem().is(POPPING_WEAPONS)) {
                this.pop();
            } else {
                this.igniteFuse();
            }
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * Lights the real fuse after a wrong-tool hit. The bubble neither takes damage nor blows up on
     * the spot: it swells, flashes white like a vanilla creeper and detonates once the fuse is out.
     * The harmless out-of-water burst is called off, and hitting it again does not restart the fuse.
     */
    private void igniteFuse()
    {
        if (this.isDetonating()) {
            return;
        }
        this.setBursting(false);
        this.burst = 0;
        this.fuse = 0;
        this.oldFuse = 0;
        this.setDetonating(true);
        this.getNavigation().stop();
    }

    /** The harmless end of a burst: no blast and no damage, only the drop. */
    private void burstHarmlessly()
    {
        this.setBursting(false);
        this.burst = 0;
        this.pop();
    }

    /** Pops the bubble open: bubble sound and particles, the carried TNT, and that is all. */
    private void pop()
    {
        Level level = this.level();
        level.playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
            this.getSoundSource(),
            2.0F,
            1.0F
        );
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getY(0.6), this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            this.spawnAtLocation(new ItemStack(ModItems.UNDERWATER_TNT.get(), UNDERWATER_TNT_DROP_COUNT));
        }
        this.discard();
    }

    private boolean isBursting()
    {
        return this.entityData.get(DATA_BURSTING);
    }

    private void setBursting(boolean bursting)
    {
        this.entityData.set(DATA_BURSTING, bursting);
    }

    /**
     * Ticks of air left before the bubble bursts on its own; {@value #AIR_LIFETIME_TICKS} whenever
     * it is sitting in water.
     */
    public int getAirTicks()
    {
        return this.entityData.get(DATA_AIR_TICKS);
    }

    private void setAirTicks(int ticks)
    {
        this.entityData.set(DATA_AIR_TICKS, ticks);
    }

    /** Whether the real fuse lit by a melee hit is burning. */
    private boolean isDetonating()
    {
        return this.entityData.get(DATA_DETONATING);
    }

    private void setDetonating(boolean detonating)
    {
        this.entityData.set(DATA_DETONATING, detonating);
    }

    /**
     * Whether the given position is a valid natural spawn for a creepop. This mirrors the drowned's
     * rule ({@code Drowned#checkDrownedSpawnRules}): it stands in water with water underneath, it is
     * dark enough, and then the same rare 1-in-40 roll, so oceans get creepops at the drowned's pace.
     */
    public static boolean checkCreepopSpawnRules(
        EntityType<Creepop> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
    )
    {
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER) && !MobSpawnType.isSpawner(spawnType)) {
            return false;
        }
        boolean darkEnough = level.getDifficulty() != Difficulty.PEACEFUL
            && (MobSpawnType.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random))
            && (MobSpawnType.isSpawner(spawnType) || level.getFluidState(pos).is(FluidTags.WATER));
        if (darkEnough && MobSpawnType.isSpawner(spawnType)) {
            return true;
        }
        return random.nextInt(NATURAL_SPAWN_ROLL) == 0 && isDeepEnoughToSpawn(level, pos) && darkEnough;
    }

    /**
     * The drowned's depth rule: the water has to lie at least five blocks below sea level. Sea level
     * is read off the level itself, since {@code LevelReader#getSeaLevel()} is deprecated.
     */
    private static boolean isDeepEnoughToSpawn(ServerLevelAccessor level, BlockPos pos)
    {
        return pos.getY() < level.getLevel().getSeaLevel() - 5;
    }

    /**
     * Water is not an obstruction for a bubble. {@code Mob}'s default refuses any position whose
     * bounding box contains liquid at all, which no spot under water can satisfy; the drowned
     * overrides this very method for this very reason. Without the override
     * {@code NaturalSpawner}'s final gate throws away every otherwise valid ocean position, so the
     * creepop never actually entered the world.
     */
    @Override
    public boolean checkSpawnObstruction(LevelReader level)
    {
        return level.isUnobstructed(this);
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
     * In water the bubble idles or swims like any other creeper variant; the tumbling
     * {@code floating} animation is what it does out of water, where it flops and drifts.
     */
    private PlayState animationPredicate(AnimationState<Creepop> state)
    {
        String animation;
        if (this.isInWater()) {
            animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        } else {
            animation = ANIMATION_FLOATING;
        }
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }

    /**
     * Swim control modelled on {@code Drowned.DrownedMoveControl} (which is package private in
     * vanilla): under water the bubble paddles towards the wanted position, on land the normal
     * walking control takes over so it can climb out of the ocean.
     */
    private static class CreepopMoveControl extends MoveControl
    {
        private final Creepop creepop;

        CreepopMoveControl(Creepop creepop)
        {
            super(creepop);
            this.creepop = creepop;
        }

        @Override
        public void tick()
        {
            if (!this.creepop.isInWater()) {
                if (!this.creepop.onGround()) {
                    this.creepop.setDeltaMovement(this.creepop.getDeltaMovement().add(0.0, -0.008, 0.0));
                }
                super.tick();
                return;
            }

            LivingEntity target = this.creepop.getTarget();
            if (target != null && target.getY() > this.creepop.getY()) {
                // Paddle upwards towards a target that is above it.
                this.creepop.setDeltaMovement(this.creepop.getDeltaMovement().add(0.0, 0.004, 0.0));
            }

            if (!this.hasWanted() || this.creepop.getNavigation().isDone()) {
                this.creepop.setSpeed(0.0F);
                return;
            }

            double dx = this.wantedX - this.creepop.getX();
            double dy = this.wantedY - this.creepop.getY();
            double dz = this.wantedZ - this.creepop.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dy /= distance;
            float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
            this.creepop.setYRot(this.rotlerp(this.creepop.getYRot(), yaw, 90.0F));
            this.creepop.yBodyRot = this.creepop.getYRot();
            float speed = (float)(this.speedModifier * this.creepop.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float lerped = Mth.lerp(0.125F, this.creepop.getSpeed(), speed);
            this.creepop.setSpeed(lerped);
            this.creepop.setDeltaMovement(
                this.creepop.getDeltaMovement().add(lerped * dx * 0.005, lerped * dy * 0.1, lerped * dz * 0.005)
            );
        }
    }
}
