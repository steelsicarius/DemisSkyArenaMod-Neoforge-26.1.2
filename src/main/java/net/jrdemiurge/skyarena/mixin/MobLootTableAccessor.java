package net.jrdemiurge.skyarena.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/**
 * 26.1.2 port: the old NBT save/load round trip used to clear a mob's death
 * loot table cannot run on the new ValueIO entity storage, so the private
 * field is set directly.
 */
@Mixin(Mob.class)
public interface MobLootTableAccessor {
    @Accessor("lootTable")
    void skyarena$setDeathLootTable(Optional<ResourceKey<LootTable>> lootTable);
}
