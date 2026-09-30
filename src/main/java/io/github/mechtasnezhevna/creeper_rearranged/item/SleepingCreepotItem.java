package io.github.mechtasnezhevna.creeper_rearranged.item;

import io.github.mechtasnezhevna.creeper_rearranged.entity.creepot.Creepot;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The sleeping creepot item dug out of suspicious sand and suspicious gravel. Using it on the
 * ground places a sleeping creepot entity - still dormant, exactly like the pot it was buried in -
 * which wakes on a right click or an attack.
 */
public class SleepingCreepotItem extends Item
{
    public SleepingCreepotItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BlockState below = level.getBlockState(pos.below());
        if (!level.getBlockState(pos).isAir() || !below.isSolid()) {
            return InteractionResult.FAIL;
        }
        Creepot creepot = new Creepot(ModEntities.CREEPOT.get(), level);
        creepot.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0.0F);
        creepot.setPersistenceRequired();
        level.addFreshEntity(creepot);
        level.playSound(null, pos, SoundEvents.DECORATED_POT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemStack stack = context.getItemInHand();
        if (!(context.getPlayer() instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}