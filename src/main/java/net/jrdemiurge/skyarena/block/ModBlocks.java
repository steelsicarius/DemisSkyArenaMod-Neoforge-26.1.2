package net.jrdemiurge.skyarena.block;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.custom.AltarBlock;
import net.jrdemiurge.skyarena.block.custom.AltarBlockTop;
import net.jrdemiurge.skyarena.block.custom.Trophy;
import net.jrdemiurge.skyarena.item.ModItems;
import net.jrdemiurge.skyarena.item.custom.TrophyBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(SkyArena.MOD_ID);

    public static final DeferredBlock<AltarBlock> ALTAR_BATTLE = BLOCKS.registerBlock("altar_battle",
            AltarBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
                    .strength(-1.0F, 3600000.0F)  // Hardness like bedrock
            );

    public static final DeferredBlock<AltarBlockTop> ALTAR_BATTLE_TOP = BLOCKS.registerBlock("altar_battle_top",
            AltarBlockTop::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
                    .strength(-1.0F, 3600000.0F)  // Hardness like bedrock
            );

    public static final DeferredBlock<Trophy> OAK_TROPHY = BLOCKS.registerBlock("oak_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    public static final DeferredBlock<Trophy> STONE_TROPHY = BLOCKS.registerBlock("stone_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    public static final DeferredBlock<Trophy> IRON_TROPHY = BLOCKS.registerBlock("iron_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    public static final DeferredBlock<Trophy> GOLD_TROPHY = BLOCKS.registerBlock("gold_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    public static final DeferredBlock<Trophy> DIAMOND_TROPHY = BLOCKS.registerBlock("diamond_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    public static final DeferredBlock<Trophy> NETHERITE_TROPHY = BLOCKS.registerBlock("netherite_trophy",
            Trophy::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .noOcclusion()
                    .lightLevel((state) -> 5)
            );

    static {
        // 26.1.2 port: plain blocks use registerSimpleBlockItem; trophies use a
        // custom BlockItem because block hover text was removed from Block.
        ModItems.ITEMS.registerSimpleBlockItem("altar_battle", ALTAR_BATTLE);
        ModItems.ITEMS.registerSimpleBlockItem("altar_battle_top", ALTAR_BATTLE_TOP);
        ModItems.ITEMS.registerItem("oak_trophy", p -> new TrophyBlockItem(OAK_TROPHY.get(), p), Item.Properties::new);
        ModItems.ITEMS.registerItem("stone_trophy", p -> new TrophyBlockItem(STONE_TROPHY.get(), p), Item.Properties::new);
        ModItems.ITEMS.registerItem("iron_trophy", p -> new TrophyBlockItem(IRON_TROPHY.get(), p), Item.Properties::new);
        ModItems.ITEMS.registerItem("gold_trophy", p -> new TrophyBlockItem(GOLD_TROPHY.get(), p), Item.Properties::new);
        ModItems.ITEMS.registerItem("diamond_trophy", p -> new TrophyBlockItem(DIAMOND_TROPHY.get(), p), Item.Properties::new);
        ModItems.ITEMS.registerItem("netherite_trophy", p -> new TrophyBlockItem(NETHERITE_TROPHY.get(), p), Item.Properties::new);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
