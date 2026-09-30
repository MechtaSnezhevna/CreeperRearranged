package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.WiskelperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.wiskelper.Wiskelper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the wiskelper. Extending {@link VariantCreeperRenderer} provides the
 * vanilla fuse swelling and white flash, so a wiskelper looks exactly like a swelling creeper.
 */
@OnlyIn(Dist.CLIENT)
public class WiskelperRenderer extends VariantCreeperRenderer<Wiskelper>
{
    public WiskelperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new WiskelperGeoModel());
    }
}
