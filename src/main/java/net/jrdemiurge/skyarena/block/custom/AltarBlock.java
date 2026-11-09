package net.jrdemiurge.skyarena.block.custom;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.ModBlocks;
import net.jrdemiurge.skyarena.block.entity.AltarBlockEntity;
import net.jrdemiurge.skyarena.block.entity.ModBlockEntity;
import net.jrdemiurge.skyarena.config.*;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AltarBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    @Override
    public com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AltarBlock::new);
    }

    public AltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    /**
     * 26.1.2 port: Block.use() no longer exists. The interaction logic lives in
     * activate(); useWithoutItem is the vanilla hook that invokes it on both
     * sides (the client must consume the click for the interaction packet to be
     * sent to the server). The auto-wave scheduler calls activate() directly.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {
        return activate(pLevel, pPos, pState, pPlayer, InteractionHand.MAIN_HAND);
    }

    public InteractionResult activate(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer, InteractionHand pHand) {
        SkyArena.LOGGER.info("[SkyArena] activate() called at {} by {}", pPos, pPlayer.getName().getString());
        if (!pLevel.isClientSide()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);

            if (!(blockEntity instanceof AltarBlockEntity altarBlockEntity)) return InteractionResult.PASS;

            if (pPlayer.getItemInHand(pHand).getItem() == Blocks.BEDROCK.asItem()) {
                handleBedrockUse(pLevel, pPos, pPlayer, pHand, altarBlockEntity);
                return InteractionResult.SUCCESS;
            }

            if (pPlayer.getItemInHand(pHand).getItem() == ModItems.MOB_ANALYZER.get()) {
                showArenaInfo(pPlayer, altarBlockEntity);
                return InteractionResult.SUCCESS;
            }

            if (pPlayer.getItemInHand(pHand).getItem() == Items.STICK) {
                handleStickUse(pLevel, pPos, pPlayer, pHand, altarBlockEntity);
                return InteractionResult.SUCCESS;
            }

            if (JukeboxSong.fromStack(pPlayer.getItemInHand(pHand)).isPresent()) {
                handleRecordUse(pLevel, pPos, pPlayer, pHand, altarBlockEntity);
                return InteractionResult.SUCCESS;
            }

            if (pPlayer.getItemInHand(pHand).getItem() == Items.NETHERITE_INGOT) {
                return handleNetheriteIngotUse(pLevel, pPos, pPlayer, pHand, altarBlockEntity);
            }

            if (pLevel.getDifficulty() == Difficulty.PEACEFUL) {
                SkyArena.LOGGER.info("[SkyArena] activation blocked: peaceful difficulty");
                Component message = Component.translatable("message.skyarena.peaceful_disabled");
                pPlayer.sendOverlayMessage(message);
                altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
                return InteractionResult.SUCCESS;
            }

            // Reward granting
            if (altarBlockEntity.isBattlePhaseActive() && altarBlockEntity.canSummonMobs()) {
                handleGiveReward(altarBlockEntity, pLevel, pPos, pState, pPlayer);
                return InteractionResult.SUCCESS;
            }
            // Battle start
            if (!(altarBlockEntity.isBattlePhaseActive())) {

                int difficultyLevel = altarBlockEntity.getDifficultyLevel(pPlayer);

                if (!altarBlockEntity.isBattleOngoing(difficultyLevel)) {
                    handleMaxDifficultyLevel(pPlayer, altarBlockEntity);
                    return InteractionResult.SUCCESS;
                }

                if (pLevel.getGameTime() - altarBlockEntity.getBattleEndTime() < 30) {
                    SkyArena.LOGGER.info("[SkyArena] activation blocked: cooldown (gameTime={} battleEndTime={})",
                            pLevel.getGameTime(), altarBlockEntity.getBattleEndTime());
                    return InteractionResult.SUCCESS;
                }

                if (pPlayer instanceof ServerPlayer serverPlayer) {
                    UseAltarBattle.INSTANCE.trigger(serverPlayer);
                }

                List<BlockPos> validPositions = altarBlockEntity.findValidSpawnPositions(pLevel, pPos, pPlayer);
                SkyArena.LOGGER.info("[SkyArena] battle start: difficulty={} validPositions={}", difficultyLevel, validPositions.size());
                if (validPositions.isEmpty()) {
                    Component message = Component.translatable("message.skyarena.no_spawn_position");
                    pPlayer.sendOverlayMessage(message);
                    altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
                    return InteractionResult.SUCCESS;
                }

                altarBlockEntity.recordAltarActivation(pPlayer, pPos);

                altarBlockEntity.startMusic();

                if (!altarBlockEntity.isAutoWaveRun()) {
                    pLevel.playSound(null, pPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);
                } else {
                    pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);
                }

                setEnvironment(pLevel, pPlayer, altarBlockEntity.isSetNight(), altarBlockEntity.isSetRain());

                altarBlockEntity.setGlowingCounter(0);

                altarBlockEntity.setBattleDifficultyLevel(difficultyLevel);

                Component message = Component.translatable("message.skyarena.difficult_level")
                        .append(Component.literal(String.valueOf(difficultyLevel)));
                pPlayer.sendOverlayMessage(message);
                altarBlockEntity.putPlayerMessageTimestamps(pPlayer);

                String teamName = !altarBlockEntity.isDisableMobItemDrop() ? "summonedByArena" : "summonedByArenaWithoutLoot";
                PlayerTeam summonedMobsTeam = (PlayerTeam) pLevel.getScoreboard().getPlayerTeam(teamName);
                if (summonedMobsTeam == null) {
                    summonedMobsTeam = pLevel.getScoreboard().addPlayerTeam(teamName);
                    summonedMobsTeam.setAllowFriendlyFire(false);
                    summonedMobsTeam.setCollisionRule(Team.CollisionRule.NEVER);
                }

                double statMultiplier = altarBlockEntity.getStatMultiplier(pPlayer);
                double statCoefPerBlocks = altarBlockEntity.getStatMultiplierCoefficientPerBlocks();

                // Preset Wave
                Map<Integer, PresetWave> presetWaves = altarBlockEntity.getPresetWaves();
                if (presetWaves.containsKey(difficultyLevel) && presetWaves.get(difficultyLevel).mobStatMultiplier != 0) {
                    statMultiplier = presetWaves.get(difficultyLevel).mobStatMultiplier;
                }

                if (statCoefPerBlocks != 0) {
                    double distance = Math.sqrt(pPos.distSqr(BlockPos.ZERO));
                    double thousands = Math.floor(distance / 1000.0);
                    statMultiplier *= (1 + statCoefPerBlocks * thousands);
                }

                if (presetWaves.containsKey(difficultyLevel) && presetWaves.get(difficultyLevel).mobs != null) {
                    PresetWave wave = presetWaves.get(difficultyLevel);

                    for (WaveMob waveMob : wave.mobs) {
                        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(waveMob.type));
                        if (entityType == null) continue;

                        for (int i = 0; i < waveMob.count; i++) {
                            spawnArenaMob(pLevel, altarBlockEntity, entityType, validPositions, statMultiplier, waveMob.type, summonedMobsTeam);
                        }
                    }

                    if (!(altarBlockEntity.canSummonMobs())) {
                        altarBlockEntity.setBattlePhaseActive(true);
                    }

                    return InteractionResult.SUCCESS;
                }

                // Random Wave
                int remainingPoints = altarBlockEntity.getPoints(pPlayer);

                double pointsCoefPerBlocks = altarBlockEntity.getPointsCoefficientPerBlocks();
                if (pointsCoefPerBlocks != 0) {
                    double distance = Math.sqrt(pPos.distSqr(BlockPos.ZERO));
                    double thousands = Math.floor(distance / 1000.0);
                    remainingPoints = (int) (remainingPoints * (1 + pointsCoefPerBlocks * thousands));
                }

                int mobCostRatio = altarBlockEntity.getMobCostRatio();

                List<ExpandedMobInfo> availableMobs = altarBlockEntity.getAvailableMobs(altarBlockEntity.getBattleDifficultyLevel());

                if (availableMobs.isEmpty()) {
                    Component message_2 = Component.translatable("message.skyarena.available_mobs_is_empty");
                    pPlayer.sendOverlayMessage(message_2);
                    altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
                    return InteractionResult.SUCCESS;
                }

                int minMobValue = availableMobs.stream()
                        .mapToInt(mob -> mob.cost)
                        .min()
                        .orElse(Integer.MAX_VALUE);

                SkyArena.LOGGER.info("[SkyArena] random wave: points={} availableMobs={} minMobValue={}", remainingPoints, availableMobs.size(), minMobValue);
                int skipCount = 0;

                while (remainingPoints >= minMobValue) {
                    ExpandedMobInfo mobInfo = availableMobs.get(ThreadLocalRandom.current().nextInt(availableMobs.size()));

                    // chance to not spawn
                    if (mobInfo.mobSpawnChance < 1.0 && ThreadLocalRandom.current().nextDouble() > mobInfo.mobSpawnChance) {
                        continue;
                    }

                    int mobValue = mobInfo.cost;

                    // skip cheap mobs up to 5 times
                    if (mobValue < remainingPoints / mobCostRatio && skipCount < 6) {
                        skipCount++;
                        continue;
                    }
                    skipCount = 0;

                    if (remainingPoints >= mobValue) {
                        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobInfo.mobId));
                        if (entityType == null) continue;

                        boolean spawnSquad = ThreadLocalRandom.current().nextDouble() < mobInfo.squadSpawnChance;
                        int mobsToSpawn = spawnSquad ? mobInfo.squadSpawnSize : 1;

                        double actualMultiplier = statMultiplier * mobInfo.statMultiplierCoefficient;

                        for (int i = 0; i < mobsToSpawn; i++) {
                            if (remainingPoints < mobValue) break;

                            boolean success = spawnArenaMob(pLevel, altarBlockEntity, entityType, validPositions, actualMultiplier, mobInfo.mobId, summonedMobsTeam);
                            if (success) remainingPoints -= mobValue;
                        }
                    }
                }

                SkyArena.LOGGER.info("[SkyArena] random wave done: remainingPoints={} canSummonMobs={}", remainingPoints, altarBlockEntity.canSummonMobs());
                if (!(altarBlockEntity.canSummonMobs())) {
                    altarBlockEntity.setBattlePhaseActive(true);
                }

                return InteractionResult.SUCCESS;
            }

            altarBlockEntity.applyGlowEffectToSummonedMobs(pPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean spawnArenaMob(Level pLevel, AltarBlockEntity altarBlockEntity, EntityType<?> entityType, List<BlockPos> validPositions, double statMultiplier, String mobTypeString, PlayerTeam summonedMobsTeam) {
        Entity mob = entityType.create(pLevel, EntitySpawnReason.NATURAL);
        if (mob == null) return false;

        BlockPos spawnPos = validPositions.get(ThreadLocalRandom.current().nextInt(validPositions.size()));
        mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        if (mob instanceof Mob mobEntity) {
            if (altarBlockEntity.isDisableMobItemDrop()) {
                // 26.1.2 port: entity NBT is ValueIO based now; the empty death
                // loot table is injected through an accessor mixin instead of an
                // NBT save/load round trip.
                // Bugfix (port): use a pattern-match instead of a blind cast.
                // When the mixin config was not loaded (wrong [[mixins]] TOML
                // placement) this cast threw ClassCastException and aborted the
                // whole wave spawn; with instanceof the battle degrades
                // gracefully (loot not suppressed) instead of breaking.
                if (mobEntity instanceof net.jrdemiurge.skyarena.mixin.MobLootTableAccessor lootAccessor) {
                    lootAccessor.skyarena$setDeathLootTable(
                            java.util.Optional.of(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("empty"))));
                }
            }

            mobEntity.setPersistenceRequired();

            AttributeInstance healthAttribute = mobEntity.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                double baseHealth = healthAttribute.getBaseValue();
                healthAttribute.setBaseValue(baseHealth * statMultiplier);
                mobEntity.setHealth(mobEntity.getMaxHealth());
            }

            AttributeInstance attackAttribute = mobEntity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttribute != null) {
                double baseDamage = attackAttribute.getBaseValue();
                attackAttribute.setBaseValue(baseDamage * statMultiplier);
            }

            EntitySpawnReason spawnReason = EntitySpawnReason.NATURAL;

            if (mobTypeString.equals("cataclysm:scylla")
                    || mobTypeString.equals("cataclysm:netherite_monstrosity")) {
                spawnReason = EntitySpawnReason.COMMAND;
            }

            mobEntity.finalizeSpawn(
                    (ServerLevel) pLevel,
                    ((ServerLevel) pLevel).getCurrentDifficultyAt(mobEntity.blockPosition()),
                    spawnReason,
                    null
            );

            if (!mobTypeString.equals("born_in_chaos_v1:spiritof_chaos")) {
                pLevel.getScoreboard().addPlayerToTeam(mob.getStringUUID(), summonedMobsTeam);
            }
        }

        pLevel.addFreshEntity(mob);
        altarBlockEntity.addSummonedMob(mob);
        SkyArena.LOGGER.info("[SkyArena] Spawned arena mob {} at {}", mobTypeString, mob.blockPosition());
        return true;
    }

    public void showArenaInfo(Player pPlayer, AltarBlockEntity altarBlockEntity) {
        MutableComponent message = Component.literal("§4=== Arena Info ===\n");
        StringBuilder logMessage = new StringBuilder("=== Arena Info ===\n");

        int points = altarBlockEntity.getPoints(pPlayer);
        double pointsCoefPerBlocks = altarBlockEntity.getPointsCoefficientPerBlocks();
        if (pointsCoefPerBlocks != 0) {
            BlockPos pPos = altarBlockEntity.getBlockPos();
            double distance = Math.sqrt(pPos.distSqr(BlockPos.ZERO));
            double thousands = Math.floor(distance / 1000.0);
            points = (int) (points * (1 + pointsCoefPerBlocks * thousands));
        }

        double statMultiplier = altarBlockEntity.getStatMultiplier(pPlayer);
        double statCoefPerBlocks = altarBlockEntity.getStatMultiplierCoefficientPerBlocks();
        if (statCoefPerBlocks != 0) {
            BlockPos pPos = altarBlockEntity.getBlockPos();
            double distance = Math.sqrt(pPos.distSqr(BlockPos.ZERO));
            double thousands = Math.floor(distance / 1000.0);
            statMultiplier *= (1 + statCoefPerBlocks * thousands);
        }

        String[] lines = {
                "Arena Type: " + altarBlockEntity.getArenaType(),
                "Difficulty Level: " + altarBlockEntity.getDifficultyLevel(pPlayer),
                "Points: " + points,
                "Stat Multiplier: " + statMultiplier,
                "Starting Points: " + altarBlockEntity.getStartingPoints(),
                "Starting Stat Multiplier: " + altarBlockEntity.getStartingStatMultiplier(),
                "Mob Spawn Radius: " + altarBlockEntity.getMobSpawnRadius(),
                "Spawn Distance From Player: " + altarBlockEntity.getSpawnDistanceFromPlayer(),
                "Battle Loss Distance: " + altarBlockEntity.getBattleLossDistance(),
                "Mob Teleport Distance: " + altarBlockEntity.getMobTeleportDistance(),
                "Mob Griefing Protection Radius: " + altarBlockEntity.getMobGriefingProtectionRadius(),
                "Full Protection Radius: " + altarBlockEntity.getFullProtectionRadius(),
                "Boss Bar Hide Radius: " + altarBlockEntity.getBossBarHideRadius(),
                "Mob Cost Ratio: " + altarBlockEntity.getMobCostRatio(),
                "Allow Difficulty Reset: " + altarBlockEntity.isAllowDifficultyReset(),
                "Allow Water And Air Spawn: " + altarBlockEntity.isAllowWaterAndAirSpawn(),
                "Individual Player Stats: " + altarBlockEntity.isIndividualPlayerStats(),
                "Set Night: " + altarBlockEntity.isSetNight(),
                "Set Rain: " + altarBlockEntity.isSetRain(),
                "Disable Mob Item Drop: " + altarBlockEntity.isDisableMobItemDrop(),
                "Reset Difficulty On Defeat: " + altarBlockEntity.isResetDifficultyOnDefeat(),
                "Auto Wave Run: " + altarBlockEntity.isAutoWaveRun(),
                "Points Coefficient Per 1000 Blocks: " + altarBlockEntity.getPointsCoefficientPerBlocks(),
                "Stat Multiplier Coefficient Per 1000 Blocks: " + altarBlockEntity.getStatMultiplierCoefficientPerBlocks(),
                "Loot Table Count Coefficient Per 1000 Blocks: " + altarBlockEntity.getLootTableCountCoefficientPerBlocks()
        };

        for (String line : lines) {
            message = message.append(Component.literal("§6" + line.split(":")[0] + ": §a" + line.split(": ")[1] + "\n"));
            logMessage.append(line).append("\n");
        }

        // === Difficulty Level Ranges ===
        List<DifficultyLevelRange> ranges = altarBlockEntity.getDifficultyLevelRanges();
        if (!ranges.isEmpty()) {
            message = message.append(Component.literal("§4=== Difficulty Ranges ===\n"));
            logMessage.append("=== Difficulty Ranges ===\n");

            for (DifficultyLevelRange range : ranges) {
                String rangeStr = range.range.get(0) + "–" + range.range.get(1);
                message = message.append(Component.literal("§6Range: §a" + rangeStr + "\n"));
                logMessage.append("Range: ").append(rangeStr).append("\n");

                message = message.append(Component.literal("§7  Points Increase: §a" + range.pointsIncrease + "\n"));
                message = message.append(Component.literal("§7  Stat Multiplier Increase: §a" + range.statMultiplierIncrease + "\n"));
                message = message.append(Component.literal("§7  Reward Loot Table: §a" + range.rewardLootTable + "\n"));
                message = message.append(Component.literal("§7  Reward Count: §a" + range.rewardCount + "\n"));

                logMessage.append("  Points Increase: ").append(range.pointsIncrease).append("\n");
                logMessage.append("  Stat Multiplier Increase: ").append(range.statMultiplierIncrease).append("\n");
                logMessage.append("  Reward Loot Table: ").append(range.rewardLootTable).append("\n");
                logMessage.append("  Reward Count: ").append(range.rewardCount).append("\n");

                if (!range.mobGroupsUsed.isEmpty()) {
                    String joinedGroups = String.join(", ", range.mobGroupsUsed);
                    message = message.append(Component.literal("§7  Mob Groups: §e" + joinedGroups + "\n"));
                    logMessage.append("  Mob Groups: ").append(joinedGroups).append("\n");
                }
            }
        }

        // === Mob Groups ===
        Map<String, MobGroup> mobGroups = altarBlockEntity.getMobGroups();
        if (!mobGroups.isEmpty()) {
            message = message.append(Component.literal("§4=== Mob Groups ===\n"));
            logMessage.append("=== Mob Groups ===\n");

            for (Map.Entry<String, MobGroup> entry : mobGroups.entrySet()) {
                String groupId = entry.getKey();
                MobGroup group = entry.getValue();

                message = message.append(Component.literal("§6Group: §a" + groupId + "\n"));
                logMessage.append("Group: ").append(groupId).append("\n");

                message = message.append(Component.literal("§7  Squad Chance: §a" + group.squadSpawnChance + "\n"));
                message = message.append(Component.literal("§7  Squad Size: §a" + group.squadSpawnSize + "\n"));
                message = message.append(Component.literal("§7  Stat Multiplier Coefficient: §a" + group.statMultiplierCoefficient + "\n"));
                message = message.append(Component.literal("§7  Mob Spawn Chance: §a" + group.mobSpawnChance + "\n"));
                message = message.append(Component.literal("§7  Mobs: §8[list printed to console]\n"));

                logMessage.append("  Squad Chance: ").append(group.squadSpawnChance).append("\n");
                logMessage.append("  Squad Size: ").append(group.squadSpawnSize).append("\n");
                logMessage.append("  Stat Multiplier Coefficient: ").append(group.statMultiplierCoefficient).append("\n");
                logMessage.append("  Mob Spawn Chance: ").append(group.mobSpawnChance).append("\n");

                for (Map.Entry<String, Integer> mobEntry : group.mobValues.entrySet()) {
                    String mobId = mobEntry.getKey();
                    Integer mobValue = mobEntry.getValue();

                    logMessage.append("    - ").append(mobId).append(": ").append(mobValue).append("\n");
                }
            }
        }

        // === Preset Waves ===
        Map<Integer, PresetWave> presetWaves = altarBlockEntity.getPresetWaves();
        if (presetWaves != null && !presetWaves.isEmpty()) {
            message = message.append(Component.literal("§4=== Preset Waves ===\n"));
            logMessage.append("=== Preset Waves ===\n");

            for (Map.Entry<Integer, PresetWave> waveEntry : presetWaves.entrySet()) {
                int waveNumber = waveEntry.getKey();
                PresetWave wave = waveEntry.getValue();

                message = message.append(Component.literal("§6Wave " + waveNumber + ":\n"));
                logMessage.append("Wave ").append(waveNumber).append(":\n");

                message = message.append(Component.literal("§7  Stat Multiplier: §a" + wave.mobStatMultiplier + "\n"));
                logMessage.append("  Stat Multiplier: ").append(wave.mobStatMultiplier).append("\n");

                message = message.append(Component.literal("§7  Reward Loot Table: §a" + wave.rewardLootTable + "\n"));
                logMessage.append("  Reward Loot Table: ").append(wave.rewardLootTable).append("\n");

                message = message.append(Component.literal("§7  Reward Count: §a" + wave.rewardCount + "\n"));
                logMessage.append("  Reward Count: ").append(wave.rewardCount).append("\n");

                for (WaveMob mob : wave.mobs) {
                    message = message.append(Component.literal("§7    - §6" + mob.type + "§7 x§a" + mob.count + "\n"));
                    logMessage.append("    - ").append(mob.type).append(" x").append(mob.count).append("\n");
                }
            }
        }

        int linesCount = message.getString().split("\n").length;
        if (linesCount > 90) {
            message = message.append(Component.literal("§cToo much info! Check console for full details."));
        }

        pPlayer.sendSystemMessage(message);
        SkyArena.LOGGER.info(logMessage.toString());
    }

    private void handleBedrockUse(Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, AltarBlockEntity altarBlockEntity) {
        if (altarBlockEntity.isBattlePhaseActive()) {
            Component message = Component.translatable("message.skyarena.cannot_do_during_battle");
            pPlayer.sendOverlayMessage(message);
            altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
            return;
        }

        altarBlockEntity.switchToNextArena();

        Component message = Component.literal(altarBlockEntity.getArenaType());
        pPlayer.sendOverlayMessage(message);
        altarBlockEntity.putPlayerMessageTimestamps(pPlayer);

        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void handleStickUse(Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, AltarBlockEntity altarBlockEntity) {
        altarBlockEntity.clearRecordItem();
        altarBlockEntity.stopMusic();
        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (pPlayer instanceof ServerPlayer serverPlayer) {
            UseStick.INSTANCE.trigger(serverPlayer);
        }
    }

    private void handleRecordUse(Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, AltarBlockEntity altarBlockEntity) {
        altarBlockEntity.setRecordItem(pPlayer.getItemInHand(pHand));
        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (altarBlockEntity.isBattlePhaseActive()) {
            altarBlockEntity.stopMusic();
            altarBlockEntity.startMusic();
        }

        if (pPlayer instanceof ServerPlayer serverPlayer) {
            UseMusicDisk.INSTANCE.trigger(serverPlayer);
        }
    }

    private InteractionResult handleNetheriteIngotUse(Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, AltarBlockEntity altarBlockEntity) {
        if (!altarBlockEntity.isAllowDifficultyReset()) {
            Component message = Component.translatable("message.skyarena.cannot_reset_difficulty");
            pPlayer.sendOverlayMessage(message);
            altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
            return InteractionResult.PASS;
        }

        if (altarBlockEntity.isBattlePhaseActive()) {
            Component message = Component.translatable("message.skyarena.cannot_do_during_battle");
            pPlayer.sendOverlayMessage(message);
            altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
            return InteractionResult.PASS;
        }

        if (pPlayer.getCooldowns().isOnCooldown(pPlayer.getItemInHand(pHand))) {
            return InteractionResult.PASS;
        }

        altarBlockEntity.setDifficultyLevel(pPlayer, 1);

        pPlayer.getItemInHand(pHand).shrink(1);

        pPlayer.getCooldowns().addCooldown(pPlayer.getItemInHand(pHand), 40);

        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);

        Component message = Component.translatable("message.skyarena.points_reset");
        pPlayer.sendOverlayMessage(message);
        altarBlockEntity.putPlayerMessageTimestamps(pPlayer);

        if (pPlayer instanceof ServerPlayer serverPlayer) {
            UseNetheriteIngot.INSTANCE.trigger(serverPlayer);
        }

        return InteractionResult.SUCCESS;
    }

    private void handleMaxDifficultyLevel(Player pPlayer, AltarBlockEntity altarBlockEntity) {
        Component message;

        if (ThreadLocalRandom.current().nextBoolean()) {
            message = Component.translatable("message.skyarena.max_difficult_level_1");
        } else {
            message = Component.translatable("message.skyarena.max_difficult_level_2");
        }

        pPlayer.sendOverlayMessage(message);
        altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
    }

    private void handleGiveReward(AltarBlockEntity altarBlockEntity, Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!altarBlockEntity.isAutoWaveRun())
            altarBlockEntity.removeAltarActivationForPlayer();

        handleVictoryTriggers(altarBlockEntity, pPlayer);

        if (altarBlockEntity.isBattleOngoing(altarBlockEntity.getDifficultyLevel(pPlayer) + 1)) {
            pPlayer.sendOverlayMessage(Component.translatable("message.skyarena.victory"));
            altarBlockEntity.putPlayerMessageTimestamps(pPlayer);
        } else {
            if (pPlayer instanceof ServerPlayer serverPlayer) {
                Component title = Component.translatable("message.skyarena.victory");
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(title));
                serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 10));
            }
        }

        int difficultyLevel = altarBlockEntity.getBattleDifficultyLevel(); // Current difficulty level

        if (difficultyLevel == 50) sendDiscordInvite(pPlayer);

        int keyCount = 1;
        String rewardLootTableId = "minecraft:empty";

        Map<Integer, PresetWave> presetWaves = altarBlockEntity.getPresetWaves();

        AltarBlockEntity.LootReward reward = altarBlockEntity.getRewardFromDifficultyRanges(difficultyLevel);
        if (reward != null) {
            rewardLootTableId = reward.rewardLootTable();
            keyCount = reward.rewardCount();
        }

        if (presetWaves.containsKey(difficultyLevel)) {
            PresetWave wave = presetWaves.get(difficultyLevel);
            rewardLootTableId = wave.rewardLootTable;
            keyCount = wave.rewardCount;
        }

        double lootCountCoefPerBlocks = altarBlockEntity.getLootTableCountCoefficientPerBlocks();
        if (lootCountCoefPerBlocks != 0) {
            double distance = Math.sqrt(pPos.distSqr(BlockPos.ZERO));
            double thousands = Math.floor(distance / 1000.0);

            double newKeyCount = keyCount * (1 + lootCountCoefPerBlocks * thousands);
            int guaranteed = (int) Math.floor(newKeyCount);
            double fraction = newKeyCount - guaranteed;
            if (pLevel.getRandom().nextDouble() < fraction) {
                guaranteed++;
            }

            keyCount = guaranteed;
        }


        if (pLevel instanceof ServerLevel serverLevel) {
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(rewardLootTableId)));

            if (lootTable == LootTable.EMPTY && !rewardLootTableId.equals("minecraft:empty")) {
                rewardLootTableId = "skyarena:battle_rewards/crimson_key";
                lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(rewardLootTableId)));
            }

            if (lootTable != LootTable.EMPTY) {
                LootParams lootParams = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, pPlayer.position())
                        .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
                        .create(LootContextParamSets.GIFT);

                for (int i = 0; i < keyCount; i++) {
                    lootTable.getRandomItems(lootParams, stack -> {
                        ItemEntity rewardEntity = new ItemEntity(
                                pLevel,
                                pPlayer.getX(),
                                pPlayer.getY(),
                                pPlayer.getZ(),
                                stack
                        );
                        pLevel.addFreshEntity(rewardEntity);
                    });
                }
            }
        }

        if (altarBlockEntity.getBattleDifficultyLevel() >= altarBlockEntity.getDifficultyLevel(pPlayer)) {
            altarBlockEntity.increaseDifficultyLevel(pPlayer);
        }
        // Play sound
        if (!altarBlockEntity.isAutoWaveRun()) {
            pLevel.playSound(null, pPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        // Switch battle phase
        altarBlockEntity.setBattlePhaseActive(false);
        altarBlockEntity.stopMusic();
        altarBlockEntity.setBattleEndTime(pLevel.getGameTime());
    }

    public void sendDiscordInvite(Player player) {
        Style discordStyle = Style.EMPTY
                .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://discord.gg/ZMKnAB92GZ")))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("message.skyarena.discord.hover")))
                .withColor(ChatFormatting.BLUE)
                .withUnderlined(true);

        Component discord = Component.literal("Discord").withStyle(discordStyle);

        player.sendSystemMessage(Component.translatable("message.skyarena.discord.line1"));
        player.sendSystemMessage(Component.translatable("message.skyarena.discord.line2"));
        player.sendSystemMessage(Component.translatable("message.skyarena.discord.line3", discord));
    }

    private void handleVictoryTriggers(AltarBlockEntity altarBlockEntity, Player pPlayer) {
        int difficultyLevel = altarBlockEntity.getDifficultyLevel(pPlayer);

        if (pPlayer instanceof ServerPlayer serverPlayer) {
            DifficultyLevel1.INSTANCE.trigger(serverPlayer);
            if (difficultyLevel >= 1) {
                DifficultyLevel1.INSTANCE.trigger(serverPlayer);
            }
            if (difficultyLevel >= 5) {
                DifficultyLevel5.INSTANCE.trigger(serverPlayer);
            }
            if (difficultyLevel >= 10) {
                DifficultyLevel10.INSTANCE.trigger(serverPlayer);
            }
            if (difficultyLevel >= 20) {
                DifficultyLevel20.INSTANCE.trigger(serverPlayer);
            }
            if (difficultyLevel >= 50) {
                DifficultyLevel50.INSTANCE.trigger(serverPlayer);
            }
            if (difficultyLevel >= 100) {
                DifficultyLevel100.INSTANCE.trigger(serverPlayer);
            }
        }
    }

    private void setEnvironment(Level pLevel, Player pPlayer, boolean isNight, boolean isRain) {
        if (isNight) {
            setNightTime(pLevel, pPlayer);
        }

        if (isRain) {
            setRain(pLevel);
        }
    }

    private void setNightTime(Level pLevel, Player pPlayer) {
        if (!(pLevel instanceof ServerLevel serverLevel)) return;

        // 26.1.2 port: Level#setDayTime is gone. The world time is managed by
        // the clock system; setDayTime(t) is replicated as
        // setTotalTicks(total - total%24000 + t%24000) on the overworld clock.
        ServerClockManager clockManager = serverLevel.clockManager();
        Holder<WorldClock> overworldClock = serverLevel.registryAccess()
                .lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);

        long currentTotal = clockManager.getTotalTicks(overworldClock);
        long dayProgress = currentTotal % 24000; // Time inside the current day

        long newTotal = currentTotal - dayProgress + 18000; // Move to 18000 inside the current day
        if (dayProgress <= 18000 && currentTotal > 24000) {
            newTotal -= 24000; // If already night, move to the previous one
        }

        for (ServerLevel serverlevel : pLevel.getServer().getAllLevels()) {
            serverlevel.clockManager().setTotalTicks(overworldClock, newTotal);
        }
    }

    private void setRain(Level pLevel) {
        // 26.1.2 port: setWeatherParameters is gone; rain is controlled through
        // the per-level WeatherData saved data.
        ServerLevel serverLevel = (ServerLevel) pLevel;
        WeatherData weatherData = serverLevel.getWeatherData();
        weatherData.setClearWeatherTime(0);
        weatherData.setRainTime(6000); // Rain for 5 minutes
        weatherData.setRaining(true);
        weatherData.setThundering(false);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AltarBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 5, 14, 16, 11);
    private static final VoxelShape SHAPE_WEST = Block.box(5, 0, 2, 11, 16, 14);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);

        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_NORTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (pos.getY() < level.getMaxY() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        } else {
            return null; // No room to place two blocks
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    public boolean canPlaceLiquid(net.minecraft.world.entity.LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);

        Direction facing = state.getValue(FACING);
        boolean isWaterAbove = level.getBlockState(pos.above()).getBlock() == Blocks.WATER;

        BlockPos posAbove = pos.above();
        BlockState topBlockState = ModBlocks.ALTAR_BATTLE_TOP.get().defaultBlockState()
                .setValue(AltarBlockTop.FACING, facing)
                .setValue(AltarBlockTop.WATERLOGGED, isWaterAbove);
        level.setBlock(posAbove, topBlockState, 3);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AltarBlockEntity altarBlockEntity) {
            altarBlockEntity.loadArenaConfig(altarBlockEntity.getArenaType());
        }
    }

    // 26.1.2 port: BlockBehaviour#onRemove no longer exists; the pair break
    // behaviour (removing one half destroys the other) is achieved by
    // returning air from updateShape when the counterpart disappears.
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (facing == Direction.UP && !(facingState.getBlock() instanceof AltarBlockTop)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, currentPos, facing, facingPos, facingState, random);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }
}
