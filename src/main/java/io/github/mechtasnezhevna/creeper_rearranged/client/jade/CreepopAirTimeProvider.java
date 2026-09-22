package io.github.mechtasnezhevna.creeper_rearranged.client.jade;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows how many seconds a creepop has left before its bubble bursts. The countdown only runs out
 * of water, so the line appears exactly then; the remaining ticks are synced entity data, so the
 * client reads them without a server round trip.
 */
public final class CreepopAirTimeProvider implements IEntityComponentProvider
{
    public static final CreepopAirTimeProvider INSTANCE = new CreepopAirTimeProvider();
    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath(CreeperRearranged.MODID, "air_time");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config)
    {
        if (accessor.getEntity() instanceof Creepop creepop && creepop.getAirTicks() < Creepop.AIR_LIFETIME_TICKS) {
            tooltip.add(
                Component.translatable(
                    "creeper_rearranged.jade.air_time",
                    String.format(Locale.ROOT, "%.1f", creepop.getAirTicks() / 20.0F)
                )
            );
        }
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    private CreepopAirTimeProvider()
    {
    }
}
