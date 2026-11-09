package net.jrdemiurge.skyarena;

import com.mojang.logging.LogUtils;
import net.jrdemiurge.skyarena.block.ModBlocks;
import net.jrdemiurge.skyarena.block.entity.ModBlockEntity;
import net.jrdemiurge.skyarena.gametest.SkyArenaGameTests;
import net.jrdemiurge.skyarena.item.ModCreativeTabs;
import net.jrdemiurge.skyarena.item.ModItems;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel1;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel10;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel100;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel20;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel5;
import net.jrdemiurge.skyarena.triggers.DifficultyLevel50;
import net.jrdemiurge.skyarena.triggers.UseAltarBattle;
import net.jrdemiurge.skyarena.triggers.UseMusicDisk;
import net.jrdemiurge.skyarena.triggers.UseNetheriteIngot;
import net.jrdemiurge.skyarena.triggers.UseStick;
import net.minecraft.advancements.CriteriaTriggers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SkyArena.MOD_ID)
public class SkyArena {
    public static final String MOD_ID = "skyarena";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkyArena(IEventBus modEventBus, ModContainer modContainer) {
        ModCreativeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntity.register(modEventBus);

        // 26.1.2 port: the trigger_type registry is frozen at bootstrap; entries
        // go through a DeferredRegister so NeoForge registers them in RegisterEvent.
        ModTriggers.register(modEventBus);

        // 26.1.2 port: gametest functions (runGameTestServer) — prueba funcional
        // automatizada del altar (spawn de oleadas, proteccion, despawn).
        SkyArenaGameTests.register(modEventBus);

        // 26.1.2 port: event handlers use @EventBusSubscriber auto-registration;
        // the game bus rejects register() calls on objects without @SubscribeEvent.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

}
