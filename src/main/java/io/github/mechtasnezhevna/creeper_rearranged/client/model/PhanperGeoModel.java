package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the phanper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/phanper.geo.json}, {@code textures/entity/phanper.png} and
 * {@code animations/entity/phanper.animation.json}.
 */
public class PhanperGeoModel extends DefaultedEntityGeoModel<Phanper>
{
    public PhanperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "phanper"));
    }
}
