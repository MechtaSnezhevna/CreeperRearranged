package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the creepop.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/creepop.geo.json}, {@code textures/entity/creepop.png} and
 * {@code animations/entity/creepop.animation.json}.
 */
public class CreepopGeoModel extends DefaultedEntityGeoModel<Creepop>
{
    public CreepopGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "creepop"));
    }
}
