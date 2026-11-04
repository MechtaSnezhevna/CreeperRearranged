package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mechtasnezhevna.creeper_rearranged.client.model.WithperGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.withper.Withper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * GeckoLib renderer for the withper. Extending {@link VariantCreeperRenderer} provides the
 * swelling inflation and the white fuse flash, driven by the charge progress the entity reports
 * through {@link Withper#getSwelling(float)}. The white flash is overridden to wash out smoothly
 * across the whole charge instead of flickering like a short fuse, and a wither-style energy-shield
 * aura (the same one the wither shows while it wakes up and when it drops below half health) is
 * drawn while the boss wakes and while it is locked in its 150-HP phase transition.
 */
@OnlyIn(Dist.CLIENT)
public class WithperRenderer extends VariantCreeperRenderer<Withper>
{
    /** The wither's blue swirling shield, shown while the boss wakes up or locks in the transition. */
    private static final ResourceLocation WITHER_ARMOR_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");

    public WithperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new WithperGeoModel());
        this.addRenderLayer(new WithperArmorLayer());
    }

    /**
     * The withper turns white smoothly across the whole charge - matching its full duration - so
     * a 15-second charge takes the full 15 seconds to reach pure white instead of cycling white,
     * colourless, white; at the moment of detonation it is fully white.
     */
    @Override
    protected float whiteFlashProgress(float swelling)
    {
        return Mth.clamp(swelling, 0.0F, 1.0F);
    }

    /**
     * Port of the wither's energy-swirl shield ({@code WitherArmorLayer}): the same scrolling
     * {@code wither_armor} texture with the same offset curves. The wither shows it while it wakes
     * up, fading in across the summon, and at full strength while powered below half health; the
     * withper shows it while waking (fading in like the wither) and at full strength while the
     * phase transition locks it. The vanilla layer deflected projectiles through the wither's
     * powered state; the withper is fully locked while transitioning, so the shield is purely
     * cosmetic here.
     */
    private final class WithperArmorLayer extends GeoRenderLayer<Withper>
    {
        private WithperArmorLayer()
        {
            super(WithperRenderer.this);
        }

        @Override
        public void render(
            PoseStack poseStack,
            Withper animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
        )
        {
            float intensity;
            if (animatable.isTransitioning()) {
                intensity = 1.0F;
            } else if (animatable.isWaking()) {
                intensity = animatable.getWakeProgress();
            } else {
                return;
            }
            if (intensity <= 0.0F) {
                return;
            }
            float f = animatable.tickCount + partialTick;
            float xOffset = Mth.cos(f * 0.02F) * 3.0F;
            RenderType energyRenderType = RenderType.energySwirl(WITHER_ARMOR_TEXTURE, xOffset % 1.0F, f * 0.01F % 1.0F);
            getRenderer().reRender(
                getDefaultBakedModel(animatable),
                poseStack,
                bufferSource,
                animatable,
                energyRenderType,
                bufferSource.getBuffer(energyRenderType),
                partialTick,
                packedLight,
                packedOverlay,
                Color.ofRGBA(255, 255, 255, Math.round(intensity * 255.0F)).argbInt()
            );
        }
    }
}
