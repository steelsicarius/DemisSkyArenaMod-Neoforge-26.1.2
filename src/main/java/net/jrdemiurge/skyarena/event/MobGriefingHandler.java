package net.jrdemiurge.skyarena.event;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.util.MobGriefingProtectionZones;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = SkyArena.MOD_ID)
public class MobGriefingHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        Vec3 explosionPos = event.getExplosion().center();
        BlockPos center = BlockPos.containing(explosionPos);

        if (MobGriefingProtectionZones.isInZone(level, center)) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onMobGriefingCheck(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();

        if (!level.isClientSide() && MobGriefingProtectionZones.isInZone(level, entity.blockPosition())) {
            // 26.1.2 port: the event no longer uses Event.Result; it exposes
            // setCanGrief directly.
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();

        if (!level.isClientSide() && MobGriefingProtectionZones.isInZone(level, event.getPos())) {
            event.setCanceled(true);
        }
    }
}
