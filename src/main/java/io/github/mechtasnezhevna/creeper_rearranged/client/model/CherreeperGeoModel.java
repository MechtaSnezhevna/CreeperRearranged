package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the cherreeper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/cherreeper.geo.json}, {@code textures/entity/cherreeper.png} and
 * {@code animations/entity/cherreeper.animation.json}. The renderer swaps in
 * {@code cherreeper_color.png} for cherreeper named {@code color}.
 */
public class CherreeperGeoModel extends DefaultedEntityGeoModel<Cherreeper>
{
    public CherreeperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "cherreeper"));
    }
}
