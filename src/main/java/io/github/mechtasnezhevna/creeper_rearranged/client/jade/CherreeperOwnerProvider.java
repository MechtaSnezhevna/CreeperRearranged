package io.github.mechtasnezhevna.creeper_rearranged.client.jade;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows the name of the player who tamed a cherreeper in the Jade tooltip. The owner name is synced
 * entity data (like the honeeper's honey level), so the client reads it without server round trips.
 */
public final class CherreeperOwnerProvider implements IEntityComponentProvider
{
    public static final CherreeperOwnerProvider INSTANCE = new CherreeperOwnerProvider();
    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "owner");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config)
    {
        if (accessor.getEntity() instanceof Cherreeper cherreeper && cherreeper.isTamed()) {
            String ownerName = cherreeper.getOwnerName();
            if (!ownerName.isEmpty()) {
                tooltip.add(Component.translatable("creeper_rearranged.jade.owner", ownerName));
            }
        }
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    private CherreeperOwnerProvider()
    {
    }
}
