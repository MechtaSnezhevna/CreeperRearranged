package io.github.mechtasnezhevna.creeper_rearranged.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side configuration, stored in {@code config/creeper_rearranged-client.toml}.
 *
 * <p>These options only change how the client draws the mod's entities, so they are kept out of the
 * server-authoritative configs. Callers read the values through the accessors below instead of the
 * spec itself, which keeps the rendering code independent of the config plumbing.
 */
public final class ClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Culls redundant model faces on some creeper variants. The comment doubles as the fallback
     * tooltip of the config screen entry.
     */
    public static final ModConfigSpec.BooleanValue SLIMMER_MODELS = BUILDER
        .comment("When enabled, redundant model faces of some creeper variants are culled.")
        .define("slimmerModels", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig()
    {
    }

    /**
     * Whether redundant model faces should be culled. Read while rendering rather than cached in a
     * field, so toggling the option on the config screen applies immediately.
     */
    public static boolean slimmerModels()
    {
        return SLIMMER_MODELS.get();
    }
}
