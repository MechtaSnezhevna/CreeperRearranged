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
 * {@code animations/entity/withper.animation.json}. The two side heads are hidden outright in
 * phase two and shown in phase one, decided and applied every frame so the single-headed look
 * holds even during animation crossfades and the three-headed look holds from the very first
 * rendered frame of a summon.
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
        // Set the head visibility explicitly every frame, both ways: phase two hides the side
        // heads, phase one shows them. Never early-return, so no leftover scale from a previous
        // frame, animation or entity can leave the boss briefly one-headed at spawn.
        this.getBone(BONE_HEAD_LEFT).ifPresent(bone -> setBoneScale(bone, animatable.isPhaseTwo() ? 0.0F : 1.0F));
        this.getBone(BONE_HEAD_RIGHT).ifPresent(bone -> setBoneScale(bone, animatable.isPhaseTwo() ? 0.0F : 1.0F));
    }

    private static void setBoneScale(GeoBone bone, float scale)
    {
        bone.setScaleX(scale);
        bone.setScaleY(scale);
        bone.setScaleZ(scale);
    }
}
