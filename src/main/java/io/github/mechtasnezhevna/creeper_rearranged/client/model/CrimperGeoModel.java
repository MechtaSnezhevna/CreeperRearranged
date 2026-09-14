package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.crimper.Crimper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the crimper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/crimper.geo.json}, {@code textures/entity/crimper.png} and
 * {@code animations/entity/crimper.animation.json}.
 */
public class CrimperGeoModel extends DefaultedEntityGeoModel<Crimper>
{
    public CrimperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "crimper"));
    }
}
