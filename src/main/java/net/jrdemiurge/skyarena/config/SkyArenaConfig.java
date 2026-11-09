package net.jrdemiurge.skyarena.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkyArenaConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("sky_arena.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static ModConfig configData;

    public static final ArenaConfig DEFAULT_ARENA = createDefaultArena();
    public static final List<String> DEFAULT_KEY = createDefaultKey();
    public static final TrophyConfig DEFAULT_TROPHY = createDefaultTrophy();


    public static void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            loadFromFile();
        } else {
            copyDefaultConfig();
        }

        if (SkyArenaConfig.configData == null) {
            Logger LOGGER = LogUtils.getLogger();
            LOGGER.error("SkyArenaConfig.configData is null! Config not loaded properly.");

            configData = new ModConfig();
            configData.arenas = Map.of("default", DEFAULT_ARENA);
            configData.keys = Map.of("default", DEFAULT_KEY);
            configData.trophies = Map.of("default", DEFAULT_TROPHY);
        }
    }

    private static void loadFromFile() {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            configData = GSON.fromJson(reader, ModConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyDefaultConfig() {
        try (InputStream in = SkyArenaConfig.class.getResourceAsStream("/assets/skyarena/config/sky_arena.json")) {
            if (in == null) {
                System.err.println("Mod config resource not found!");
                return;
            }

            Files.copy(in, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            loadFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArenaConfig createDefaultArena() {
        return new ArenaConfig();
    }

    private static List<String> createDefaultKey() {
        List<String> defaultLootTables = List.of(
                "minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/ancient_city",
                "minecraft:chests/ancient_city_ice_box",
                "minecraft:chests/bastion_bridge",
                "minecraft:chests/bastion_hoglin_stable",
                "minecraft:chests/bastion_other",
                "minecraft:chests/bastion_treasure",
                "minecraft:chests/buried_treasure",
                "minecraft:chests/desert_pyramid",
                "minecraft:chests/end_city_treasure",
                "minecraft:chests/igloo_chest",
                "minecraft:chests/jungle_temple",
                "minecraft:chests/nether_bridge",
                "minecraft:chests/pillager_outpost",
                "minecraft:chests/ruined_portal",
                "minecraft:chests/shipwreck_map",
                "minecraft:chests/shipwreck_supply",
                "minecraft:chests/shipwreck_treasure",
                "minecraft:chests/simple_dungeon",
                "minecraft:chests/stronghold_library",
                "minecraft:chests/underwater_ruin_big",
                "minecraft:chests/underwater_ruin_small",
                "minecraft:chests/village/village_temple",
                "minecraft:chests/woodland_mansion",
                "minecraft:chests/chest_level_2",
                "minecraft:chests/chest_level_3",
                "minecraft:chests/firewell_d",
                "minecraft:chests/shater",
                "iceandfire:chest/fire_dragon_female_cave",
                "iceandfire:chest/fire_dragon_male_cave",
                "iceandfire:chest/ice_dragon_female_cave",
                "iceandfire:chest/ice_dragon_male_cave",
                "iceandfire:chest/lightning_dragon_female_cave",
                "iceandfire:chest/lightning_dragon_male_cave",
                "iceandfire:chest/mausoleum_chest"
        );
        return defaultLootTables;
    }

    private static TrophyConfig createDefaultTrophy() {
        TrophyConfig defaultTrophy = new TrophyConfig();
        defaultTrophy.cooldown = 0;

        defaultTrophy.effects = new HashMap<>();

        TrophyConfig.EffectConfig hasteEffect = new TrophyConfig.EffectConfig();
        hasteEffect.duration = 1800;
        hasteEffect.amplifier = 0;

        defaultTrophy.effects.put("minecraft:haste", hasteEffect);

        return defaultTrophy;
    }
}
