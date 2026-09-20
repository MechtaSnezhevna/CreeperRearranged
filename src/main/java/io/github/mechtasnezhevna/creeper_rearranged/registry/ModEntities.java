package io.github.mechtasnezhevna.creeper_rearranged.registry;

import io.github.mechtasnezhevna.creeper_rearranged.CreeperRearranged;
import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import io.github.mechtasnezhevna.creeper_rearranged.entity.crimper.Crimper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.endper.Endper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.item.UnderwaterPrimedTnt;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.Warper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for the mod's entity types: the creeper variants and the underwater TNT's primed entity.
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
            .clientTrackingRange(8)
            .build("warper")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<Cherreeper>> CHERREEPER = ENTITY_TYPES.register(
        "cherreeper",
        () -> EntityType.Builder.<Cherreeper>of(Cherreeper::new, MobCategory.MONSTER)
            .sized(0.7F, 1.7F)
            .eyeHeight(1.3F)
            .clientTrackingRange(8)
            .build("cherreeper")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<Creepop>> CREEPOP = ENTITY_TYPES.register(
        "creepop",
        () -> EntityType.Builder.<Creepop>of(Creepop::new, MobCategory.MONSTER)
            .sized(0.9F, 1.1F)
            .eyeHeight(0.55F)
            .clientTrackingRange(8)
            .build("creepop")
    );

    /**
     * The primed entity of the underwater TNT. Its builder mirrors vanilla's {@code EntityType.TNT}
     * so the blast has the same reach, hitbox and client tracking.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<UnderwaterPrimedTnt>> UNDERWATER_TNT = ENTITY_TYPES.register(
        "underwater_tnt",
        () -> EntityType.Builder.<UnderwaterPrimedTnt>of(UnderwaterPrimedTnt::new, MobCategory.MISC)
            .fireImmune()
            .sized(0.98F, 0.98F)
            .eyeHeight(0.15F)
            .clientTrackingRange(10)
            .updateInterval(10)
            .build("underwater_tnt")
    );

    private ModEntities()
    {
    }
}
