package io.github.mechtasnezhevna.creeper_rearranged.client;

import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.CherreeperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.CrimperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.CreepopRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.EndperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.HoneeperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.PhanperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.WarperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.config.ClientConfig;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only registration, wired from the mod constructor on the client distribution.
 */
public final class ModClient
{
    private ModClient()
    {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.UNDERWATER_TNT.get(), TntRenderer::new);
        event.registerEntityRenderer(ModEntities.HONEEPER.get(), HoneeperRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDPER.get(), EndperRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMPER.get(), CrimperRenderer::new);
        event.registerEntityRenderer(ModEntities.WARPER.get(), WarperRenderer::new);
        event.registerEntityRenderer(ModEntities.CREEPOP.get(), CreepopRenderer::new);
        event.registerEntityRenderer(ModEntities.CHERREEPER.get(), CherreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANPER.get(), PhanperRenderer::new);
    }

    /**
     * Registers the client config together with NeoForge's generic config screen. The screen factory
     * is what makes the mod list's "Config" button work - NeoForge has no default for it.
     */
    public static void registerConfig(ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
