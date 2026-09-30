package io.github.mechtasnezhevna.creeper_rearranged.mixin;

import io.github.mechtasnezhevna.creeper_rearranged.event.CreeperHooks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Grows one in ten trial chamber decorated pots into a dormant creepot.
 *
 * <p>NeoForge has no structure-placement event, so a mixin redirects the {@code processBlockInfos}
 * call inside {@link StructureTemplate#placeInWorld}. Pots whose loot table lives under
 * {@code minecraft:pots/trial_chambers/} are swapped for air with a {@value #CREEPOT_REPLACES_TRIAL_POT_CHANCE}
 * chance and remembered in {@link CreeperHooks}; the level tick drains the queue and spawns the
 * sleeping creepot as soon as the chunk is actually ticking.
 */
@Mixin(StructureTemplate.class)
public abstract class CreepotStructureMixin
{
    /** Chance that a trial chamber pot becomes a sleeping creepot instead. */
    private static final float CREEPOT_REPLACES_TRIAL_POT_CHANCE = 0.1F;
    /** NBT key of the loot table a randomizable block entity carries. */
    private static final String TAG_LOOT_TABLE = "LootTable";

    private CreepotStructureMixin()
    {
    }

    @Redirect(
        method = "placeInWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;"
        )
    )
    private static List<StructureTemplate.StructureBlockInfo> creeperRearranged$replaceTrialChamberPots(
        ServerLevelAccessor levelAccessor,
        BlockPos origin,
        BlockPos pivot,
        StructurePlaceSettings settings,
        List<StructureTemplate.StructureBlockInfo> infos,
        StructureTemplate template
    )
    {
        List<StructureTemplate.StructureBlockInfo> processed =
            StructureTemplate.processBlockInfos(levelAccessor, origin, pivot, settings, infos, template);
        if (!(levelAccessor.getLevel() instanceof ServerLevel serverLevel)) {
            return processed;
        }
        RandomSource random = RandomSource.create();
        List<StructureTemplate.StructureBlockInfo> replaced = new ArrayList<>(processed.size());
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (info.state().is(Blocks.DECORATED_POT)
                && isTrialChamberPot(info)
                && random.nextFloat() < CREEPOT_REPLACES_TRIAL_POT_CHANCE) {
                CreeperHooks.queueCreepotSpawn(serverLevel, info.pos());
                replaced.add(new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.AIR.defaultBlockState(), null));
            } else {
                replaced.add(info);
            }
        }
        return replaced;
    }

    /** Whether this decorated pot carries one of the trial chamber pot loot tables. */
    private static boolean isTrialChamberPot(StructureTemplate.StructureBlockInfo info)
    {
        CompoundTag nbt = info.nbt();
        return nbt != null
            && nbt.contains(TAG_LOOT_TABLE, 8)
            && nbt.getString(TAG_LOOT_TABLE).startsWith("minecraft:pots/trial_chambers/");
    }
}
