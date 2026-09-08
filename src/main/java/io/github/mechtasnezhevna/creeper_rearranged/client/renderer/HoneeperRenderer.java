package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.HoneeperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the honeeper. The model, texture and animation files are resolved by
 * {@link HoneeperGeoModel}; no vanilla {@code CreeperRenderer} is used anymore. Extending
 * {@link VariantCreeperRenderer} provides the vanilla fuse swelling and white flash.
 */
@OnlyIn(Dist.CLIENT)
public class HoneeperRenderer extends VariantCreeperRenderer<Honeeper>
{
    public HoneeperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HoneeperGeoModel());
    }
}
