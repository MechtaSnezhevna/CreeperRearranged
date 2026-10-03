package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.WithperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.withper.Withper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the withper. Extending {@link VariantCreeperRenderer} provides the
 * swelling inflation and the white fuse flash, driven by the charge progress the entity reports
 * through {@link Withper#getSwelling(float)}.
 */
@OnlyIn(Dist.CLIENT)
public class WithperRenderer extends VariantCreeperRenderer<Withper>
{
    public WithperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new WithperGeoModel());
    }
}
