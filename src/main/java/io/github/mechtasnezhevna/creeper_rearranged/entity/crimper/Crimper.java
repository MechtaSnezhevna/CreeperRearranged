package io.github.mechtasnezhevna.creeper_rearranged.entity.crimper;

import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Crimper - a crimson-forest creeper variant overgrown with nether fungus.
 *
 * <p>Gameplay is identical to a vanilla creeper: same goals, fuse, blast radius and lightning
 * charge behaviour, all inherited unchanged from
 * {@link net.minecraft.world.entity.monster.Creeper}. The variant only replaces the model and
 * animations, and it naturally spawns in the crimson forest instead of the Overworld (see
 * {@code data/creeper_rearranged/neoforge/biome_modifier/crimper_crimson_forest.json}).
 */
public class Crimper extends VariantCreeper implements GeoEntity
{
    /** GeckoLib animation controller name. */
    public static final String ANIMATION_CONTROLLER = "crimper_controller";
    /** Animation keys from {@code assets/creeper_rearranged/animations/entity/crimper.animation.json}. */
    public static final String ANIMATION_IDLE = "animation.crimper.idle";
    public static final String ANIMATION_MOVE = "animation.crimper.move";

    private static final int ANIMATION_TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public Crimper(EntityType<? extends Crimper> entityType, Level level)
    {
        super(entityType, level);
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
     * Loops the idle animation while standing still and the walk animation while moving; the fungus
     * growths on the body are animated inside both loops.
     */
    private PlayState animationPredicate(AnimationState<Crimper> state)
    {
        String animation = state.isMoving() ? ANIMATION_MOVE : ANIMATION_IDLE;
        state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }
}
