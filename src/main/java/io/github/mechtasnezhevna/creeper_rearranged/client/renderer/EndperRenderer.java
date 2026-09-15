package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mechtasnezhevna.creeper_rearranged.client.model.EndperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * GeckoLib renderer for the endper. Extending {@link VariantCreeperRenderer} provides the vanilla
 * fuse swelling and white flash; the pearl bone is skipped entirely once the endper spent its pearl
 * escaping, which needs no special animation branch (the code hides the bone instead).
 *
 * <p>The slimmer silhouette is not handled here at all: it is a separate asset set that
 * {@link EndperGeoModel} swaps in, so this renderer only has to deal with the pearl, which both
 * variants carry under the same bone name.
 */
@OnlyIn(Dist.CLIENT)
public class EndperRenderer extends VariantCreeperRenderer<Endper>
{
    private static final String PEARL_BONE = "pearl";

    public EndperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new EndperGeoModel());
    }

    @Override
    public void renderRecursively(
        PoseStack poseStack,
        Endper animatable,
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
        if (!animatable.hasPearl() && PEARL_BONE.equals(bone.getName())) {
            return;
        }
        super.renderRecursively(
            poseStack, animatable, bone, renderType, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, renderColor
        );
    }
}
