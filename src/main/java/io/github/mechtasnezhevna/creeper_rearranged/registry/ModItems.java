package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> ENDPER_SPAWN_EGG = ITEMS.register(
        "endper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.ENDPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> CRIMPER_SPAWN_EGG = ITEMS.register(
        "crimper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CRIMPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> WARPED_SHROOMLIGHT = ITEMS.register(
        "warped_shroomlight",
        () -> new BlockItem(ModBlocks.WARPED_SHROOMLIGHT.get(), new Item.Properties())
    );

    private ModItems()
    {
    }
}
