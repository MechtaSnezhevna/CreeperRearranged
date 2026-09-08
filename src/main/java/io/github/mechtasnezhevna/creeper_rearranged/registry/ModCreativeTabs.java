package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperRearranged.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREEPER_REARRANGED = CREATIVE_TABS.register(
        "creeper_rearranged",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + CreeperRearranged.MODID))
            .icon(() -> new ItemStack(ModItems.HONEEPER_SPAWN_EGG.get()))
            .displayItems((parameters, output) -> {
            output.accept(ModItems.HONEEPER_SPAWN_EGG.get());
            output.accept(ModItems.ENDPER_SPAWN_EGG.get());
        })
            .build()
    );

    private ModCreativeTabs()
    {
    }
}
