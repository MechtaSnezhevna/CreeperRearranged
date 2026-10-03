package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.withper.Withper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the withper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/withper.geo.json}, {@code textures/entity/withper.png} and
 * {@code animations/entity/withper.animation.json}. Once the boss enters phase two its two side
 * heads are hidden outright, so the single-headed look holds even during animation crossfades.
 */
public class WithperGeoModel extends DefaultedEntityGeoModel<Withper>
{
    /** Side heads from {@code geo/entity/withper.geo.json}, hidden in phase two. */
    private static final String BONE_HEAD_LEFT = "head1";
    private static final String BONE_HEAD_RIGHT = "head2";

    public WithperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "withper"));
    }

    @Override
    public void setCustomAnimations(Withper animatable, long instanceId, AnimationState<Withper> state)
    {
        super.setCustomAnimations(animatable, instanceId, state);
        if (!animatable.isPhaseTwo()) {
            return;
        }
        this.getBone(BONE_HEAD_LEFT).ifPresent(bone -> hideBone(bone));
        this.getBone(BONE_HEAD_RIGHT).ifPresent(bone -> hideBone(bone));
    }

    private static void hideBone(GeoBone bone)
    {
        bone.setScaleX(0.0F);
        bone.setScaleY(0.0F);
        bone.setScaleZ(0.0F);
    }
}
