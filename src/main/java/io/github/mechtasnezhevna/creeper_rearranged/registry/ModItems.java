package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, CreeperRearranged.MODID);

    public static final DeferredHolder<Item, Item> HONEEPER_SPAWN_EGG = ITEMS.register(
        "honeeper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.HONEEPER::get,
            0xF2C14E,
            0x395A22,
            new Item.Properties()
        )
    );

    private ModItems()
    {
    }
}
