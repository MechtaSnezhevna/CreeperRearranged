package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public class CreeperSpawnPlacements {
    /**
     * Registers how natural crimper and warper spawns are validated. Like the other Nether monsters
     * (blaze, magma cube) they spawn on the ground regardless of light level, which is what their
     * biomes need; the biome modifiers add them to those biomes' spawn lists.
     */
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event)
    {
        event.register(
                ModEntities.CRIMPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.WARPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        // Cherry grove spawns: like the nether monsters, any light level is fine, so the docile
        // cherreeper can be found while exploring the grove during the day as well as at night.
        event.register(
                ModEntities.CHERREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        // Ocean floor spawns, under water and in the dark exactly like the drowned's.
        event.register(
                ModEntities.CREEPOP.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Creepop::checkCreepopSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        // Deep dark spawns: like the other ground monsters, any light level is fine (the deep dark
        // is always dark anyway); the biome modifier keeps the spawn rate very low.
        event.register(
                ModEntities.SCULKPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
