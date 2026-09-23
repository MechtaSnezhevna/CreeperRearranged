package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.CreepalerGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepaler.Creepaler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the creepaler. Extending {@link VariantCreeperRenderer} provides the vanilla
 * swelling, fuse white flash and the creeper's explosion-scale curve.
 */
@OnlyIn(Dist.CLIENT)
public class CreepalerRenderer extends VariantCreeperRenderer<Creepaler>
{
    public CreepalerRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CreepalerGeoModel());
    }
}
