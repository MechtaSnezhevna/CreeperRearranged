package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.item.SleepingCreepotItem;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

/**
 * GeckoLib item model for the sleeping creepot. The dormant pot reuses the creepot's own geo
 * model, texture and sleep animation, so the item looks exactly like the placed sleeping entity -
 * the body sunk into the pot and the pot head hidden while it dozes.
 */
@OnlyIn(Dist.CLIENT)
public class SleepingCreepotGeoModel extends DefaultedItemGeoModel<SleepingCreepotItem>
{
    /** Body bone from {@code geo/entity/creepot.geo.json}, sunk into the pot while asleep. */
    private static final String BONE_BODY = "body";
    /** Pot head bone from {@code geo/entity/creepot.geo.json}, hidden while asleep. */
    private static final String BONE_POT_HEAD = "POThead";

    public SleepingCreepotGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "sleeping_creepot"));
    }

    @Override
    public ResourceLocation getModelResource(SleepingCreepotItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "geo/entity/creepot.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SleepingCreepotItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "textures/entity/creepot.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SleepingCreepotItem animatable)
    {
        return ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "animations/entity/creepot.animation.json");
    }

    /**
     * A dormant creepot must look asleep from the very first frame it appears. GeckoLib crossfades
     * every new animation from the model's default standing pose, so without this the first render
     * flashes the pot-head pose before the sleep animation takes over - the same first-frame fix
     * the {@link CreepotGeoModel} applies to the placed entity.
     */
    @Override
    public void setCustomAnimations(SleepingCreepotItem animatable, long instanceId, AnimationState<SleepingCreepotItem> state)
    {
        super.setCustomAnimations(animatable, instanceId, state);
        Optional<GeoBone> body = this.getBone(BONE_BODY);
        body.ifPresent(bone -> bone.setPosY(-3.0F));
        Optional<GeoBone> potHead = this.getBone(BONE_POT_HEAD);
        potHead.ifPresent(bone -> {
            bone.setScaleX(0.0F);
            bone.setScaleY(0.0F);
            bone.setScaleZ(0.0F);
        });
    }
}
