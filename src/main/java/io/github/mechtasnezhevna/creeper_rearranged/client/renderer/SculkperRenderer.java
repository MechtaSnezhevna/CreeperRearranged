package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mechtasnezhevna.creeper_rearranged.client.model.SculkperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.config.ClientConfig;
import io.github.mechtasnezhevna.creeper_rearranged.entity.sculkper.Sculkper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * GeckoLib renderer for the sculkper. Extending {@link VariantCreeperRenderer} provides the
 * swelling inflate driven by the sculkper's own 5-second fuse; the white flash is overridden to
 * wash out smoothly across the whole charge instead of flickering like a vanilla 1.5-second fuse.
 *
 * <p>While the "slimmer models" client option is on, the chest cube is skipped entirely, so the
 * glowing chest face is not drawn; the animation keeps driving the hidden bone, which needs no
 * special handling. The skip applies to the white flash overlay too, because GeckoLib's
 * {@code reRender} routes through the same {@link #renderRecursively} hook.
 */
@OnlyIn(Dist.CLIENT)
public class SculkperRenderer extends VariantCreeperRenderer<Sculkper>
{
    /** Bone holding the chest cube that the slimmer option culls. */
    private static final String CHEST_BONE = "chest";

    public SculkperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new SculkperGeoModel());
    }

    @Override
    public void renderRecursively(
        PoseStack poseStack,
        Sculkper animatable,
        GeoBone bone,
        RenderType renderType,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight,
        int packedOverlay,
        int renderColor
    ) {
        if (ClientConfig.slimmerModels() && CHEST_BONE.equals(bone.getName())) {
            return;
        }
        super.renderRecursively(
            poseStack, animatable, bone, renderType, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, renderColor
        );
    }

    @Override
    protected float whiteFlashProgress(float swelling)
    {
        return Mth.clamp(swelling, 0.0F, 1.0F);
    }
}
