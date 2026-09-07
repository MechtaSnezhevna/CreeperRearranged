package io.github.mechtasnezhevna.creeper_rearranged.client.renderer;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Renders the honeeper with the vanilla creeper model but its own texture.
 * Swap in a custom model/texture here later without touching the entity logic.
 */
@OnlyIn(Dist.CLIENT)
public class HoneeperRenderer extends CreeperRenderer
{
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "textures/entity/honeeper/honeeper.png");

    public HoneeperRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper entity)
    {
        return TEXTURE;
    }
}
