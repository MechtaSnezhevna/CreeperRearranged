package io.github.mechtasnezhevna.creeper_rearranged.client.jade;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows the honeeper's worn-nest honey level (and the full-honey state) in the Jade tooltip.
 * The level is synced entity data, so the client can read it directly without server data.
 */
public final class HoneeperHoneyLevelProvider implements IEntityComponentProvider
{
    public static final HoneeperHoneyLevelProvider INSTANCE = new HoneeperHoneyLevelProvider();
    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "honey_level");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config)
    {
        if (accessor.getEntity() instanceof Honeeper honeeper) {
            tooltip.add(
                Component.translatable(
                    "creeper_rearranged.jade.honey_level",
                    honeeper.getHoneyLevel(),
                    Honeeper.MAX_HONEY_LEVEL
                )
            );
            if (honeeper.isFullHoney()) {
                tooltip.add(Component.translatable("creeper_rearranged.jade.honey_full"));
            }
        }
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    private HoneeperHoneyLevelProvider()
    {
    }
}
