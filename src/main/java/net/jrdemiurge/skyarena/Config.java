package net.jrdemiurge.skyarena;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SkyArena.MOD_ID)  // 26.1.2: no bus attribute; the event type selects the bus
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue REQUIRE_EMPTY_CHEST = BUILDER
            .comment("""
                    Description for customizing the skyarena.json config can be found at the following link:
                    https://github.com/JrDemiurg/DemisSkyArenaMod/wiki


                    If true, the reward key can only be used on empty chests.
                    If false, the chest's contents will be cleared before being filled with loot.""")
            .define("requireEmptyChest", false);

    private static final ModConfigSpec.BooleanValue ENABLE_LOSS_MESSAGE_LEAVE = BUILDER
            .comment("\nIf true, a defeat message will be shown when the player leaves the arena.")
            .define("enableLossMessageLeave", true);

    private static final ModConfigSpec.BooleanValue ENABLE_LOSS_MESSAGE_DEATH = BUILDER
            .comment("\nIf true, a defeat message will be shown when the player dies in battle.")
            .define("enableLossMessageDeath", true);

    private static final ModConfigSpec.BooleanValue ENABLE_UNCLAIMED_REWARD_MESSAGE = BUILDER
            .comment("""

                    If true, when mobs receive the Glowing effect, a message will appear recommending leaving the arena to restart the battle.
                    The message appears only once per game session.""")
            .define("enableUnclaimedRewardMessage", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean requireEmptyChest;
    public static boolean enableLossMessageLeave;
    public static boolean enableLossMessageDeath;
    public static boolean enableUnclaimedRewardMessage;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        requireEmptyChest = REQUIRE_EMPTY_CHEST.get();
        enableLossMessageLeave = ENABLE_LOSS_MESSAGE_LEAVE.get();
        enableLossMessageDeath = ENABLE_LOSS_MESSAGE_DEATH.get();
        enableUnclaimedRewardMessage = ENABLE_UNCLAIMED_REWARD_MESSAGE.get();
    }
}
