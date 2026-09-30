package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.CreepotGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepot.Creepot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the creepot. Extending {@link VariantCreeperRenderer} provides the vanilla
 * swelling, fuse white flash and the creeper's explosion-scale curve for the moment it stands up to
 * blow.
 */
@OnlyIn(Dist.CLIENT)
public class CreepotRenderer extends VariantCreeperRenderer<Creepot>
{
    public CreepotRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CreepotGeoModel());
    }
}