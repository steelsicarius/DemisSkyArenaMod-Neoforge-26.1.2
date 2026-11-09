package net.jrdemiurge.skyarena.item.custom;

import net.jrdemiurge.skyarena.Config;
import net.jrdemiurge.skyarena.config.SkyArenaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class RewardKeyItem extends Item {
    public RewardKeyItem(Properties pProperties) {
        super(pProperties);
    }

    public static final Set<BlockPos> keyedChests = new HashSet<>();

    public static final List<String> BLOCK_ID_BLACKLIST = List.of(
            "lootr:lootr_chest",
            "lootr:lootr_inventory",
            "lootr:lootr_trapped_chest"
    );

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos positionClicked = pContext.getClickedPos();
        Player player = pContext.getPlayer();
        InteractionHand hand = pContext.getHand();

        if (!level.isClientSide() && player != null) {
            BlockEntity blockEntity = level.getBlockEntity(positionClicked);

            BlockState clickedBlockState = level.getBlockState(positionClicked);
            Identifier blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(clickedBlockState.getBlock());

            if (blockId != null && BLOCK_ID_BLACKLIST.contains(blockId.toString())) {
                return InteractionResult.PASS;
            }

            if (blockEntity instanceof ChestBlockEntity chestEntity) {

                if (Config.requireEmptyChest && !chestEntity.isEmpty()) {
                    return InteractionResult.FAIL;
                }

                chestEntity.clearContent();

                ItemStack itemStack = player.getItemInHand(hand);
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

                List<String> lootTables = SkyArenaConfig.configData.keys.getOrDefault(itemId, SkyArenaConfig.DEFAULT_KEY);

                List<Identifier> validLootTables = lootTables.stream()
                        .map(Identifier::parse)
                        .filter(id -> level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.LOOT_TABLE, id)) != LootTable.EMPTY)
                        .toList();

                if (!validLootTables.isEmpty()) {
                    Identifier randomLootTable = validLootTables.get(new Random().nextInt(validLootTables.size()));
                    System.out.println("Loot table: " + randomLootTable);
                    chestEntity.setLootTable(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.LOOT_TABLE, randomLootTable), player.getRandom().nextLong());
                    chestEntity.unpackLootTable(player);
                }

                int keyCount = itemStack.getCount();

                player.getCooldowns().addCooldown(player.getItemInHand(hand), 15);
                player.getItemInHand(hand).shrink(1);
                level.playSound(null, positionClicked, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 5.0F, 1.0F);

                if (keyCount == 1) {
                    keyedChests.add(positionClicked.immutable());
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, TooltipDisplay pTooltipDisplay, Consumer<Component> pTooltipAdder, TooltipFlag pIsAdvanced) {
        pTooltipAdder.accept(Component.translatable("tooltip.skyarena.reward_key"));
    }
}
