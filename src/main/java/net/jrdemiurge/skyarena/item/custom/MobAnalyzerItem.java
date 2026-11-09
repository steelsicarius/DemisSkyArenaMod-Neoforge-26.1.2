package net.jrdemiurge.skyarena.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MobAnalyzerItem extends Item {
    public MobAnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (!pPlayer.level().isClientSide() && pPlayer instanceof ServerPlayer serverPlayer) {
            String entityId = pInteractionTarget.getEncodeId();
            serverPlayer.sendSystemMessage(Component.literal("§4Mob: §a" + entityId));

            Map<Holder<Attribute>, Double> attributes = new LinkedHashMap<>();

            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.MAX_HEALTH);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.ATTACK_DAMAGE);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.ATTACK_KNOCKBACK);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.ATTACK_SPEED);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.ARMOR);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.ARMOR_TOUGHNESS);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.FOLLOW_RANGE);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.KNOCKBACK_RESISTANCE);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.MOVEMENT_SPEED);
            addAttributeIfPresent(attributes, pInteractionTarget, Attributes.FLYING_SPEED);

            attributes.forEach((attr, value) -> {
                String formattedValue = String.format("%.2f", value);
                serverPlayer.sendSystemMessage(
                        Component.translatable(attr.value().getDescriptionId())
                                .withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(": §a" + formattedValue))
                );
            });

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void addAttributeIfPresent(Map<Holder<Attribute>, Double> attributes, LivingEntity entity, Holder<Attribute> attribute) {
        if (entity.getAttribute(attribute) != null) {
            attributes.put(attribute, entity.getAttributeValue(attribute));
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, TooltipDisplay pTooltipDisplay, Consumer<Component> pTooltipAdder, TooltipFlag pIsAdvanced) {
        pTooltipAdder.accept(Component.translatable("tooltip.skyarena.mob_analyzer"));
    }
}
