package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.HoneeperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the honeeper. The model, texture and animation files are resolved by
 * {@link HoneeperGeoModel}; no vanilla {@code CreeperRenderer} is used anymore.
 */
@OnlyIn(Dist.CLIENT)
public class HoneeperRenderer extends GeoEntityRenderer<Honeeper>
{
    public HoneeperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new HoneeperGeoModel());
    }
}
