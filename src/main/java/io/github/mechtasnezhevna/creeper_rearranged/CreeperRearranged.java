package io.github.mechtasnezhevna.creeper_rearranged;

import io.github.mechtasnezhevna.creeper_rearranged.client.ModClient;
import io.github.mechtasnezhevna.creeper_rearranged.event.CreeperHooks;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModCreativeTabs;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.monster.Creeper;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreeperRearranged.MODID)
public class CreeperRearranged
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "creeper_rearranged";
    public CreeperRearranged(IEventBus modEventBus, ModContainer modContainer)
    {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(CreeperRearranged::registerEntityAttributes);

        NeoForge.EVENT_BUS.addListener(CreeperHooks::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(CreeperHooks::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(CreeperHooks::onLivingDamage);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ModClient::registerRenderers);
        }
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ModEntities.HONEEPER.get(), Creeper.createAttributes().build());
    }
}
