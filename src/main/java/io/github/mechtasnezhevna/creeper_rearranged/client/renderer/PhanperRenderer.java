package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.PhanperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the phanper. The idle / stuck animations are chosen by the entity itself,
 * and the shadow radius matches the vanilla phantom's 0.75.
 */
@OnlyIn(Dist.CLIENT)
public class PhanperRenderer extends GeoEntityRenderer<Phanper>
{
    public PhanperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new PhanperGeoModel());
        this.shadowRadius = 0.75F;
    }
}
