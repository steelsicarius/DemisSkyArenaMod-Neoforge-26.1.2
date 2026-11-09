package net.jrdemiurge.skyarena.event;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.entity.AltarBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 26.1.2 port: block entities no longer get automatic ticks through the block
 * class, so the altar entities are ticked from this server tick listener.
 */
@EventBusSubscriber(modid = SkyArena.MOD_ID)
public class AltarTicker {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AltarBlockEntity.tickAll();
    }
}
