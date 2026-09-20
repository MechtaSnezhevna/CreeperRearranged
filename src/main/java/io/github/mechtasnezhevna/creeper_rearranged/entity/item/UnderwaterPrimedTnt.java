package io.github.mechtasnezhevna.creeper_rearranged.entity.item;

import io.github.mechtasnezhevna.creeper_rearranged.entity.WaterTransparentExplosionDamageCalculator;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModBlocks;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * The primed entity of the underwater TNT.
 *
 * <p>Every independent behaviour of vanilla TNT is inherited: 0.98 x 0.98 hitbox, fire immunity,
 * gravity, an 80 tick fuse and the four block explosion of {@link Level.ExplosionInteraction#TNT}.
 * Only the {@link ExplosionDamageCalculator} differs: water is treated as if it was air, so a blast
 * under water keeps its full power instead of spending it on the 100 explosion resistance vanilla
 * fluids have. Lava and every other block keep vanilla behaviour.
 *
 * <p>{@link PrimedTnt#explode()} is protected in NeoForge, so this class needs no mixin.
 */
public class UnderwaterPrimedTnt extends PrimedTnt
{
    /**
     * Used while the TNT never entered a nether portal. Water costs no blast power, and a water block
     * itself is left alone instead of being removed.
     */
    private static final ExplosionDamageCalculator WATER_DAMAGE_CALCULATOR =
        new WaterTransparentExplosionDamageCalculator(false);

    /** Same, but keeps vanilla's "a TNT that used a portal does not blow up portals" rule. */
    private static final ExplosionDamageCalculator WATER_PORTAL_DAMAGE_CALCULATOR =
        new WaterTransparentExplosionDamageCalculator(true);

    /**
     * Vanilla exposes its owner only through {@link #getOwner()}, and the five argument constructor
     * hard-codes {@link EntityType#TNT}, so the owner is tracked here instead.
     */
    @Nullable
    private LivingEntity owner;
    /** Counterpart of {@code PrimedTnt#usedPortal}, whose setter is private. */
    private boolean travelledThroughPortal;

    public UnderwaterPrimedTnt(EntityType<? extends UnderwaterPrimedTnt> entityType, Level level)
    {
        super(entityType, level);
    }

    public UnderwaterPrimedTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner)
    {
        this(ModEntities.UNDERWATER_TNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2F, -Math.cos(angle) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = owner;
        // Makes the flashing entity render the underwater TNT texture instead of vanilla TNT.
        this.setBlockState(ModBlocks.UNDERWATER_TNT.get().defaultBlockState());
    }

    @Override
    protected void explode()
    {
        this.level()
            .explode(
                this,
                Explosion.getDefaultDamageSource(this.level(), this),
                this.travelledThroughPortal ? WATER_PORTAL_DAMAGE_CALCULATOR : WATER_DAMAGE_CALCULATOR,
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
        if (entity instanceof UnderwaterPrimedTnt primedTnt) {
            this.owner = primedTnt.owner;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition transition)
    {
        Entity entity = super.changeDimension(transition);
        if (entity instanceof UnderwaterPrimedTnt primedTnt) {
            primedTnt.travelledThroughPortal = true;
        }

        return entity;
    }
}
