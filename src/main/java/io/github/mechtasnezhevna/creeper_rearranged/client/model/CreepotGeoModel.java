package io.github.mechtasnezhevna.creeper_rearranged.client.model;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepot.Creepot;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model wrapper for the creepot.
 *
 * <p>The default entity asset paths are resolved automatically:
 * {@code geo/entity/creepot.geo.json}, {@code textures/entity/creepot.png} and
 * {@code animations/entity/creepot.animation.json}. The sleep, wake, POT and scroll states are all
 * animations on the same model, so one texture covers every state.
 */
public class CreepotGeoModel extends DefaultedEntityGeoModel<Creepot>
{
    /** Body bone from {@code geo/entity/creepot.geo.json}, sunk into the pot while asleep. */
    private static final String BONE_BODY = "body";
    /** Pot head bone from {@code geo/entity/creepot.geo.json}, hidden while asleep. */
    private static final String BONE_POT_HEAD = "POThead";

    public CreepotGeoModel()
    {
        super(ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "creepot"));
    }

    /**
     * A dormant creepot must look asleep from the very first frame it appears. GeckoLib crossfades
     * every new animation from the model's default standing pose, so without this the spawn frame
     * flashes the pot-head pose before the sleep animation takes over. Forcing the sleep pose on
     * the bones while the creepot sleeps makes the very first frame - and every sleep frame -
     * already the dormant pot.
     */
    @Override
    public void setCustomAnimations(Creepot animatable, long instanceId, AnimationState<Creepot> state)
    {
        super.setCustomAnimations(animatable, instanceId, state);
        if (!animatable.isSleeping()) {
            return;
        }
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
