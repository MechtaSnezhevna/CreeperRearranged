package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.client.model.CherreeperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.config.ClientConfig;
import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * GeckoLib renderer for the cherreeper. Extending {@link VariantCreeperRenderer} provides the
 * vanilla fuse swelling and white flash. A cherreeper named {@code color} (case-insensitive) is
 * rendered with the {@code cherreeper_color} texture instead of the default one.
 *
 * <p>While the "slimmer models" client option is on, the chest cube is skipped entirely, so the
 * flower face on its chest is not drawn; the animation keeps driving the hidden bone, which needs
 * no special handling. The skip applies to the white flash overlay too, because GeckoLib's
 * {@code reRender} routes through the same {@link #renderRecursively} hook.
 */
@OnlyIn(Dist.CLIENT)
public class CherreeperRenderer extends VariantCreeperRenderer<Cherreeper>
{
    /** Bone holding the chest cube that the slimmer option culls. */
    private static final String CHEST_BONE = "chest";

    public CherreeperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new CherreeperGeoModel());
    }

    @Override
    public void renderRecursively(
        PoseStack poseStack,
        Cherreeper animatable,
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
    public ResourceLocation getTextureLocation(Cherreeper cherreeper)
    {
        if (cherreeper.hasCustomName() && Cherreeper.COLOR_TEXTURE_NAME.equalsIgnoreCase(cherreeper.getCustomName().getString())) {
            return ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "textures/entity/cherreeper_color.png");
        }
        return super.getTextureLocation(cherreeper);
    }
}
