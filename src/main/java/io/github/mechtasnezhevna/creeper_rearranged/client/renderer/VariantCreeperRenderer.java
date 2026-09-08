package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mechtasnezhevna.creeper_rearranged.entity.VariantCreeper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * Shared GeckoLib renderer base for every creeper variant.
 *
 * <p>Vanilla creepers owe their fuse look to two renderer-side effects driven by the entity's own
 * swelling data: {@code CreeperRenderer.scale()} inflates the model and
 * {@code CreeperRenderer#getWhiteOverlayProgress()} flashes it white. Variant models use the
 * GeckoLib pipeline instead of the vanilla model renderer, so those effects are recreated here:
 * the swelling transform (identical formulas, anchored at the feet) and a white flash overlay
 * layer. Because the timing comes from the entity's own swelling data, every future variant
 * inherits vanilla-identical fuse behaviour by simply extending this class.
 */
@OnlyIn(Dist.CLIENT)
public abstract class VariantCreeperRenderer<T extends VariantCreeper & GeoAnimatable> extends GeoEntityRenderer<T>
{
    /** Fully opaque white texture used to draw the fuse flash without shipping extra assets. */
    private static final ResourceLocation WHITE_FLASH_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");

    protected VariantCreeperRenderer(EntityRendererProvider.Context context, GeoModel<T> model)
    {
        super(context, model);
        this.addRenderLayer(new WhiteFlashLayer());
    }

    /**
     * Port of the vanilla {@code CreeperRenderer.scale()} swelling curve: the model inflates
     * horizontally (up to 1.4x) and grows slightly taller while the fuse burns, with the classic
     * high-frequency wobble. The transform is applied around the feet, like the vanilla renderer.
     */
    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
    {
        float swelling = entity.getSwelling(partialTick);
        float wobble = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        float progress = Mth.clamp(swelling, 0.0F, 1.0F);
        progress *= progress;
        progress *= progress;
        float scaleXZ = (1.0F + progress * 0.4F) * wobble;
        float scaleY = (1.0F + progress * 0.1F) / wobble;

        poseStack.pushPose();
        poseStack.scale(scaleXZ, scaleY, scaleXZ);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    /**
     * White overlay pass replicating the vanilla fuse flash. The flash starts once the swelling
     * fraction passes 0.1 and then alternates every 0.1 with a strength that ramps 0.5 to 1, so the
     * model is progressively washed out instead of flashing on and off at full strength.
     */
    private final class WhiteFlashLayer extends GeoRenderLayer<T>
    {
        private WhiteFlashLayer()
        {
            super(VariantCreeperRenderer.this);
        }

        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay)
        {
            float progress = whiteFlashProgress(animatable.getSwelling(partialTick));
            if (progress <= 0.0F) {
                return;
            }
            RenderType whiteRenderType = RenderType.entityTranslucent(WHITE_FLASH_TEXTURE);
            getRenderer().reRender(
                getDefaultBakedModel(animatable),
                poseStack,
                bufferSource,
                animatable,
                whiteRenderType,
                bufferSource.getBuffer(whiteRenderType),
                partialTick,
                packedLight,
                packedOverlay,
                Color.ofRGBA(255, 255, 255, Math.round(progress * 255.0F)).argbInt()
            );
        }
    }

    /** Vanilla {@code CreeperRenderer#getWhiteOverlayProgress} flicker curve. */
    private static float whiteFlashProgress(float swelling)
    {
        return (int) (swelling * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swelling, 0.5F, 1.0F);
    }
}
