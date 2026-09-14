package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.crimper.Crimper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.Warper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for creeper variant entity types.
 *
 * <p>To add a new variant:
 * <ol>
 *   <li>Add a {@code DeferredHolder} here, mirroring {@link #HONEEPER}.</li>
 *   <li>Create the entity class (extend {@code VariantCreeper}).</li>
 *   <li>Register its attributes in {@code CreeperRearranged#registerEntityAttributes}.</li>
 *   <li>Register a renderer on the client ({@code ModClient}).</li>
 * </ol>
 */
public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, CreeperRearranged.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Honeeper>> HONEEPER = ENTITY_TYPES.register(
        "honeeper",
        () -> EntityType.Builder.<Honeeper>of(Honeeper::new, MobCategory.MONSTER)
            .sized(0.7F, 1.5F)
            .clientTrackingRange(8)
            .build("honeeper")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<Endper>> ENDPER = ENTITY_TYPES.register(
        "endper",
        () -> EntityType.Builder.<Endper>of(Endper::new, MobCategory.MONSTER)
            .sized(0.6F, 2.8F)
            .clientTrackingRange(8)
            .build("endper")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<Crimper>> CRIMPER = ENTITY_TYPES.register(
        "crimper",
        () -> EntityType.Builder.<Crimper>of(Crimper::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F)
            .eyeHeight(1.2F)
            .clientTrackingRange(8)
            .build("crimper")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<Warper>> WARPER = ENTITY_TYPES.register(
        "warper",
        () -> EntityType.Builder.<Warper>of(Warper::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F)
            //.eyeHeight(1.2F)
            .clientTrackingRange(8)
            .build("warper")
    );

    private ModEntities()
    {
    }
}
