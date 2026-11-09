package net.jrdemiurge.skyarena.block.entity;

import net.jrdemiurge.skyarena.SkyArena;
import net.jrdemiurge.skyarena.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SkyArena.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AltarBlockEntity>> ALTAR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("altar_block_entity",
                    () -> new BlockEntityType<>(AltarBlockEntity::new, Set.of(ModBlocks.ALTAR_BATTLE.get())));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
