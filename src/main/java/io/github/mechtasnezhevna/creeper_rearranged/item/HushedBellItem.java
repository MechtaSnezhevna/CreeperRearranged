package io.github.mechtasnezhevna.creeper_rearranged.item;

import net.minecraft.world.item.Item;

/**
 * The hushed bell - a small instrument that swallows vibration signals.
 *
 * <p>A creature holding one in either hand makes no vibration that wardens or sculk sensors can
 * perceive: each absorbed vibration signal costs one durability point (respecting Unbreaking),
 * and when the {@value #MAX_DURABILITY} durability runs out the bell breaks and disappears. The
 * enchantability value lets the enchanting table offer Unbreaking, Mending and the Curse of
 * Vanishing; the supported-items tags gate which enchantments are actually applicable.
 */
public class HushedBellItem extends Item
{
    /** The bell's full durability. */
    public static final int MAX_DURABILITY = 256;

    public HushedBellItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getEnchantmentValue()
    {
        return 10;
    }
}
