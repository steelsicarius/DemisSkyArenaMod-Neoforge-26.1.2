package net.jrdemiurge.skyarena.gametest;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.custom.AltarBlock;
import net.jrdemiurge.skyarena.block.entity.AltarBlockEntity;
import net.jrdemiurge.skyarena.config.ArenaConfig;
import net.jrdemiurge.skyarena.config.DifficultyLevelRange;
import net.jrdemiurge.skyarena.config.ExpandedMobInfo;
import net.jrdemiurge.skyarena.config.MobGroup;
import net.jrdemiurge.skyarena.config.SkyArenaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Prueba funcional automatizada del flujo completo del Altar de Batalla
 * (port NeoForge 26.1.2). Ejecutable con `gradlew runGameTestServer`.
 *
 * Estructura: skyarena:arena_test_platform (15x8x15, altar en (7,1,7),
 * ArenaType=gametest_arena). La config de esa arena se inyecta en runtime
 * desde el propio test para no tocar la config distribuida.
 *
 * Verifica los 3 bugs reportados por el usuario:
 *  1. click derecho al altar -> spawnea la oleada de mobs (el ClassCastException
 *     de MobLootTableAccessor ya no ocurre: el mixin esta aplicado).
 *  2. la zona de proteccion bloquea setBlock junto al altar (LevelMixin).
 *  3. al alejarse del altar mas de battleLossDistance, los mobs spawneados
 *     desaparecen (AltarTicker -> tick -> checkPlayerLeftArena -> onDefeat).
 */
public final class SkyArenaGameTests {

    private static final BlockPos ALTAR_LOCAL = new BlockPos(7, 1, 7);
    private static final BlockPos INSIDE_PROBE_LOCAL = new BlockPos(10, 1, 10); // dist ~4.2 < radio 5
    private static final int EXPECTED_MOBS = 3; // 60 puntos / zombie 20

    private SkyArenaGameTests() {}

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SkyArena.MOD_ID);

    public static final Supplier<Consumer<GameTestHelper>> ALTAR_BATTLE_FLOW =
            TEST_FUNCTIONS.register("altar_battle_flow", () -> SkyArenaGameTests::altarBattleFlow);

    public static final Supplier<Consumer<GameTestHelper>> DYNAMIC_MOB_POOL =
            TEST_FUNCTIONS.register("dynamic_mob_pool", () -> SkyArenaGameTests::dynamicMobPool);

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    public static void altarBattleFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos altarWorld = helper.absolutePos(ALTAR_LOCAL);

        injectTestArenaConfig();

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            // el GameTestServer arranca en peaceful por defecto
            level.getServer().setDifficulty(Difficulty.EASY, false);
        }

        // --- altar presente y con la config de la arena de test ---
        BlockEntity blockEntity = level.getBlockEntity(altarWorld);
        if (!(blockEntity instanceof AltarBlockEntity altar)) {
            helper.fail("No hay AltarBlockEntity en " + altarWorld + " (¿template arena_test_platform mal colocado?)");
            return;
        }
        if (!"gametest_arena".equals(altar.getArenaType())) {
            helper.fail("ArenaType inesperado: " + altar.getArenaType());
            return;
        }

        // La estructura se coloca ANTES de que este test inyecte la config de
        // 'gametest_arena', por lo que loadAdditional no encontro la arena y el
        // BE conserva los valores del NBT del template. Recargar la config aqui
        // replica exactamente lo que hace setPlacedBy al colocarlo un jugador
        // (y lo que hace loadAdditional en un mundo real, donde 'sky_arena'
        // ya existe en config/sky_arena.json).
        altar.loadArenaConfig("gametest_arena");

        if (altar.getFullProtectionRadius() != 5 || altar.getBattleLossDistance() != 10 || altar.getMobSpawnRadius() != 6) {
            helper.fail("La config del altar no cargo: protection=" + altar.getFullProtectionRadius()
                    + " lossDistance=" + altar.getBattleLossDistance() + " spawnRadius=" + altar.getMobSpawnRadius());
            return;
        }

        // --- Bug 2: proteccion de bloques junto al altar ---
        BlockPos insideWorld = helper.absolutePos(INSIDE_PROBE_LOCAL);
        boolean changed = level.setBlock(insideWorld, Blocks.DIRT.defaultBlockState(), 3);
        if (changed || level.getBlockState(insideWorld).is(Blocks.DIRT)) {
            helper.fail("setBlock dentro de la zona de proteccion NO se cancelo (LevelMixin sin aplicar?)");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] proteccion OK: setBlock cancelado dentro de la zona");

        // --- Bug 1: activar la batalla con un jugador simulado ---
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.teleportTo(altarWorld.getX() + 0.5D, altarWorld.getY() + 1.0D, altarWorld.getZ() + 0.5D);

        // El altar recien colocado tiene battleEndTime=0; si el mundo del
        // GameTestServer todavia no ha superado el tick 30, el cooldown
        // anti-doble-activacion bloquearia la batalla. Marcar la "ultima
        // batalla" como terminada hace 100 ticks (estado legitimo de un altar
        // en un mundo real, donde getGameTime() es enorme).
        altar.setBattleEndTime(level.getGameTime() - 100L);

        SkyArena.LOGGER.info("[SkyArena-GameTest] activando: difficulty={} gameTime={} isOngoing={}",
                level.getDifficulty(), level.getGameTime(),
                altar.isBattleOngoing(altar.getDifficultyLevel(player)));

        BlockState state = level.getBlockState(altarWorld);
        if (!(state.getBlock() instanceof AltarBlock altarBlock)) {
            helper.fail("El bloque en el altar no es AltarBlock: " + state.getBlock());
            return;
        }

        InteractionResult result = altarBlock.activate(level, altarWorld, state, player, InteractionHand.MAIN_HAND);

        if (altar.summonedMobs.isEmpty()) {
            helper.fail("La activacion no spawneo ningun mob (result=" + result
                    + ", difficulty=" + altar.getDifficultyLevel(player)
                    + ", availableMobs=" + altar.getAvailableMobs(altar.getDifficultyLevel(player)).size() + ")");
            return;
        }
        int spawned = altar.summonedMobs.size();
        if (spawned != EXPECTED_MOBS) {
            helper.fail("Oleada inesperada: esperados " + EXPECTED_MOBS + " mobs, spawneados " + spawned);
            return;
        }
        if (!altar.isBattlePhaseActive()) {
            helper.fail("battlePhaseActive no quedo activo tras el spawn de la oleada");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] spawn OK: {} mobs (result={})", spawned, result);

        // --- Bug 3: al alejarse del altar, los mobs desaparecen ---
        double away = altarWorld.getZ() + altar.getBattleLossDistance() + 8.0D;
        player.teleportTo(altarWorld.getX() + 0.5D, altarWorld.getY() + 1.0D, away);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!altar.summonedMobs.isEmpty()) {
                        throw new GameTestAssertException(Component.literal(
                                "Los mobs spawneados siguen presentes tras alejar al jugador ("
                                        + altar.summonedMobs.size() + " vivos)"), 1);
                    }
                })
                .thenSucceed();
        SkyArena.LOGGER.info("[SkyArena-GameTest] esperando despawn por distancia (battleLossDistance={})",
                altar.getBattleLossDistance());
    }

    /**
     * Arena de prueba: 60 puntos, zombies de coste 20 -> exactamente 3 zombies.
     * Radios pequenos para que la plataforma 15x15 cubra todo el area.
     */
    private static void injectTestArenaConfig() {
        if (SkyArenaConfig.configData == null) {
            SkyArenaConfig.loadConfig();
        }
        if (SkyArenaConfig.configData == null || SkyArenaConfig.configData.arenas == null) {
            throw new GameTestAssertException(
                    Component.literal("No se pudo cargar sky_arena.json para el gametest"), 1);
        }
        if (SkyArenaConfig.configData.arenas.containsKey("gametest_arena")) {
            return; // ya inyectada
        }

        ArenaConfig arena = new ArenaConfig();
        arena.mobSpawnRadius = 6;
        arena.spawnDistanceFromPlayer = 2;
        arena.battleLossDistance = 10;
        arena.mobTeleportDistance = 15;
        arena.mobGriefingProtectionRadius = 5;
        arena.fullProtectionRadius = 5;
        arena.bossBarHideRadius = 5;
        arena.mobCostRatio = 20;
        arena.startingPoints = 60;
        arena.startingStatMultiplier = 1.0D;
        arena.allowDifficultyReset = true;
        arena.allowWaterAndAirSpawn = false;
        arena.individualPlayerStats = false;
        arena.setNight = false;
        arena.setRain = false;
        arena.disableMobItemDrop = true;
        arena.resetDifficultyOnDefeat = false;
        arena.autoWaveRun = false;
        arena.pointsCoefficientPer1000BlocksFromWorldCenter = 0.0D;
        arena.statMultiplierCoefficientPer1000BlocksFromWorldCenter = 0.0D;
        arena.lootTableCountCoefficientPer1000BlocksFromWorldCenter = 0.0D;

        MobGroup group = new MobGroup();
        group.squadSpawnChance = 0.0D;
        group.squadSpawnSize = 1;
        group.statMultiplierCoefficient = 1.0D;
        group.mobSpawnChance = 1.0D;
        group.mobValues = new LinkedHashMap<>();
        group.mobValues.put("minecraft:zombie", 20);

        DifficultyLevelRange range = new DifficultyLevelRange();
        range.range = List.of(1, 10);
        range.pointsIncrease = 0;
        range.statMultiplierIncrease = 0.0D;
        range.rewardLootTable = "minecraft:empty";
        range.rewardCount = 0;
        range.mobGroupsUsed = List.of("test");

        arena.difficultyLevelRanges = List.of(range);
        arena.mobGroups = new LinkedHashMap<>();
        arena.mobGroups.put("test", group);
        arena.presetWaves = new LinkedHashMap<>();

        SkyArenaConfig.configData.arenas.put("gametest_arena", arena);
    }

    /**
     * 26.1.2 port: verifica la expansion dinamica de la pool de mobs
     * (mobs de otros mods) y su reparto por dificultad. En un entorno
     * vanilla:
     *  - con onlyModded=true (default) la pool dinamica no anade nada;
     *  - con onlyModded=false se anaden los hostiles vanilla no listados,
     *    con coste automatico (vida/3 + dano*4 + armadura*2) y SIN superar
     *    el cap del grupo (dynamicMaxCost; por defecto, el coste estatico
     *    mas alto del grupo);
     *  - los mobs excluidos por id (excludeMobs) o por mod entero
     *    (excludeMods) nunca entran;
     *  - un grupo vacio sin cap explicito no expande nada (y con cap si,
     *    como via de escape).
     */
    public static void dynamicMobPool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos altarWorld = helper.absolutePos(ALTAR_LOCAL);

        injectDynamicTestArenaConfigs();

        BlockEntity blockEntity = level.getBlockEntity(altarWorld);
        if (!(blockEntity instanceof AltarBlockEntity altar)) {
            helper.fail("No hay AltarBlockEntity en " + altarWorld);
            return;
        }

        // --- caso 1: onlyModded=true (default) -> en vanilla no se anade nada ---
        altar.loadArenaConfig("gametest_dynamic_modded");
        List<ExpandedMobInfo> moddedPool = altar.getAvailableMobs(1);
        if (moddedPool.size() != 1 || !"minecraft:zombie".equals(moddedPool.get(0).mobId)) {
            helper.fail("Pool con onlyModded=true deberia ser solo zombie, es: " + moddedPool.size() + " entradas");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] onlyModded=true OK: pool vanilla sin cambios ({})", moddedPool.size());

        // --- caso 2: onlyModded=false -> se expanden los hostiles registrados ---
        altar.loadArenaConfig("gametest_dynamic_all");
        List<ExpandedMobInfo> pool = altar.getAvailableMobs(1);

        java.util.Set<String> ids = new java.util.HashSet<>();
        for (ExpandedMobInfo m : pool) {
            if (!ids.add(m.mobId)) {
                helper.fail("Mob duplicado en la pool: " + m.mobId);
                return;
            }
            if (m.cost < 1) {
                helper.fail("Coste invalido para " + m.mobId + ": " + m.cost);
                return;
            }
        }

        boolean hasZombieStatic = pool.stream().anyMatch(m -> "minecraft:zombie".equals(m.mobId) && m.cost == 20);
        if (!hasZombieStatic) {
            helper.fail("El zombie estatico (coste 20) desaparecio de la pool");
            return;
        }
        if (!ids.contains("minecraft:creeper")) {
            helper.fail("La expansion dinamica no anadio minecraft:creeper (onlyModded=false)");
            return;
        }
        if (ids.contains("minecraft:iron_golem") || ids.contains("minecraft:villager")) {
            helper.fail("La pool dinamica incluyo mobs no hostiles");
            return;
        }
        if (ids.size() <= 1) {
            helper.fail("La pool no se expando: " + ids.size() + " entradas");
            return;
        }
        // cap por defecto = coste estatico maximo del grupo (zombie 20)
        for (ExpandedMobInfo m : pool) {
            if (m.cost > 20) {
                helper.fail("Cap por defecto no aplicado: " + m.mobId + " cost=" + m.cost + " > 20");
                return;
            }
        }

        int creeperCost = pool.stream().filter(m -> "minecraft:creeper".equals(m.mobId)).findFirst().orElseThrow().cost;
        SkyArena.LOGGER.info("[SkyArena-GameTest] pool dinamica OK: {} mobs (cap default 20), creeper cost={} (20/3 vida + 3*4 dano)",
                pool.size(), creeperCost);

        // --- caso 3: dynamicMaxCost deja fuera a los fuertes del tier ---
        altar.loadArenaConfig("gametest_dynamic_cap");
        List<ExpandedMobInfo> cappedPool = altar.getAvailableMobs(1);
        java.util.Set<String> cappedIds = new java.util.HashSet<>();
        for (ExpandedMobInfo m : cappedPool) {
            if (!cappedIds.add(m.mobId)) {
                helper.fail("Mob duplicado en la pool capada: " + m.mobId);
                return;
            }
            if (m.cost > 20) {
                helper.fail("dynamicMaxCost no se respeto: " + m.mobId + " cost=" + m.cost + " > 20");
                return;
            }
        }
        if (!cappedIds.contains("minecraft:creeper")) {
            helper.fail("creeper (coste 19) deberia caber en un grupo con cap 20");
            return;
        }
        if (cappedIds.contains("minecraft:warden")) {
            helper.fail("warden (coste >= 167) no debe entrar en un grupo con cap 20");
            return;
        }
        if (cappedIds.contains("minecraft:ravager")) {
            helper.fail("ravager (coste >= 33) no debe entrar en un grupo con cap 20");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] tiering OK: {} mobs con cap 20, sin warden/ravager", cappedPool.size());

        // --- caso 4: excludeMobs excluye mobs concretos ---
        altar.loadArenaConfig("gametest_dynamic_excludemob");
        List<ExpandedMobInfo> exMobPool = altar.getAvailableMobs(1);
        java.util.Set<String> exMobIds = new java.util.HashSet<>();
        for (ExpandedMobInfo m : exMobPool) exMobIds.add(m.mobId);
        if (exMobIds.contains("minecraft:creeper")) {
            helper.fail("excludeMobs no excluyo minecraft:creeper");
            return;
        }
        if (!exMobIds.contains("minecraft:blaze")) {
            helper.fail("blaze (coste 31 <= cap 60) deberia estar en la pool con excludeMobs solo de creeper");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] excludeMobs OK: {} mobs, sin creeper, con blaze", exMobPool.size());

        // --- caso 5: excludeMods excluye un mod entero ---
        altar.loadArenaConfig("gametest_dynamic_excludemod");
        List<ExpandedMobInfo> exModPool = altar.getAvailableMobs(1);
        if (exModPool.size() != 1 || !"minecraft:zombie".equals(exModPool.get(0).mobId)) {
            helper.fail("excludeMods=[minecraft] deberia dejar la pool solo con el zombie estatico, es: " + exModPool.size());
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] excludeMods OK: namespace entero excluido");

        // --- caso 6: grupo vacio sin cap no expande; con cap explicito si ---
        altar.loadArenaConfig("gametest_dynamic_empty");
        List<ExpandedMobInfo> emptyPool = altar.getAvailableMobs(1);
        if (!emptyPool.isEmpty()) {
            helper.fail("Un grupo vacio sin dynamicMaxCost no deberia expandirse: " + emptyPool.size() + " entradas");
            return;
        }
        altar.loadArenaConfig("gametest_dynamic_empty_on");
        List<ExpandedMobInfo> emptyOnPool = altar.getAvailableMobs(1);
        boolean emptyOnHasCreeper = emptyOnPool.stream().anyMatch(m -> "minecraft:creeper".equals(m.mobId));
        if (!emptyOnHasCreeper) {
            helper.fail("Grupo vacio con dynamicMaxCost=60 deberia admitir dinamicos (creeper)");
            return;
        }
        SkyArena.LOGGER.info("[SkyArena-GameTest] grupos vacios OK: sin cap = aparcados, con cap = opt-in");

        helper.succeed();
    }

    private static void injectDynamicTestArenaConfigs() {
        if (SkyArenaConfig.configData == null) {
            SkyArenaConfig.loadConfig();
        }
        if (SkyArenaConfig.configData == null || SkyArenaConfig.configData.arenas == null) {
            throw new GameTestAssertException(
                    Component.literal("No se pudo cargar sky_arena.json para el gametest"), 1);
        }
        if (SkyArenaConfig.configData.arenas.containsKey("gametest_dynamic_modded")) {
            return; // ya inyectadas
        }

        DifficultyLevelRange range = new DifficultyLevelRange();
        range.range = List.of(1, 10);
        range.pointsIncrease = 0;
        range.statMultiplierIncrease = 0.0D;
        range.rewardLootTable = "minecraft:empty";
        range.rewardCount = 0;
        range.mobGroupsUsed = List.of("dyn");

        // Caso 1: onlyModded=true (default implicito) -> vanilla sin cambios
        registerDynamicArena("gametest_dynamic_modded", range,
                makeGroup(zombieOnly(), null, null, null, null, null));

        // Caso 2: onlyModded=false -> expande con los hostiles registrados,
        // capados al maximo estatico del grupo (zombie 20)
        registerDynamicArena("gametest_dynamic_all", range,
                makeGroup(zombieOnly(), true, false, null, null, null));

        // Caso 3: cap explicito 20 -> los fuertes (warden, ravager) quedan fuera
        registerDynamicArena("gametest_dynamic_cap", range,
                makeGroup(zombieOnly(), true, false, 20, null, null));

        // Caso 4: excludeMobs -> creeper fuera, blaze (coste 31 <= 60) dentro
        registerDynamicArena("gametest_dynamic_excludemob", range,
                makeGroup(zombieOnly(), true, false, 60, List.of("minecraft:creeper"), null));

        // Caso 5: excludeMods -> el namespace entero fuera
        registerDynamicArena("gametest_dynamic_excludemod", range,
                makeGroup(zombieOnly(), true, false, 60, null, List.of("minecraft")));

        // Caso 6a: grupo vacio sin cap -> no expande (aparcado)
        registerDynamicArena("gametest_dynamic_empty", range,
                makeGroup(null, true, false, null, null, null));

        // Caso 6b: grupo vacio con cap explicito -> opt-in de dinamicos
        registerDynamicArena("gametest_dynamic_empty_on", range,
                makeGroup(null, true, false, 60, null, null));
    }

    private static LinkedHashMap<String, Integer> zombieOnly() {
        LinkedHashMap<String, Integer> mobValues = new LinkedHashMap<>();
        mobValues.put("minecraft:zombie", 20);
        return mobValues;
    }

    private static MobGroup makeGroup(LinkedHashMap<String, Integer> mobValues, Boolean dynamic,
                                      Boolean onlyModded, Integer dynamicMaxCost,
                                      List<String> excludeMobs, List<String> excludeMods) {
        MobGroup group = new MobGroup();
        group.squadSpawnChance = 0.0D;
        group.squadSpawnSize = 1;
        group.statMultiplierCoefficient = 1.0D;
        group.mobSpawnChance = 1.0D;
        group.mobValues = mobValues != null ? mobValues : new LinkedHashMap<>();
        group.dynamic = dynamic;
        group.onlyModded = onlyModded;
        group.dynamicMaxCost = dynamicMaxCost;
        group.excludeMobs = excludeMobs;
        group.excludeMods = excludeMods;
        return group;
    }

    private static void registerDynamicArena(String arenaName, DifficultyLevelRange range, MobGroup group) {
        ArenaConfig arena = baseDynamicArena();
        arena.difficultyLevelRanges = List.of(range);
        arena.mobGroups = new LinkedHashMap<>();
        arena.mobGroups.put("dyn", group);
        SkyArenaConfig.configData.arenas.put(arenaName, arena);
    }

    private static ArenaConfig baseDynamicArena() {
        ArenaConfig arena = new ArenaConfig();
        arena.mobSpawnRadius = 6;
        arena.spawnDistanceFromPlayer = 2;
        arena.battleLossDistance = 10;
        arena.mobTeleportDistance = 15;
        arena.mobGriefingProtectionRadius = 5;
        arena.fullProtectionRadius = 5;
        arena.bossBarHideRadius = 5;
        arena.mobCostRatio = 20;
        arena.startingPoints = 60;
        arena.startingStatMultiplier = 1.0D;
        arena.individualPlayerStats = false;
        arena.setNight = false;
        arena.setRain = false;
        arena.autoWaveRun = false;
        arena.presetWaves = new LinkedHashMap<>();
        return arena;
    }
}
