package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.item.UnderwaterPrimedTnt;
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

    public static final DeferredHolder<Item, Item> CREEPOP_SPAWN_EGG = ITEMS.register(
        "creepop_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.CREEPOP::get,
            0xFFFFFF,
            0xFFFFFF,
            new Item.Properties()
        )
    );

    public static final DeferredHolder<Item, Item> UNDERWATER_TNT = ITEMS.register(
        "underwater_tnt",
        () -> new BlockItem(ModBlocks.UNDERWATER_TNT.get(), new Item.Properties())
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
