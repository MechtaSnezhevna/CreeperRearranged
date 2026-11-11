package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModItems;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.VanillaGameEvent;

/**
 * The hushed bell's passive effect: whenever a living creature holding a bell performs an action
 * that would emit a vibration, the {@link VanillaGameEvent} is cancelled before any game event
 * listener - sculk sensor, warden or anything else - can hear it, and the bell loses one point of
 * durability. The absorption happens once per vibration; when the bell's durability is exhausted
 * it breaks and disappears.
 */
public final class HushedBellVibration
{
    private HushedBellVibration()
    {
    }

    public static void onVanillaGameEvent(VanillaGameEvent event)
    {
        Entity cause = event.getCause();
        if (!(cause instanceof LivingEntity living) || !event.getVanillaEvent().is(GameEventTags.VIBRATIONS)) {
            return;
        }
        ItemStack bell = findHushedBell(living);
        if (bell.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        bell.hurtAndBreak(1, living, living.getEquipmentSlotForItem(bell));
    }

    /** The bell in the main or off hand, if the creature is holding one. */
    private static ItemStack findHushedBell(LivingEntity living)
    {
        ItemStack mainHand = living.getMainHandItem();
        if (mainHand.is(ModItems.HUSHED_BELL.get())) {
            return mainHand;
        }
        ItemStack offHand = living.getOffhandItem();
        if (offHand.is(ModItems.HUSHED_BELL.get())) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }
}
