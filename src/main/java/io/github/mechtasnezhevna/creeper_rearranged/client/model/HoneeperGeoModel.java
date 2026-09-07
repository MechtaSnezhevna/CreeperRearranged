package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the honeeper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/honeeper.geo.json}, {@code textures/entity/honeeper.png} and
 * {@code animations/entity/honeeper.animation.json}.
 */
public class HoneeperGeoModel extends DefaultedEntityGeoModel<Honeeper>
{
    public HoneeperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "honeeper"));
    }
}
