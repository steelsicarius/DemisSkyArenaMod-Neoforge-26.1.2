package net.jrdemiurge.skyarena.item.custom;

import net.jrdemiurge.skyarena.util.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;

public class DungeonEyeItem extends Item {
    private final TagKey<Structure> structure;

    public DungeonEyeItem(Properties pProperties, TagKey<Structure> structure) {
        super(pProperties);
        this.structure = structure;
    }

    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (pLevel instanceof ServerLevel serverLevel) {
            BlockPos blockPos = serverLevel.findNearestMapStructure(structure, pPlayer.blockPosition(), 100, false);
            if (blockPos != null) {
                EyeOfEnder eyeOfEnder = new EyeOfEnder(pLevel, pPlayer.getX(), pPlayer.getY(0.5), pPlayer.getZ());
                eyeOfEnder.setItem(itemStack);
                eyeOfEnder.signalTo(blockPos.getCenter());
                pLevel.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnder.position(), GameEvent.Context.of(pPlayer));
                pLevel.addFreshEntity(eyeOfEnder);
                if (pPlayer instanceof ServerPlayer) {
                    CriteriaTriggers.USED_ENDER_EYE.trigger((ServerPlayer) pPlayer, blockPos);
                }

                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 0.5F, 0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F));
                pLevel.levelEvent(null, 1003, pPlayer.blockPosition(), 0);
                if (!pPlayer.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                pPlayer.awardStat(Stats.ITEM_USED.get(this));
                pPlayer.swing(pHand, true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.FAIL;
    }
}
