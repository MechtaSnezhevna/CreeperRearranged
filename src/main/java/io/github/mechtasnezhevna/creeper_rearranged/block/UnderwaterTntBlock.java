package io.github.mechtasnezhevna.creeper_rearranged.block;

import com.mojang.serialization.MapCodec;
import io.github.mechtasnezhevna.creeper_rearranged.entity.item.UnderwaterPrimedTnt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A retextured copy of vanilla TNT whose blast ignores water.
 *
 * <p>NeoForge routes every ignition path - redstone, flint and steel, fire charges, burning
 * projectiles, fire spreading onto the block and dispensers loaded with flint and steel - through
 * {@link TntBlock#onCaughtFire(BlockState, Level, BlockPos, Direction, LivingEntity)}, so overriding
 * that single hook is enough for all of them to prime {@link UnderwaterPrimedTnt} instead of a
 * vanilla primed TNT. Everything else (the unstable state, "no drop when exploded" and the short
 * random fuse of a TNT that is destroyed by another blast) is inherited unchanged.
 */
public class UnderwaterTntBlock extends TntBlock
{
    /** Same codec shape as vanilla TNT, which this block mirrors in every state. */
    public static final MapCodec<TntBlock> CODEC = simpleCodec(UnderwaterTntBlock::new);

    /** {@code FireBlock.bootStrap} uses {@code setFlammable(Blocks.TNT, 15, 100)}. */
    private static final int FIRE_SPREAD_SPEED = 15;
    private static final int FLAMMABILITY = 100;

    public UnderwaterTntBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public MapCodec<TntBlock> codec()
    {
        return CODEC;
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter)
    {
        prime(level, pos, igniter);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion)
    {
        if (!level.isClientSide) {
            UnderwaterPrimedTnt primedTnt = new UnderwaterPrimedTnt(
                level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, explosion.getIndirectSourceEntity()
            );
            int fuse = primedTnt.getFuse();
            primedTnt.setFuse(level.random.nextInt(fuse / 4) + fuse / 8);
            level.addFreshEntity(primedTnt);
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return FIRE_SPREAD_SPEED;
    }

    /**
     * Primes the block's position with an underwater primed TNT, mirroring vanilla's
     * {@code TntBlock#explode}: sound, game event and a fresh 80 tick fuse.
     */
    public static void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter)
    {
        if (!level.isClientSide) {
            UnderwaterPrimedTnt primedTnt = new UnderwaterPrimedTnt(
                level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, igniter
            );
            level.addFreshEntity(primedTnt);
            level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        }
    }
}
