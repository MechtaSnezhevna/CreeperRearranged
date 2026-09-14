package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.CrimperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.crimper.Crimper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the crimper. Extending {@link VariantCreeperRenderer} provides the vanilla
 * fuse swelling and white flash, so a crimper looks exactly like a swelling vanilla creeper.
 */
@OnlyIn(Dist.CLIENT)
public class CrimperRenderer extends VariantCreeperRenderer<Crimper>
{
    public CrimperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CrimperGeoModel());
    }
}
