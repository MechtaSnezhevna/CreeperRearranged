package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Injects the hushed bell into the loot tables that drop it: wardens carry a
 * {@value #WARDEN_DROP_CHANCE_PERCENT}% chance per kill, and every chest of an ancient city -
 * both the standard {@code chests/ancient_city} and the ice-box variant - has a
 * {@value #ANCIENT_CITY_CHEST_CHANCE_PERCENT}% chance to contain one.
 */
public final class HushedBellLoot
{
    private static final ResourceLocation WARDEN_LOOT = ResourceLocation.withDefaultNamespace("entities/warden");
    private static final ResourceLocation ANCIENT_CITY_LOOT = ResourceLocation.withDefaultNamespace("chests/ancient_city");
    private static final ResourceLocation ANCIENT_CITY_ICE_BOX_LOOT = ResourceLocation.withDefaultNamespace("chests/ancient_city_ice_box");

    private static final float WARDEN_DROP_CHANCE = 0.05F;
    private static final float WARDEN_DROP_CHANCE_PERCENT = 5.0F;
    private static final float ANCIENT_CITY_CHEST_CHANCE = 0.0078F;
    private static final float ANCIENT_CITY_CHEST_CHANCE_PERCENT = 0.78F;

    private HushedBellLoot()
    {
    }

    public static void onLootTableLoad(LootTableLoadEvent event)
    {
        ResourceLocation name = event.getName();
        if (name.equals(WARDEN_LOOT)) {
            event.getTable().addPool(hushedBellPool(WARDEN_DROP_CHANCE));
        } else if (name.equals(ANCIENT_CITY_LOOT) || name.equals(ANCIENT_CITY_ICE_BOX_LOOT)) {
            event.getTable().addPool(hushedBellPool(ANCIENT_CITY_CHEST_CHANCE));
        }
    }

    /** A single-roll pool that only rolls when the given chance succeeds. */
    private static LootPool hushedBellPool(float chance)
    {
        return LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(ModItems.HUSHED_BELL.get()))
            .when(LootItemRandomChanceCondition.randomChance(chance))
            .build();
    }
}
