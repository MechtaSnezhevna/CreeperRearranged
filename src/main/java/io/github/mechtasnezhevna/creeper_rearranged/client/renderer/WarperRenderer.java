package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.WarperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.Warper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the warper. Extending {@link VariantCreeperRenderer} provides the vanilla
 * fuse swelling and white flash, so a warper looks exactly like a swelling vanilla creeper.
 */
@OnlyIn(Dist.CLIENT)
public class WarperRenderer extends VariantCreeperRenderer<Warper>
{
    public WarperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new WarperGeoModel());
    }
}
