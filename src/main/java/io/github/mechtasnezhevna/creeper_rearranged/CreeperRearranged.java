package io.github.mechtasnezhevna.creeper_rearranged;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreeperRearranged.MODID)
public class CreeperRearranged
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "creeper_rearranged";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreeperRearranged(IEventBus modEventBus, ModContainer modContainer)
    {

    }
}
