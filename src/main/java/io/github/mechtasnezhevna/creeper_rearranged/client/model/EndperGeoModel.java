package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.config.ClientConfig;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the endper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/endper.geo.json}, {@code textures/entity/endper.png} and
 * {@code animations/entity/endper.animation.json}.
 *
 * <p>While the "slimmer models" client option is on, the model and animation are swapped for the
 * {@code endper_slimmer} pair shipped alongside them: same texture, same animation keys, but the
 * chest and rump cubes are gone and the pearl stays. Both resources are picked per call, so
 * toggling the option needs no restart.
 */
public class EndperGeoModel extends DefaultedEntityGeoModel<Endper>
{
    /** Id of the alternate asset set; the path builders expand it to the {@code geo/} and {@code animations/} files. */
    private static final ResourceLocation SLIMMER_ASSETS =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "endper_slimmer");

    public EndperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "endper"));
    }

    @Override
    public ResourceLocation getModelResource(Endper animatable)
    {
        return ClientConfig.slimmerModels() ? buildFormattedModelPath(SLIMMER_ASSETS) : super.getModelResource(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(Endper animatable)
    {
        return ClientConfig.slimmerModels() ? buildFormattedAnimationPath(SLIMMER_ASSETS) : super.getAnimationResource(animatable);
    }
}
