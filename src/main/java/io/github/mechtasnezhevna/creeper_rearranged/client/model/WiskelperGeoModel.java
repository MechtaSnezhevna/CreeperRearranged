package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.wiskelper.Wiskelper;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the wiskelper.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/wiskelper.geo.json}, {@code textures/entity/wiskelper.png} and
 * {@code animations/entity/wiskelper.animation.json}.
 */
public class WiskelperGeoModel extends DefaultedEntityGeoModel<Wiskelper>
{
    public WiskelperGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "wiskelper"));
    }
}
