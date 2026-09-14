package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.Warper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the warper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/warper.geo.json}, {@code textures/entity/warper.png} and
 * {@code animations/entity/warper.animation.json}.
 */
public class WarperGeoModel extends DefaultedEntityGeoModel<Warper>
{
    public WarperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "warper"));
    }
}
