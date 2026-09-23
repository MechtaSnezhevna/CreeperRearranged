package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mechtasnezhevna.creeper_rearranged.client.model.PhanperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.phanper.Phanper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the phanper. The flight animation is chosen by the entity itself,
 * and the shadow radius matches the vanilla phantom's 0.75. Like the vanilla phantom renderer the
 * body pitches with the entity's {@code xRot} so it faces its flight direction while climbing or
 * diving.
 */
@OnlyIn(Dist.CLIENT)
public class PhanperRenderer extends GeoEntityRenderer<Phanper>
{
    public PhanperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new PhanperGeoModel());
        this.shadowRadius = 0.75F;
    }

    @Override
    protected void applyRotations(
        Phanper animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale
    ) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
    }
}
