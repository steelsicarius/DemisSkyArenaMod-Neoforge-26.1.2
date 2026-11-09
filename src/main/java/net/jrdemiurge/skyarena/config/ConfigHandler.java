package net.jrdemiurge.skyarena.config;

import net.jrdemiurge.skyarena.SkyArena;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@EventBusSubscriber(modid = SkyArena.MOD_ID)
public class ConfigHandler {
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        SkyArenaConfig.loadConfig();
    }
}
