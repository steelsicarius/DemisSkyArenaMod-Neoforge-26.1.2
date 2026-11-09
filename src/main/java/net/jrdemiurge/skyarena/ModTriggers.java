package net.jrdemiurge.skyarena;

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
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 26.1.2 port: CriteriaTriggers.register can no longer be called at mod
 * construction time (the registry is frozen); the triggers are registered
 * through the standard NeoForge registry events instead.
 */
public class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, SkyArena.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, UseAltarBattle> USE_ALTAR_BATTLE =
            TRIGGERS.register("use_altar_battle", () -> UseAltarBattle.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, UseMusicDisk> USE_MUSIC_DISK =
            TRIGGERS.register("use_music_disk", () -> UseMusicDisk.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, UseStick> USE_STICK =
            TRIGGERS.register("use_stick", () -> UseStick.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, UseNetheriteIngot> USE_NETHERITE_INGOT =
            TRIGGERS.register("use_netherite_ingot", () -> UseNetheriteIngot.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel1> DIFFICULTY_LEVEL_1 =
            TRIGGERS.register("difficulty_level_1", () -> DifficultyLevel1.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel5> DIFFICULTY_LEVEL_5 =
            TRIGGERS.register("difficulty_level_5", () -> DifficultyLevel5.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel10> DIFFICULTY_LEVEL_10 =
            TRIGGERS.register("difficulty_level_10", () -> DifficultyLevel10.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel20> DIFFICULTY_LEVEL_20 =
            TRIGGERS.register("difficulty_level_20", () -> DifficultyLevel20.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel50> DIFFICULTY_LEVEL_50 =
            TRIGGERS.register("difficulty_level_50", () -> DifficultyLevel50.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, DifficultyLevel100> DIFFICULTY_LEVEL_100 =
            TRIGGERS.register("difficulty_level_100", () -> DifficultyLevel100.INSTANCE);

    public static void register(IEventBus eventBus) {
        TRIGGERS.register(eventBus);
    }
}
