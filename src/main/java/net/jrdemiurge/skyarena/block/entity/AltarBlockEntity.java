package net.jrdemiurge.skyarena.block.entity;

import net.jrdemiurge.skyarena.Config;
import net.jrdemiurge.skyarena.block.custom.AltarBlock;
import net.jrdemiurge.skyarena.config.*;
import net.jrdemiurge.skyarena.scheduler.Scheduler;
import net.jrdemiurge.skyarena.util.BossBarHideZones;
import net.jrdemiurge.skyarena.util.FullProtectionZones;
import net.jrdemiurge.skyarena.util.MobGriefingProtectionZones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class AltarBlockEntity extends BlockEntity {

    private static final Map<Player, BlockPos> activeAltarBlocks = new HashMap<>();
    /** Server-side loaded altar entities; ticked from the global server tick. */
    private static final Set<AltarBlockEntity> ACTIVE_INSTANCES = Collections.newSetFromMap(new IdentityHashMap<>());

    private Player activatingPlayer;
    public final List<Entity> summonedMobs = new ArrayList<>();
    private boolean battlePhaseActive = false;
    private boolean playerDeath = false;
    private int deathDelay = 0;
    private long battleEndTime = 0;
    private int glowingCounter = 0;
    private boolean firstMessageSent = false;
    private static final Map<Player, Long> playerMessageTimestamps = new HashMap<>();
    // music
    private ItemStack recordItem = ItemStack.EMPTY;
    private boolean isPlayingMusic = false;
    private long musicEndTick = 0;
    private long musicTickCount = 0;
    // difficulty
    private int difficultyLevel = 1;
    private int battleDifficultyLevel = 1;
    // block data
    private String arenaType = "sky_arena";
    private int mobSpawnRadius;
    private int spawnDistanceFromPlayer;
    private int battleLossDistance;
    private int mobTeleportDistance;
    private int mobGriefingProtectionRadius = 0;
    private int fullProtectionRadius = 0;
    private int bossBarHideRadius = 0;
    private int mobCostRatio;

    private boolean allowDifficultyReset;
    private boolean allowWaterAndAirSpawn;
    private boolean individualPlayerStats;
    private boolean setNight;
    private boolean setRain;
    private boolean disableMobItemDrop;

    private int startingPoints;
    private double startingStatMultiplier;

    private boolean resetDifficultyOnDefeat;
    private boolean autoWaveRun;

    private double pointsCoefficientPerBlocks;
    private double statMultiplierCoefficientPerBlocks;
    private double lootTableCountCoefficientPerBlocks;

    private List<DifficultyLevelRange> difficultyLevelRanges = new ArrayList<>();
    private LinkedHashMap<String, MobGroup> mobGroups = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PresetWave> presetWaves = new LinkedHashMap<>();

    private final Map<String, Integer> playerDifficulty = new HashMap<>();

    public AltarBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntity.ALTAR_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public void loadArenaConfig(String arenaType) {
        if (level != null && level.isClientSide()) {
            return;
        }

        ArenaConfig arenaConfig;

        if (SkyArenaConfig.configData == null) {
            arenaConfig = SkyArenaConfig.DEFAULT_ARENA;
        } else {
            arenaConfig = SkyArenaConfig.configData.arenas.getOrDefault(arenaType, SkyArenaConfig.DEFAULT_ARENA);
        }

        if (arenaConfig != null) {
            this.arenaType = arenaType;
            this.mobSpawnRadius = arenaConfig.mobSpawnRadius != 0 ? arenaConfig.mobSpawnRadius : 36;
            this.spawnDistanceFromPlayer = arenaConfig.spawnDistanceFromPlayer;
            this.battleLossDistance = arenaConfig.battleLossDistance != 0 ? arenaConfig.battleLossDistance : 60;
            this.mobTeleportDistance = arenaConfig.mobTeleportDistance != 0 ? arenaConfig.mobTeleportDistance : 50;
            this.mobGriefingProtectionRadius = arenaConfig.mobGriefingProtectionRadius;
            this.fullProtectionRadius = arenaConfig.fullProtectionRadius;
            this.bossBarHideRadius = arenaConfig.bossBarHideRadius;
            this.mobCostRatio = arenaConfig.mobCostRatio != 0 ? arenaConfig.mobCostRatio : 20;

            this.allowDifficultyReset = arenaConfig.allowDifficultyReset;
            this.allowWaterAndAirSpawn = arenaConfig.allowWaterAndAirSpawn;
            this.individualPlayerStats = arenaConfig.individualPlayerStats;
            this.setNight = arenaConfig.setNight;
            this.setRain = arenaConfig.setRain;
            this.disableMobItemDrop = arenaConfig.disableMobItemDrop;

            this.startingPoints = arenaConfig.startingPoints != 0 ? arenaConfig.startingPoints : 500;
            this.startingStatMultiplier = arenaConfig.startingStatMultiplier != 0.0 ? arenaConfig.startingStatMultiplier : 1.0;

            this.resetDifficultyOnDefeat = arenaConfig.resetDifficultyOnDefeat;
            this.autoWaveRun = arenaConfig.autoWaveRun;

            this.pointsCoefficientPerBlocks = arenaConfig.pointsCoefficientPer1000BlocksFromWorldCenter;
            this.statMultiplierCoefficientPerBlocks = arenaConfig.statMultiplierCoefficientPer1000BlocksFromWorldCenter;
            this.lootTableCountCoefficientPerBlocks = arenaConfig.lootTableCountCoefficientPer1000BlocksFromWorldCenter;

            if (fullProtectionRadius != 0 && fullProtectionRadius > mobGriefingProtectionRadius) {
                mobGriefingProtectionRadius = fullProtectionRadius;
            }

            if (arenaConfig.difficultyLevelRanges != null) {
                this.difficultyLevelRanges = new ArrayList<>(arenaConfig.difficultyLevelRanges);
            } else {
                this.difficultyLevelRanges = new ArrayList<>();
            }

            if (arenaConfig.mobGroups != null) {
                this.mobGroups = new LinkedHashMap<>(arenaConfig.mobGroups);
            } else {
                this.mobGroups = new LinkedHashMap<>();
            }

            if (arenaConfig.presetWaves != null) {
                this.presetWaves = new LinkedHashMap<>(arenaConfig.presetWaves);
            } else {
                this.presetWaves = new LinkedHashMap<>();
            }

            if (level != null) {
                MobGriefingProtectionZones.remove(level, this.getBlockPos());
                if (mobGriefingProtectionRadius != 0) {
                    MobGriefingProtectionZones.add(level, this.getBlockPos(), mobGriefingProtectionRadius);
                }

                FullProtectionZones.remove(level, this.getBlockPos());
                if (fullProtectionRadius != 0) {
                    FullProtectionZones.add(level, this.getBlockPos(), fullProtectionRadius);
                }

                BossBarHideZones.remove(level, this.getBlockPos());
                if (bossBarHideRadius != 0) {
                    BossBarHideZones.add(level, this.getBlockPos(), bossBarHideRadius);
                }
            }

            if (this.level != null) {
                this.level.blockEntityChanged(this.getBlockPos());
            }
        }
    }

    public void switchToNextArena() {
        if (SkyArenaConfig.configData == null) {
            return;
        }

        List<String> arenaTypes = new ArrayList<>(SkyArenaConfig.configData.arenas.keySet());

        if (arenaTypes.isEmpty()) {
            return;
        }

        int currentIndex = arenaTypes.indexOf(this.arenaType);
        int nextIndex = (currentIndex + 1) % arenaTypes.size();

        this.arenaType = arenaTypes.get(nextIndex);
        loadArenaConfig(this.arenaType);

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            ACTIVE_INSTANCES.add(this);

            if (mobGriefingProtectionRadius != 0) {
                MobGriefingProtectionZones.add(level, this.getBlockPos(), mobGriefingProtectionRadius);
            }

            if (fullProtectionRadius != 0) {
                FullProtectionZones.add(level, this.getBlockPos(), fullProtectionRadius);
            }

            if (bossBarHideRadius != 0) {
                BossBarHideZones.add(level, this.getBlockPos(), bossBarHideRadius);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ACTIVE_INSTANCES.remove(this);
        removeSummonedMobs();
        if (this.level != null && !this.level.isClientSide()) {
            MobGriefingProtectionZones.remove(level, this.getBlockPos());

            FullProtectionZones.remove(level, this.getBlockPos());

            BossBarHideZones.remove(level, this.getBlockPos());
        }
        stopMusic();
    }

    /**
     * 26.1.2 port: block entities no longer receive ticks through
     * BaseEntityBlock#getTicker; all loaded altar entities are ticked once per
     * server tick from AltarTicker (TickEvent.ServerTickEvent.Post).
     */
    public static void tickAll() {
        for (AltarBlockEntity entity : List.copyOf(ACTIVE_INSTANCES)) {
            Level level = entity.getLevel();
            if (level == null || !level.isLoaded(entity.getBlockPos())) continue;
            entity.tick(level, entity.getBlockPos(), level.getBlockState(entity.getBlockPos()));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput pOutput) {
        super.saveAdditional(pOutput);

        pOutput.putInt("DifficultyLevel", this.difficultyLevel);
        pOutput.putString("ArenaType", this.arenaType);
        pOutput.putInt("StartingPoints", this.startingPoints);
        pOutput.putDouble("StartingStatMultiplier", this.startingStatMultiplier);
        pOutput.putInt("MobSpawnRadius", this.mobSpawnRadius);
        pOutput.putInt("MobCostRatio", this.mobCostRatio);
        pOutput.putInt("SpawnDistanceFromPlayer", this.spawnDistanceFromPlayer);
        pOutput.putInt("BattleLossDistance", this.battleLossDistance);
        pOutput.putInt("MobTeleportDistance", this.mobTeleportDistance);
        pOutput.putBoolean("AllowDifficultyReset", this.allowDifficultyReset);
        pOutput.putBoolean("AllowWaterAndAirSpawn", this.allowWaterAndAirSpawn);
        pOutput.putBoolean("IndividualPlayerStats", this.individualPlayerStats);
        pOutput.putBoolean("SetNight", this.setNight);
        pOutput.putBoolean("SetRain", this.setRain);
        pOutput.putBoolean("EnableMobItemDrop", this.disableMobItemDrop);
        pOutput.putInt("MobGriefingProtectionRadius", this.mobGriefingProtectionRadius);
        pOutput.putInt("FullProtectionRadius", this.fullProtectionRadius);
        pOutput.putInt("BossBarHideRadius", this.bossBarHideRadius);
        pOutput.putBoolean("ResetDifficultyOnDefeat", this.resetDifficultyOnDefeat);
        pOutput.putBoolean("AutoWaveRun", this.autoWaveRun);
        pOutput.putDouble("PointsCoefficientPerBlocks", this.pointsCoefficientPerBlocks);
        pOutput.putDouble("StatMultiplierCoefficientPerBlocks", this.statMultiplierCoefficientPerBlocks);
        pOutput.putDouble("LootTableCountCoefficientPerBlocks", this.lootTableCountCoefficientPerBlocks);

        if (!this.recordItem.isEmpty()) {
            pOutput.store("RecordItem", ItemStack.CODEC, this.recordItem);
        }

        if (!playerDifficulty.isEmpty()) {
            ValueOutput.ValueOutputList list = pOutput.childrenList("PlayerDifficulty");
            for (Map.Entry<String, Integer> entry : playerDifficulty.entrySet()) {
                ValueOutput child = list.addChild();
                child.putString("Name", entry.getKey());
                child.putInt("Level", entry.getValue());
            }
        }

        if (!difficultyLevelRanges.isEmpty()) {
            ValueOutput.ValueOutputList rangesTag = pOutput.childrenList("DifficultyLevelRanges");
            for (DifficultyLevelRange range : difficultyLevelRanges) {
                ValueOutput rangeTag = rangesTag.addChild();

                ValueOutput.ValueOutputList intList = rangeTag.childrenList("Range");
                for (int val : range.range) {
                    intList.addChild().putInt("Value", val);
                }
                rangeTag.putInt("PointsIncrease", range.pointsIncrease);
                rangeTag.putDouble("StatMultiplierIncrease", range.statMultiplierIncrease);
                rangeTag.putString("RewardLootTable", range.rewardLootTable);
                rangeTag.putInt("RewardCount", range.rewardCount);

                ValueOutput.ValueOutputList usedGroupsTag = rangeTag.childrenList("MobGroupsUsed");
                for (String group : range.mobGroupsUsed) {
                    usedGroupsTag.addChild().putString("Group", group);
                }
            }
        }

        if (!mobGroups.isEmpty()) {
            ValueOutput.ValueOutputList groupsTag = pOutput.childrenList("MobGroups");
            for (Map.Entry<String, MobGroup> entry : mobGroups.entrySet()) {
                ValueOutput groupTag = groupsTag.addChild();
                MobGroup group = entry.getValue();
                groupTag.putString("Id", entry.getKey());
                groupTag.putDouble("SquadSpawnChance", group.squadSpawnChance);
                groupTag.putInt("SquadSpawnSize", group.squadSpawnSize);
                groupTag.putDouble("StatMultiplierCoefficient", group.statMultiplierCoefficient);
                groupTag.putDouble("MobSpawnChance", group.mobSpawnChance);

                ValueOutput.ValueOutputList mobValuesTag = groupTag.childrenList("MobValues");
                for (Map.Entry<String, Integer> mobEntry : group.mobValues.entrySet()) {
                    ValueOutput mobChild = mobValuesTag.addChild();
                    mobChild.putString("MobId", mobEntry.getKey());
                    mobChild.putInt("Value", mobEntry.getValue());
                }
            }
        }

        if (!presetWaves.isEmpty()) {
            ValueOutput.ValueOutputList presetWavesTag = pOutput.childrenList("PresetWaves");

            for (Map.Entry<Integer, PresetWave> entry : presetWaves.entrySet()) {
                ValueOutput waveTag = presetWavesTag.addChild();
                waveTag.putInt("Wave", entry.getKey());
                waveTag.putDouble("MobStatMultiplier", entry.getValue().mobStatMultiplier);
                waveTag.putString("RewardLootTable", entry.getValue().rewardLootTable);
                waveTag.putInt("RewardCount", entry.getValue().rewardCount);

                ValueOutput.ValueOutputList mobsTag = waveTag.childrenList("Mobs");
                for (WaveMob mob : entry.getValue().mobs) {
                    ValueOutput mobTag = mobsTag.addChild();
                    mobTag.putString("Type", mob.type);
                    mobTag.putInt("Count", mob.count);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput pInput) {
        super.loadAdditional(pInput);

        this.difficultyLevel = pInput.getIntOr("DifficultyLevel", this.difficultyLevel);
        this.arenaType = pInput.getStringOr("ArenaType", this.arenaType);
        this.startingPoints = pInput.getIntOr("StartingPoints", this.startingPoints);
        this.startingStatMultiplier = pInput.getDoubleOr("StartingStatMultiplier", this.startingStatMultiplier);
        this.mobSpawnRadius = pInput.getIntOr("MobSpawnRadius", this.mobSpawnRadius);
        this.mobCostRatio = pInput.getIntOr("MobCostRatio", this.mobCostRatio);
        this.spawnDistanceFromPlayer = pInput.getIntOr("SpawnDistanceFromPlayer", this.spawnDistanceFromPlayer);
        this.battleLossDistance = pInput.getIntOr("BattleLossDistance", this.battleLossDistance);
        this.mobTeleportDistance = pInput.getIntOr("MobTeleportDistance", this.mobTeleportDistance);
        this.allowDifficultyReset = pInput.getBooleanOr("AllowDifficultyReset", this.allowDifficultyReset);
        this.allowWaterAndAirSpawn = pInput.getBooleanOr("AllowWaterAndAirSpawn", this.allowWaterAndAirSpawn);
        this.individualPlayerStats = pInput.getBooleanOr("IndividualPlayerStats", this.individualPlayerStats);
        this.setNight = pInput.getBooleanOr("SetNight", this.setNight);
        this.setRain = pInput.getBooleanOr("SetRain", this.setRain);
        this.disableMobItemDrop = pInput.getBooleanOr("EnableMobItemDrop", this.disableMobItemDrop);
        this.mobGriefingProtectionRadius = pInput.getIntOr("MobGriefingProtectionRadius", this.mobGriefingProtectionRadius);
        this.fullProtectionRadius = pInput.getIntOr("FullProtectionRadius", this.fullProtectionRadius);
        this.bossBarHideRadius = pInput.getIntOr("BossBarHideRadius", this.bossBarHideRadius);
        this.resetDifficultyOnDefeat = pInput.getBooleanOr("ResetDifficultyOnDefeat", this.resetDifficultyOnDefeat);
        this.autoWaveRun = pInput.getBooleanOr("AutoWaveRun", this.autoWaveRun);
        this.pointsCoefficientPerBlocks = pInput.getDoubleOr("PointsCoefficientPerBlocks", this.pointsCoefficientPerBlocks);
        this.statMultiplierCoefficientPerBlocks = pInput.getDoubleOr("StatMultiplierCoefficientPerBlocks", this.statMultiplierCoefficientPerBlocks);
        this.lootTableCountCoefficientPerBlocks = pInput.getDoubleOr("LootTableCountCoefficientPerBlocks", this.lootTableCountCoefficientPerBlocks);

        java.util.Optional<ItemStack> record = pInput.read("RecordItem", ItemStack.CODEC);
        if (record.isPresent()) {
            this.recordItem = record.get();
        }

        pInput.childrenListOrEmpty("PlayerDifficulty").forEach(child -> {
            java.util.Optional<String> name = child.getString("Name");
            if (name.isPresent()) {
                playerDifficulty.put(name.get(), child.getIntOr("Level", 1));
            }
        });

        difficultyLevelRanges.clear();
        pInput.childrenListOrEmpty("DifficultyLevelRanges").forEach(rangeTag -> {
            DifficultyLevelRange range = new DifficultyLevelRange();

            List<Integer> values = new ArrayList<>();
            rangeTag.childrenListOrEmpty("Range").forEach(intChild ->
                    values.add(intChild.getIntOr("Value", 0)));
            range.range = values;

            range.pointsIncrease = rangeTag.getIntOr("PointsIncrease", 0);
            range.statMultiplierIncrease = rangeTag.getDoubleOr("StatMultiplierIncrease", 0.0);
            range.rewardLootTable = rangeTag.getStringOr("RewardLootTable", "minecraft:empty");
            range.rewardCount = rangeTag.getIntOr("RewardCount", 0);

            List<String> usedGroups = new ArrayList<>();
            rangeTag.childrenListOrEmpty("MobGroupsUsed").forEach(groupChild ->
                    groupChild.getString("Group").ifPresent(usedGroups::add));
            range.mobGroupsUsed = usedGroups;

            difficultyLevelRanges.add(range);
        });

        mobGroups.clear();
        pInput.childrenListOrEmpty("MobGroups").forEach(groupTag -> {
            java.util.Optional<String> id = groupTag.getString("Id");
            if (id.isEmpty()) return;
            MobGroup group = new MobGroup();

            group.squadSpawnChance = groupTag.getDoubleOr("SquadSpawnChance", 0.0);
            group.squadSpawnSize = groupTag.getIntOr("SquadSpawnSize", 0);
            group.statMultiplierCoefficient = groupTag.getDoubleOr("StatMultiplierCoefficient", 1.0);
            group.mobSpawnChance = groupTag.getDoubleOr("MobSpawnChance", 0.0);

            group.mobValues = new LinkedHashMap<>();
            groupTag.childrenListOrEmpty("MobValues").forEach(mobChild -> {
                java.util.Optional<String> mobId = mobChild.getString("MobId");
                if (mobId.isPresent()) {
                    group.mobValues.put(mobId.get(), mobChild.getIntOr("Value", 0));
                }
            });

            mobGroups.put(id.get(), group);
        });

        presetWaves.clear();
        pInput.childrenListOrEmpty("PresetWaves").forEach(waveTag -> {
            int waveNumber = waveTag.getIntOr("Wave", 0);

            PresetWave wave = new PresetWave();
            wave.mobStatMultiplier = waveTag.getDoubleOr("MobStatMultiplier", 0.0);
            wave.rewardLootTable = waveTag.getStringOr("RewardLootTable", "minecraft:empty");
            wave.rewardCount = waveTag.getIntOr("RewardCount", 0);

            List<WaveMob> mobs = new ArrayList<>();
            waveTag.childrenListOrEmpty("Mobs").forEach(mobTag -> {
                WaveMob mob = new WaveMob();
                mob.type = mobTag.getStringOr("Type", "");
                mob.count = mobTag.getIntOr("Count", 0);
                mobs.add(mob);
            });
            wave.mobs = mobs;

            presetWaves.put(waveNumber, wave);
        });

        if (SkyArenaConfig.configData != null) {
            ArenaConfig arenaConfig = SkyArenaConfig.configData.arenas.getOrDefault(this.arenaType, null);
            if (arenaConfig != null) {
                loadArenaConfig(this.arenaType);
            }
        }
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (battlePhaseActive) {
            handleMusic();
            checkPlayerLeftArena();
            handlePlayerDeath();
            updateSummonedMobs();
            if (autoWaveRun) runAutoWave();
        } else {
            updateDifficultyMessages();
        }
    }

    private void updateDifficultyMessages() {
        long gameTime = level.getGameTime();
        if (gameTime % 10 != 0) return;

        AABB area = new AABB(worldPosition).inflate(2);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            if (!isBattleOngoing(getDifficultyLevel(player))) return;

            long lastShownTick = playerMessageTimestamps.getOrDefault(player, -40L);

            if (gameTime - lastShownTick >= 60) {
                int diff = getDifficultyLevel(player);
                player.sendOverlayMessage(
                        Component.translatable("message.skyarena.difficult_level")
                                .append(Component.literal(String.valueOf(diff))));
                playerMessageTimestamps.put(player, level.getGameTime() - 20);
            }
        }
    }

    private void handleMusic() {
        if (!isPlayingMusic || level == null || recordItem.isEmpty()) return;

        if (musicTickCount >= musicEndTick) {
            stopMusic();
            startMusic();
        }
        musicTickCount++;
    }

    public void startMusic() {
        if (this.level != null && !recordItem.isEmpty() && !isPlayingMusic) {
            java.util.Optional<net.minecraft.core.Holder<JukeboxSong>> song = JukeboxSong.fromStack(recordItem);
            if (song.isEmpty()) return;
            this.musicTickCount = this.level.getGameTime();
            this.musicEndTick = this.musicTickCount + song.get().value().lengthInTicks() + 20L;
            this.isPlayingMusic = true;
            // Same level event the vanilla JukeboxSongPlayer uses to play a song.
            int songId = this.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(song.get().value());
            this.level.levelEvent(null, 1010, this.getBlockPos(), songId);
            this.setChanged();
        }
    }

    public void stopMusic() {
        if (this.level != null && isPlayingMusic) {
            this.isPlayingMusic = false;
            this.level.levelEvent(1011, this.getBlockPos(), 0);
            this.setChanged();
        }
    }

    private void checkPlayerLeftArena() {
        if (activatingPlayer == null) return;
        double distance = activatingPlayer.distanceToSqr(worldPosition.getCenter());

        if (distance > battleLossDistance * battleLossDistance) {
            onDefeat();

            if (activatingPlayer instanceof ServerPlayer serverPlayer && Config.enableLossMessageLeave) {
                sendTitle(serverPlayer,
                        "message.skyarena.battle_failed", ChatFormatting.DARK_RED,
                        "message.skyarena.left_arena", ChatFormatting.WHITE);
            }
        }
    }

    private void handlePlayerDeath() {
        if (!playerDeath) return;
        deathDelay++;

        if (activatingPlayer != null && deathDelay > 10) {
            if (activatingPlayer.getHealth() > 0) {
                playerDeath = false;
                deathDelay = 0;
                return;
            }

            onDefeat();
            if (activatingPlayer instanceof ServerPlayer serverPlayer && Config.enableLossMessageDeath) {
                sendTitle(serverPlayer,
                        "message.skyarena.wasted", ChatFormatting.WHITE,
                        "message.skyarena.battle_failed", ChatFormatting.DARK_RED);
            }
        }
    }

    private void sendTitle(ServerPlayer player, String titleKey, ChatFormatting titleColor, String subtitleKey, ChatFormatting subtitleColor) {
        Component title = Component.translatable(titleKey).withStyle(titleColor);
        Component subtitle = Component.translatable(subtitleKey).withStyle(subtitleColor);
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 10));
    }

    public void onDefeat() {
        removeSummonedMobs();
        setBattlePhaseActive(false);
        this.stopMusic();
        if (resetDifficultyOnDefeat) setDifficultyLevel(activatingPlayer, 1);
        removeAltarActivationForPlayer();
        playerDeath = false;
        deathDelay = 0;
    }

    private void updateSummonedMobs() {
        if (activatingPlayer == null) return;
        summonedMobs.removeIf(Entity::isRemoved);

        for (Entity entity : summonedMobs) {
            if (entity instanceof Mob mob) {
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    if (!(activatingPlayer.isCreative() || activatingPlayer.isSpectator())) {
                        double mobDistance = activatingPlayer.distanceToSqr(entity);
                        if (mobDistance <= 16 * 16) {
                            mob.setTarget(activatingPlayer);
                        }
                    }
                }
            }

            double mobDistanceToAltar = entity.distanceToSqr(worldPosition.getCenter());
            if (mobDistanceToAltar > mobTeleportDistance * mobTeleportDistance) {
                List<BlockPos> spawnPositions = findValidSpawnPositions(level, worldPosition, activatingPlayer);
                if (!spawnPositions.isEmpty()) {
                    BlockPos teleportPos = spawnPositions.get(level.getRandom().nextInt(spawnPositions.size()));
                    entity.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
                }
            }
        }
    }

    private void runAutoWave() {
        if (!battlePhaseActive || !canSummonMobs()) return;
        long gameTime = level.getGameTime();
        if (gameTime % 5 != 0) return;
        BlockState state = level.getBlockState(worldPosition);

        if (!(state.getBlock() instanceof AltarBlock customBlock)) {
            return;
        }

        // 26.1.2 port: Block.use() no longer exists; the shared logic lives in
        // AltarBlock#activate.
        customBlock.activate(
                level,
                worldPosition,
                state,
                activatingPlayer,
                InteractionHand.OFF_HAND
        );

        if (!isBattleOngoing(getDifficultyLevel(activatingPlayer))) {
            removeAltarActivationForPlayer();
            return;
        }

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Scheduler.schedule(() -> {
                if (activatingPlayer instanceof ServerPlayer serverPlayer) {
                    Component title = Component.translatable(String.valueOf(5 - finalI));
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(title));
                    serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(5, 15, 0));
                }
            }, 20 * i);
        }

        Scheduler.schedule(() -> {
            customBlock.activate(
                    level,
                    worldPosition,
                    state,
                    activatingPlayer,
                    InteractionHand.OFF_HAND
            );
        }, 100);
    }

    public List<BlockPos> findValidSpawnPositions(Level level, BlockPos center, Player player) {
        List<BlockPos> validPositions = new ArrayList<>();

        if (!allowWaterAndAirSpawn) {
            for (int x = -mobSpawnRadius; x <= mobSpawnRadius; x++) {
                for (int z = -mobSpawnRadius; z <= mobSpawnRadius; z++) {
                    BlockPos currentPos = center.offset(x, 0, z);

                    if (x * x + z * z <= mobSpawnRadius * mobSpawnRadius &&
                            level.isEmptyBlock(currentPos) &&
                            level.isEmptyBlock(currentPos.above()) &&
                            level.isEmptyBlock(currentPos.above(2)) &&
                            level.isEmptyBlock(currentPos.north()) && level.isEmptyBlock(currentPos.south()) &&
                            level.isEmptyBlock(currentPos.east()) && level.isEmptyBlock(currentPos.west()) &&
                            !level.isEmptyBlock(currentPos.below()) &&
                            player.blockPosition().distSqr(currentPos) > spawnDistanceFromPlayer * spawnDistanceFromPlayer) {
                        validPositions.add(currentPos);
                    }
                }
            }
            return validPositions;
        } else {
            for (int x = -mobSpawnRadius; x <= mobSpawnRadius; x++) {
                for (int z = -mobSpawnRadius; z <= mobSpawnRadius; z++) {
                    BlockPos currentPos = center.offset(x, 0, z);

                    if (x * x + z * z <= mobSpawnRadius * mobSpawnRadius &&
                            level.getBlockState(currentPos).getCollisionShape(level, currentPos).isEmpty() &&
                            level.getBlockState(currentPos.above()).getCollisionShape(level, currentPos.above()).isEmpty() &&
                            level.getBlockState(currentPos.above(2)).getCollisionShape(level, currentPos.above(2)).isEmpty() &&
                            level.getBlockState(currentPos.north()).getCollisionShape(level, currentPos.north()).isEmpty() &&
                            level.getBlockState(currentPos.south()).getCollisionShape(level, currentPos.south()).isEmpty() &&
                            level.getBlockState(currentPos.east()).getCollisionShape(level, currentPos.east()).isEmpty() &&
                            level.getBlockState(currentPos.west()).getCollisionShape(level, currentPos.west()).isEmpty() &&
                            player.blockPosition().distSqr(currentPos) > spawnDistanceFromPlayer * spawnDistanceFromPlayer) {
                        validPositions.add(currentPos);
                    }
                }
            }
            return validPositions;
        }
    }

    public void applyGlowEffectToSummonedMobs(Player pPlayer) {
        glowingCounter++;

        if (glowingCounter > 4) {
            int duration = 12000;
            int amplifier = 0;
            glowingCounter = 0;

            int affectedMobs = 0;

            for (Entity mob : summonedMobs) {
                if (mob instanceof LivingEntity livingMob) {
                    livingMob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, amplifier, false, false));
                    affectedMobs++;
                }
            }
            if (Config.enableUnclaimedRewardMessage) {
                if (!firstMessageSent) {
                    pPlayer.sendSystemMessage(Component.translatable("message.skyarena.unclaimed_reward"));
                    firstMessageSent = true;
                } else {
                    pPlayer.sendOverlayMessage(Component.translatable("message.skyarena.mobs_glowing", affectedMobs));
                }
                putPlayerMessageTimestamps(pPlayer);
            }
        }
    }

    public boolean isBattleOngoing(int difficultyLevel) {
        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                return true;
            }
        }
        return presetWaves.containsKey(difficultyLevel);
    }

    public record LootReward(String rewardLootTable, int rewardCount) {}

    public LootReward getRewardFromDifficultyRanges(int difficultyLevel) {
        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                String lootTable = range.rewardLootTable;
                int count = range.rewardCount;
                return new LootReward(lootTable, count);
            }
        }
        return null;
    }

    public List<ExpandedMobInfo> getAvailableMobs(int difficultyLevel) {
        List<ExpandedMobInfo> result = new ArrayList<>();

        // Every statically listed mob (across ALL groups, whether or not they
        // are used at this difficulty) is off-limits for dynamic expansion, so
        // a mob can never join the pool twice with different settings.
        Set<String> usedMobIds = new HashSet<>();
        for (MobGroup g : mobGroups.values()) {
            if (g.mobValues != null) usedMobIds.addAll(g.mobValues.keySet());
        }

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);
            if (difficultyLevel >= start && difficultyLevel <= end) {
                for (String groupId : range.mobGroupsUsed) {
                    MobGroup group = mobGroups.get(groupId);
                    if (group == null) continue;

                    for (Map.Entry<String, Integer> entry : group.mobValues.entrySet()) {
                        String mobId = entry.getKey();
                        int cost = entry.getValue();

                        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(mobId))) continue;

                        ExpandedMobInfo info = new ExpandedMobInfo(
                                mobId,
                                cost,
                                group.squadSpawnChance,
                                group.squadSpawnSize,
                                group.statMultiplierCoefficient,
                                group.mobSpawnChance
                        );
                        result.add(info);
                    }

                    // 26.1.2 port: dynamically add every registered hostile
                    // mob from other mods that is not in a static list, so the
                    // arena fights the modpack's own monsters.
                    if (group.isDynamic()) {
                        appendDynamicMobs(result, group, usedMobIds);
                    }
                }
                break;
            }
        }

        return result;
    }

    /**
     * 26.1.2 port: expands a dynamic mob group with all registered hostile
     * entity types that are not statically listed and not excluded. Each mob
     * enters the pool only once (first dynamic group that admits it wins) and
     * gets an automatic cost derived from its base max health, attack damage
     * and armor.
     *
     * The group's dynamicMaxCost cap keeps mobs that are too strong out of
     * this tier: caps default to the highest statically curated cost of the
     * group, so the difficulty level ranges decide when strong modded mobs
     * start appearing (common -> elite -> minibosses -> bosses).
     */
    private void appendDynamicMobs(List<ExpandedMobInfo> result, MobGroup group, Set<String> usedMobIds) {
        int maxCost = group.getDynamicMaxCost();
        if (maxCost <= 0) {
            // Empty group without an explicit cap: the author listed no mobs
            // here on purpose, so the dynamic expansion stays off.
            return;
        }

        List<String> excludes = group.getExcludeMobs();
        List<String> excludeMods = group.getExcludeMods();
        boolean onlyModded = group.isOnlyModded();

        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String mobId = id.toString();

            if (usedMobIds.contains(mobId)) continue;
            if (excludes.contains(mobId)) continue;
            if (excludeMods.contains(id.getNamespace())) continue;
            if (onlyModded && id.getNamespace().equals("minecraft")) continue;

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type == null || !isHostile(type)) continue;

            int cost = computeDynamicCost(type, group);
            if (cost > maxCost) continue; // too strong for this difficulty tier

            // Mark as used so a mob admitted by an early tier is not added
            // again (with different settings) by a later tier in the same
            // difficulty range.
            usedMobIds.add(mobId);

            result.add(new ExpandedMobInfo(
                    mobId,
                    cost,
                    group.squadSpawnChance,
                    group.squadSpawnSize,
                    group.statMultiplierCoefficient,
                    group.mobSpawnChance
            ));
        }
    }

    /**
     * 26.1.2 port: a mob counts as hostile if the vanilla classification says
     * MONSTER, or if it uses a custom (NeoForge extensible) non-friendly
     * category, which is what some mob mods do for their own monsters.
     */
    private static boolean isHostile(EntityType<?> type) {
        MobCategory category = type.getCategory();
        return category == MobCategory.MONSTER || (!category.isFriendly() && category != MobCategory.MISC);
    }

    /**
     * 26.1.2 port: automatic point cost for a dynamically added mob, so
     * stronger modded mobs cost more points and are admitted only into
     * higher tiers:
     *
     *   cost = maxHealth / healthCostDivisor (default 3.0)
     *        + attackDamage * attackCostMultiplier (default 4.0)
     *        + armor * 2
     *
     * clamped to [1, 250]. Vanilla examples: zombie 19, creeper 19,
     * blaze 31, ravager 81, warden 287 -> 250.
     */
    @SuppressWarnings("unchecked")
    private static int computeDynamicCost(EntityType<?> type, MobGroup group) {
        try {
            if (DefaultAttributes.hasSupplier(type)) {
                AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
                double health = supplier.getBaseValue(Attributes.MAX_HEALTH);
                if (health > 0) {
                    double attack = supplier.hasAttribute(Attributes.ATTACK_DAMAGE)
                            ? supplier.getBaseValue(Attributes.ATTACK_DAMAGE) : 0.0D;
                    double armor = supplier.hasAttribute(Attributes.ARMOR)
                            ? supplier.getBaseValue(Attributes.ARMOR) : 0.0D;
                    double raw = health / group.getHealthCostDivisor()
                            + attack * group.getAttackCostMultiplier()
                            + armor * 2.0D;
                    int cost = (int) Math.round(raw);
                    return Math.max(1, Math.min(cost, 250));
                }
            }
        } catch (Exception ignored) {
            // Fall through to the flat default cost.
        }
        return group.getDefaultCost();
    }

    public void removeSummonedMobs() {
        for (Entity mob : summonedMobs) {
            if (mob != null && mob.isAlive()) {
                mob.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        summonedMobs.clear();
    }

    public int getPoints(Player player) {
        int difficulty = getDifficultyLevel(player);
        int completedLevels = difficulty - 1;
        int totalPoints = startingPoints;

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);

            if (completedLevels >= start) {
                int levelsPassedInRange = Math.min(completedLevels, end) - start + 1;
                totalPoints += levelsPassedInRange * range.pointsIncrease;
            }
        }

        return totalPoints;
    }

    public double getStatMultiplier(Player player) {
        int difficulty = getDifficultyLevel(player);
        int completedLevels = difficulty - 1;
        double totalStatMultiplier = startingStatMultiplier;

        for (DifficultyLevelRange range : difficultyLevelRanges) {
            int start = range.range.get(0);
            int end = range.range.get(1);

            if (completedLevels >= start) {
                int levelsPassedInRange = Math.min(completedLevels, end) - start + 1;
                totalStatMultiplier += levelsPassedInRange * range.statMultiplierIncrease;
            }
        }

        return totalStatMultiplier;
    }

    public int getDifficultyLevel(Player player) {
        if (individualPlayerStats) {
            String key = player.getGameProfile().name() + "_" + this.arenaType;
            return playerDifficulty.getOrDefault(key, 1);
        }
        return difficultyLevel;
    }

    public void increaseDifficultyLevel(Player player) {
        if (individualPlayerStats) {
            String key = player.getGameProfile().name() + "_" + this.arenaType;
            playerDifficulty.put(key, getDifficultyLevel(player) + 1);
        } else {
            this.difficultyLevel++;
        }

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void setDifficultyLevel(Player player, int level) {
        if (player == null) return;
        if (individualPlayerStats) {
            String key = player.getGameProfile().name() + "_" + this.arenaType;
            playerDifficulty.put(key, level);
        } else {
            this.difficultyLevel = level;
        }

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void setRecordItem(ItemStack stack) {
        if (JukeboxSong.fromStack(stack).isPresent()) {
            this.recordItem = stack.copy();

            if (this.level != null) {
                this.level.blockEntityChanged(this.getBlockPos());
            }
        }
    }

    public void clearRecordItem() {
        this.recordItem = ItemStack.EMPTY;

        if (this.level != null) {
            this.level.blockEntityChanged(this.getBlockPos());
        }
    }

    public void addSummonedMob(Entity mob) {
        summonedMobs.add(mob);
    }

    public boolean canSummonMobs() {
        summonedMobs.removeIf(Entity::isRemoved);
        return summonedMobs.isEmpty();
    }

    public void recordAltarActivation(Player player, BlockPos pos) {
        activeAltarBlocks.put(player, pos);
        activatingPlayer = player;
    }

    public static BlockPos getAltarPosForPlayer(Player player) {
        return activeAltarBlocks.get(player);
    }

    public void removeAltarActivationForPlayer() {
        activeAltarBlocks.remove(activatingPlayer);
    }

    public void putPlayerMessageTimestamps(Player player) {
        playerMessageTimestamps.put(player, level.getGameTime());
    }

    public boolean isResetDifficultyOnDefeat() {
        return resetDifficultyOnDefeat;
    }

    public boolean isAutoWaveRun() {
        return autoWaveRun;
    }

    public double getPointsCoefficientPerBlocks() {
        return pointsCoefficientPerBlocks;
    }

    public double getStatMultiplierCoefficientPerBlocks() {
        return statMultiplierCoefficientPerBlocks;
    }

    public double getLootTableCountCoefficientPerBlocks() {
        return lootTableCountCoefficientPerBlocks;
    }

    public int getFullProtectionRadius() {
        return fullProtectionRadius;
    }

    public int getStartingPoints() {
        return startingPoints;
    }

    public void setGlowingCounter(int glowingCounter) {
        this.glowingCounter = glowingCounter;
    }

    public void setPlayerDeath(boolean death) {
        playerDeath = death;
    }

    public int getBattleDifficultyLevel() {
        return battleDifficultyLevel;
    }

    public void setBattleDifficultyLevel(int battleDifficultyLevel) {
        this.battleDifficultyLevel = battleDifficultyLevel;
    }

    public String getArenaType() {
        return arenaType;
    }

    public boolean isBattlePhaseActive() {
        return battlePhaseActive;
    }

    public int getSpawnDistanceFromPlayer() {
        return spawnDistanceFromPlayer;
    }

    public int getBattleLossDistance() {
        return battleLossDistance;
    }

    public int getMobTeleportDistance() {
        return mobTeleportDistance;
    }

    public boolean isAllowWaterAndAirSpawn() {
        return allowWaterAndAirSpawn;
    }

    public boolean isIndividualPlayerStats() {
        return individualPlayerStats;
    }

    public int getMobSpawnRadius() {
        return mobSpawnRadius;
    }

    public int getMobCostRatio() {
        return mobCostRatio;
    }

    public boolean isSetNight() {
        return setNight;
    }

    public boolean isSetRain() {
        return setRain;
    }

    public boolean isDisableMobItemDrop() {
        return disableMobItemDrop;
    }

    public boolean isAllowDifficultyReset() {
        return allowDifficultyReset;
    }

    public int getMobGriefingProtectionRadius() {
        return mobGriefingProtectionRadius;
    }

    public int getBossBarHideRadius() {
        return bossBarHideRadius;
    }

    public Map<Integer, PresetWave> getPresetWaves() {
        return this.presetWaves;
    }

    public long getBattleEndTime() {
        return battleEndTime;
    }

    public void setBattleEndTime(long battleEndTime) {
        this.battleEndTime = battleEndTime;
    }

    public double getStartingStatMultiplier() {
        return startingStatMultiplier;
    }

    public List<DifficultyLevelRange> getDifficultyLevelRanges() {
        return difficultyLevelRanges;
    }

    public LinkedHashMap<String, MobGroup> getMobGroups() {
        return mobGroups;
    }

    public void setBattlePhaseActive(boolean battlePhaseActive) {
        this.battlePhaseActive = battlePhaseActive;
    }

    public Player getActivatingPlayer() {
        return activatingPlayer;
    }
}
