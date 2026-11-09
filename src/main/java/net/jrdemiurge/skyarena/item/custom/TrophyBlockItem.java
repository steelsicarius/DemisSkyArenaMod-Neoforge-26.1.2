package net.jrdemiurge.skyarena.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * 26.1.2 port: blocks no longer provide hover text (BlockBehaviour#appendHoverText
 * was removed); the trophy tooltip moved to its block item.
 */
public class TrophyBlockItem extends BlockItem {

    public TrophyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("tooltip.skyarena.trophy"));
    }
}
