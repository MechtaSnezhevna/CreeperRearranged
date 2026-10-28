package io.github.mechtasnezhevna.creeper_rearranged.event;

import io.github.mechtasnezhevna.creeper_rearranged.block.CreeperSoulAttractGoal;
import io.github.mechtasnezhevna.creeper_rearranged.block.CreeperSoulRepelGoal;
import io.github.mechtasnezhevna.creeper_rearranged.entity.warper.EndermanApproachWarperGoal;
import io.github.mechtasnezhevna.creeper_rearranged.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class GoalInjector {

    /** Goal priority of the warper approach; the enderman's vanilla random stroll sits at 7. */
    private static final int WARPER_APPROACH_GOAL_PRIORITY = 6;
    /**
     * Endermen that already received {@link EndermanApproachWarperGoal}, so a mob that re-joins a
     * level does not collect duplicate goals. Weak keys let unloaded endermen be collected.
     */
    private static final Set<EnderMan> ENDERMEN_APPROACHING_WARPERS =
            Collections.newSetFromMap(new WeakHashMap<>());
    /** Goal priority of the soul-block attraction; beats the creeper's random stroll at 7. */
    private static final int SOUL_ATTRACT_GOAL_PRIORITY = 6;
    /** Goal priority of the soul-block repulsion; dominates idle goals but yields to combat. */
    private static final int SOUL_REPEL_GOAL_PRIORITY = 2;
    /** Cats, piglins and piglin brutes that the soul block drives away (its blacklist). */
    private static final Set<EntityType<?>> SOUL_REPEL_BLACKLIST = Set.of(
            EntityType.CAT, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE
    );
    /**
     * Creepers that already received {@link CreeperSoulAttractGoal}, so a mob that re-joins a
     * level does not collect duplicate goals. Weak keys let unloaded mobs be collected.
     */
    private static final Set<Mob> CREEPERS_ATTRACTED_BY_SOUL =
            Collections.newSetFromMap(new WeakHashMap<>());
    /**
     * Cats and piglins that already received {@link CreeperSoulRepelGoal}, guarded like the
     * attraction set above.
     */
    private static final Set<Mob> MOBS_REPELLED_BY_SOUL =
            Collections.newSetFromMap(new WeakHashMap<>());
    /** The creeper kinds the soul block attracts (its whitelist); see {@link #soulWhitelist()}. */
    private static Set<EntityType<?>> soulAttractWhitelist;


    /**
     * Teaches endermen to walk towards nearby warpers, and injects the soul block's two goals.
     *
     * <p>Fired for every entity that joins a level - freshly spawned ones as well as those read
     * back from disk - so mobs in existing worlds get the goals too. The warper goal outranks the
     * vanilla random stroll but not the combat goals, so an angry enderman keeps fighting instead
     * of wandering off to a warper. Whitelisted creepers get {@link CreeperSoulAttractGoal} and
     * blacklisted cats and piglins get {@link CreeperSoulRepelGoal}. No mixin is involved.
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof EnderMan enderman) {
            if (!ENDERMEN_APPROACHING_WARPERS.add(enderman)) {
                return;
            }
            enderman.goalSelector.addGoal(WARPER_APPROACH_GOAL_PRIORITY, new EndermanApproachWarperGoal(enderman));
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (soulWhitelist().contains(mob.getType())) {
            if (CREEPERS_ATTRACTED_BY_SOUL.add(mob)) {
                mob.goalSelector.addGoal(SOUL_ATTRACT_GOAL_PRIORITY, new CreeperSoulAttractGoal((Creeper)mob));
            }
        } else if (SOUL_REPEL_BLACKLIST.contains(mob.getType())) {
            if (MOBS_REPELLED_BY_SOUL.add(mob)) {
                mob.goalSelector.addGoal(SOUL_REPEL_GOAL_PRIORITY, new CreeperSoulRepelGoal(mob));
            }
        }
    }

    /**
     * The creeper kinds the soul block attracts: the vanilla creeper plus every variant that is a
     * real {@code Creeper}. The phanper is excluded - it is a flying mob, so the walking attract
     * goal cannot apply to it. Built lazily because {@code DeferredHolder#get()} must not run
     * before the registries are populated.
     */
    private static Set<EntityType<?>> soulWhitelist()
    {
        Set<EntityType<?>> whitelist = soulAttractWhitelist;
        if (whitelist == null) {
            whitelist = Set.of(
                    EntityType.CREEPER,
                    ModEntities.HONEEPER.get(),
                    ModEntities.ENDPER.get(),
                    ModEntities.CRIMPER.get(),
                    ModEntities.WARPER.get(),
                    ModEntities.CHERREEPER.get(),
                    ModEntities.CREEPOP.get(),
                    ModEntities.CREEPALER.get(),
                    ModEntities.CREEPOT.get()
            );
            soulAttractWhitelist = whitelist;
        }
        return whitelist;
    }
}
