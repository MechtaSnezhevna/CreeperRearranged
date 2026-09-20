package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.CreepopGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GeckoLib renderer for the creepop. Extending {@link VariantCreeperRenderer} provides the vanilla
 * swelling, and its harmless out-of-water burst opts out of the white flash through
 * {@code Creepop#showsFuseWhiteFlash()}.
 */
@OnlyIn(Dist.CLIENT)
public class CreepopRenderer extends VariantCreeperRenderer<Creepop>
{
    public CreepopRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CreepopGeoModel());
    }
}
