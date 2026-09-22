package io.github.mechtasnezhevna.creeper_rearranged.client.jade;

import io.github.mechtasnezhevna.creeper_rearranged.entity.honeeper.Honeeper;
import io.github.mechtasnezhevna.creeper_rearranged.entity.creepop.Creepop;
import io.github.mechtasnezhevna.creeper_rearranged.entity.cherreeper.Cherreeper;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin entry point, auto-discovered by Jade on the client. Only client registration is
 * needed: every displayed value (honeeper honey level, creepop air time, cherreeper owner) is
 * synced entity data.
 */
@WailaPlugin
public final class HoneeperJadePlugin implements IWailaPlugin
{
    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerEntityComponent(HoneeperHoneyLevelProvider.INSTANCE, Honeeper.class);
        registration.registerEntityComponent(CreepopAirTimeProvider.INSTANCE, Creepop.class);
        registration.registerEntityComponent(CherreeperOwnerProvider.INSTANCE, Cherreeper.class);
    }
}
