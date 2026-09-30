package io.github.mechtasnezhevna.creeper_rearranged.item;

import io.github.mechtasnezhevna.creeper_rearranged.client.model.SleepingCreepotGeoModel;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepot.Creepot;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The sleeping creepot item dug out of suspicious sand and suspicious gravel. Using it on the
 * ground places a sleeping creepot entity - still dormant, exactly like the pot it was buried in -
 * which wakes on a right click or an attack.
 *
 * <p>As a {@link GeoItem}, the item renders with the creepot's own 3D model and texture instead of
 * a flat icon: the same {@code geo/entity/creepot.geo.json} model wearing
 * {@code textures/entity/creepot.png}, playing the sleep animation so the dormant pot breathes in
 * the inventory exactly like the placed entity.
 */
public class SleepingCreepotItem extends Item implements GeoItem
{
    /** Loop the dormancy animation so the item breathes like a placed sleeping creepot. */
    private static final String ANIMATION_SLEEP = "animation.creepot.sleep";
    private static final String ANIMATION_CONTROLLER = "sleeping_creepot";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public SleepingCreepotItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, ANIMATION_CONTROLLER, 0, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<SleepingCreepotItem> state)
    {
        state.getController().setAnimation(RawAnimation.begin().thenLoop(ANIMATION_SLEEP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return this.animatableCache;
    }

    /**
     * Supplies the client-side geo renderer that draws the 3D dormant pot in hands, the inventory
     * and item frames. This is only ever invoked on the client, by the render provider the cache
     * builds when the item is about to be drawn.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer)
    {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<SleepingCreepotItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer()
            {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(new SleepingCreepotGeoModel());
                }
                return this.renderer;
            }
        });
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
