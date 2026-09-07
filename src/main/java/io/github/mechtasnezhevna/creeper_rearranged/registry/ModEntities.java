package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
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

    private ModEntities()
    {
    }
}
