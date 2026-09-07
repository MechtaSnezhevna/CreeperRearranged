package io.github.mechtasnezhevna.creeper_rearranged.client;

import io.github.mechtasnezhevna.creeper_rearranged.client.renderer.HoneeperRenderer;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

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
        event.registerEntityRenderer(ModEntities.HONEEPER.get(), HoneeperRenderer::new);
    }
}
