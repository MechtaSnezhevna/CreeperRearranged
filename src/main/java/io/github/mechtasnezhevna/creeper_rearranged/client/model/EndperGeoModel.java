package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the endper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/endper.geo.json}, {@code textures/entity/endper.png} and
 * {@code animations/entity/endper.animation.json}.
 */
public class EndperGeoModel extends DefaultedEntityGeoModel<Endper>
{
    public EndperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "endper"));
    }
}
