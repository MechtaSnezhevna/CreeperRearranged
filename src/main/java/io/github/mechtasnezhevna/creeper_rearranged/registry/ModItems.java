package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.item.UnderwaterPrimedTnt;
import io.github.mechtasnezhevna.creeper_rearranged.item.SleepingCreepotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
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

    public static final DeferredHolder<Item, Item> WARPER_SPAWN_EGG = ITEMS.register(
        "warper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.WARPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> CHERREEPER_SPAWN_EGG = ITEMS.register(
        "cherreeper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CHERREEPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> CREEPOP_SPAWN_EGG = ITEMS.register(
        "creepop_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CREEPOP::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> PHANPER_SPAWN_EGG = ITEMS.register(
        "phanper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.PHANPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> CREEPALER_SPAWN_EGG = ITEMS.register(
        "creepaler_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CREEPALER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    /**
     * The archaeology item: brushing a suspicious sand or gravel block can dig this out, and using
     * it on the ground places a sleeping creepot.
     */
    public static final DeferredHolder<Item, Item> SLEEPING_CREEPOT = ITEMS.register(
        "sleeping_creepot",
        () -> new SleepingCreepotItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredHolder<Item, Item> CREEPOT_SPAWN_EGG = ITEMS.register(
        "creepot_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CREEPOT::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> WISKELPER_SPAWN_EGG = ITEMS.register(
        "wiskelper_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.WISKELPER::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> UNDERWATER_TNT = ITEMS.register(
        "underwater_tnt",
        () -> new BlockItem(ModBlocks.UNDERWATER_TNT.get(), new Item.Properties())
    );

    public static final DeferredHolder<Item, Item> BLOCK_OF_CREEPER_SOUL = ITEMS.register(
        "block_of_creeper_soul",
        () -> new BlockItem(ModBlocks.BLOCK_OF_CREEPER_SOUL.get(), new Item.Properties())
    );

    /**
     * Vanilla primes a TNT that a dispenser fires; without a behaviour of its own a dispenser would
     * only spit the block item out, so the underwater TNT copies that behaviour with its own entity.
     */
    public static void registerDispenseBehaviors()
    {
        DispenserBlock.registerBehavior(UNDERWATER_TNT.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack)
            {
                Level level = source.level();
                BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                UnderwaterPrimedTnt primedTnt = new UnderwaterPrimedTnt(
                    level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, null
                );
                level.addFreshEntity(primedTnt);
                level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.ENTITY_PLACE, pos);
                stack.shrink(1);
                return stack;
            }
        });
    }

    private ModItems()
    {
    }
}
