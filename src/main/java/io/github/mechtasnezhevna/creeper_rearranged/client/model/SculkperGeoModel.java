package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.sculkper.Sculkper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the sculkper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/sculkper.geo.json}, {@code textures/entity/sculkper.png} and
 * {@code animations/entity/sculkper.animation.json}.
 */
public class SculkperGeoModel extends DefaultedEntityGeoModel<Sculkper>
{
    public SculkperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "sculkper"));
    }
}
