package net.jrdemiurge.skyarena.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 26.1.2 port: SimpleCriterionTrigger moved to net.minecraft.advancements.criterion
 * and trigger instances are deserialized through a Codec instead of
 * createInstance(JsonObject, ...). This trigger matches any activation.
 */
public class DifficultyLevel1 extends SimpleCriterionTrigger<DifficultyLevel1.TriggerInstance> {
    public static final DifficultyLevel1 INSTANCE = new DifficultyLevel1();

    private DifficultyLevel1() {}

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer serverPlayer) {
        this.trigger(serverPlayer, TriggerInstance::matches);
    }

    public record TriggerInstance() implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = MapCodec.unitCodec(new TriggerInstance());

        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }

        public boolean matches() {
            return true;
        }
    }
}
