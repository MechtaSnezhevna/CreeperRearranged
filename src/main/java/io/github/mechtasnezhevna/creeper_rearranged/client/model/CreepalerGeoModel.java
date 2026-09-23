package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepaler.Creepaler;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the creepaler.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/creepaler.geo.json}, {@code textures/entity/creepaler.png} and
 * {@code animations/entity/creepaler.animation.json}. The texture is picked per state: a sleeping
 * creepaler renders with the dark texture, an active one with the lit texture.
 */
public class CreepalerGeoModel extends DefaultedEntityGeoModel<Creepaler>
{
    private static final ResourceLocation ACTIVE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "textures/entity/creepaler.png");
    private static final ResourceLocation INACTIVE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "textures/entity/dark_creepaler.png");

    public CreepalerGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "creepaler"));
    }

    @Override
    public ResourceLocation getTextureResource(Creepaler animatable)
    {
        return animatable.isActive() ? ACTIVE_TEXTURE : INACTIVE_TEXTURE;
    }
}
