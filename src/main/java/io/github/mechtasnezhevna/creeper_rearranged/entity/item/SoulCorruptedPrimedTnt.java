package io.github.mechtasnezhevna.creeper_rearranged.entity.item;

import io.github.mechtasnezhevna.creeper_rearranged.registry.ModBlocks;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * The primed entity of the soul corrupted TNT.
 *
 * <p>Every independent behaviour of vanilla TNT is inherited: 0.98 x 0.98 hitbox, fire immunity,
 * gravity, an 80 tick fuse and the four block explosion of {@link Level.ExplosionInteraction#TNT}.
 * Only the withering of blast victims is added, and that happens outside this class - the
 * explosion's damage source carries this entity as its direct entity, so
 * {@code SoulTntWithering} can recognise the blast through {@code LivingDamageEvent.Pre}.
 *
 * <p>{@link PrimedTnt#explode()} is protected in NeoForge, so this class needs no mixin.
 */
public class SoulCorruptedPrimedTnt extends PrimedTnt
{
    /** Same "a TNT that travelled through a nether portal does not blow up portals" rule as vanilla. */
    private static final ExplosionDamageCalculator USED_PORTAL_DAMAGE_CALCULATOR = new ExplosionDamageCalculator()
    {
        @Override
        public Optional<Float> getBlockExplosionResistance(
            Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid
        )
        {
            return state.is(Blocks.NETHER_PORTAL)
                ? Optional.empty()
                : super.getBlockExplosionResistance(explosion, level, pos, state, fluid);
        }

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power)
        {
            return state.is(Blocks.NETHER_PORTAL) ? false : super.shouldBlockExplode(explosion, level, pos, state, power);
        }
    };

    /**
     * Vanilla exposes its owner only through {@link #getOwner()}, and the five argument constructor
     * hard-codes {@link EntityType#TNT}, so the owner is tracked here instead.
     */
    @Nullable
    private LivingEntity owner;
    /** Counterpart of {@code PrimedTnt#usedPortal}, whose setter is private. */
    private boolean travelledThroughPortal;

    public SoulCorruptedPrimedTnt(EntityType<? extends SoulCorruptedPrimedTnt> entityType, Level level)
    {
        super(entityType, level);
    }

    public SoulCorruptedPrimedTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner)
    {
        this(ModEntities.SOUL_CORRUPTED_TNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2F, -Math.cos(angle) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = owner;
        // Makes the flashing entity render the soul corrupted TNT texture instead of vanilla TNT.
        this.setBlockState(ModBlocks.SOUL_CORRUPTED_TNT.get().defaultBlockState());
    }

    @Override
    protected void explode()
    {
        this.level()
            .explode(
                this,
                Explosion.getDefaultDamageSource(this.level(), this),
                this.travelledThroughPortal ? USED_PORTAL_DAMAGE_CALCULATOR : null,
                this.getX(),
                this.getY(0.0625),
                this.getZ(),
                4.0F,
                false,
                Level.ExplosionInteraction.TNT
            );
    }

    @Nullable
    @Override
    public LivingEntity getOwner()
    {
        return this.owner;
    }

    @Override
    public void restoreFrom(Entity entity)
    {
        super.restoreFrom(entity);
        if (entity instanceof SoulCorruptedPrimedTnt primedTnt) {
            this.owner = primedTnt.owner;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition transition)
    {
        Entity entity = super.changeDimension(transition);
        if (entity instanceof SoulCorruptedPrimedTnt primedTnt) {
            primedTnt.travelledThroughPortal = true;
        }

        return entity;
    }
}
