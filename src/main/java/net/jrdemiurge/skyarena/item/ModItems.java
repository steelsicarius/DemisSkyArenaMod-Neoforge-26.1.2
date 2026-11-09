package net.jrdemiurge.skyarena.item;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.item.custom.DungeonEyeItem;
import net.jrdemiurge.skyarena.item.custom.MobAnalyzerItem;
import net.jrdemiurge.skyarena.item.custom.RewardKeyItem;
import net.jrdemiurge.skyarena.util.ModTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(SkyArena.MOD_ID);

    public static final DeferredItem<RewardKeyItem> FOREST_KEY = ITEMS.registerItem("forest_key", RewardKeyItem::new, net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<RewardKeyItem> CRIMSON_KEY = ITEMS.registerItem("crimson_key", RewardKeyItem::new, net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<RewardKeyItem> DESERT_KEY = ITEMS.registerItem("desert_key", RewardKeyItem::new, net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<RewardKeyItem> ICE_KEY = ITEMS.registerItem("ice_key", RewardKeyItem::new, net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<RewardKeyItem> ENDER_KEY = ITEMS.registerItem("ender_key", RewardKeyItem::new, net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<DungeonEyeItem> CRIMSON_EYE = ITEMS.registerItem("crimson_eye", p -> new DungeonEyeItem(p, ModTags.EYE_OF_SKY_LOCATED), net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<DungeonEyeItem> ICE_EYE = ITEMS.registerItem("ice_eye", p -> new DungeonEyeItem(p, ModTags.EYE_OF_ICE_LOCATED), net.minecraft.world.item.Item.Properties::new);

    public static final DeferredItem<MobAnalyzerItem> MOB_ANALYZER = ITEMS.registerItem("mob_analyzer", p -> new MobAnalyzerItem(p.stacksTo(1)), net.minecraft.world.item.Item.Properties::new);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
