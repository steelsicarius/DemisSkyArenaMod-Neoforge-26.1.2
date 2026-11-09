package net.jrdemiurge.skyarena.config;

import java.util.LinkedHashMap;
import java.util.List;

public class MobGroup {
    public double squadSpawnChance;
    public int squadSpawnSize;
    public double statMultiplierCoefficient = 1.0;
    public double mobSpawnChance;
    public LinkedHashMap<String, Integer> mobValues;

    // ------------------------------------------------------------------
    // 26.1.2 port additions: dynamic mob pool expansion.
    //
    // All fields are nullable so an existing sky_arena.json without them
    // keeps working: missing = default.
    //
    // dynamic              (default true)  -> also offer every registered
    //                                          hostile mob that is not listed
    //                                          in any static mob list.
    // onlyModded           (default true)  -> only mobs whose namespace is
    //                                          not "minecraft" are added
    //                                          dynamically (vanilla pool stays
    //                                          exactly as the author curated
    //                                          it; set false to also include
    //                                          unlisted vanilla hostiles).
    // dynamicMaxCost       (default: the   -> dynamic mobs whose computed cost
    //                        highest static   is above this value never enter
    //                        cost in this     this group. This is what keeps
    //                        group; 0 when    heavy modded bosses out of the
    //                        the group is     low-difficulty tiers: a group
    //                        empty)           is only used by the difficulty
    //                                         ranges that list it, so cost
    //                                         caps per group = mobs appear
    //                                         according to difficulty.
    //                                         Set 0 to disable the dynamic
    //                                         expansion for this group; set a
    //                                         value on an EMPTY group to opt
    //                                         it in.
    // defaultCost          (default 10)    -> cost used when the mob has no
    //                                          default attributes.
    // healthCostDivisor    (default 3.0)   -> automatic cost weight.
    // attackCostMultiplier (default 4.0)   -> automatic cost weight.
    //
    // automatic cost = maxHealth / healthCostDivisor
    //                + attackDamage * attackCostMultiplier
    //                + armor * 2
    // clamped to [1, 250]. Example: a 400 HP / 15 ATK / 12 armor boss
    // (Legendary Monsters Possessed Paladin) costs 217 and can therefore
    // never join a "common" group capped at 60, but it fits "minibosses".
    // ------------------------------------------------------------------
    public Boolean dynamic;
    public Boolean onlyModded;
    public Integer dynamicMaxCost;
    public Integer defaultCost;
    public Double healthCostDivisor;
    public Double attackCostMultiplier;
    public List<String> excludeMobs;
    public List<String> excludeMods;

    public static final List<String> DEFAULT_EXCLUDES = List.of(
            "minecraft:ender_dragon",
            "minecraft:wither",
            "minecraft:giant",
            "minecraft:iron_golem",
            "minecraft:snow_golem"
    );

    public boolean isDynamic() {
        return dynamic == null || dynamic;
    }

    public boolean isOnlyModded() {
        return onlyModded == null || onlyModded;
    }

    /**
     * Cost cap for dynamically added mobs. When the field is missing, the
     * group inherits the highest cost the author statically curated into it
     * (an empty group therefore admits nothing, which keeps template arenas
     * like "additional_unused_mobs" parked).
     */
    public int getDynamicMaxCost() {
        if (dynamicMaxCost != null) return dynamicMaxCost;
        if (mobValues == null || mobValues.isEmpty()) return 0;
        int max = 0;
        for (Integer value : mobValues.values()) {
            if (value != null && value > max) max = value;
        }
        return max;
    }

    public int getDefaultCost() {
        return defaultCost != null && defaultCost > 0 ? defaultCost : 10;
    }

    public double getHealthCostDivisor() {
        return healthCostDivisor != null && healthCostDivisor > 0 ? healthCostDivisor : 3.0;
    }

    public double getAttackCostMultiplier() {
        return attackCostMultiplier != null && attackCostMultiplier > 0 ? attackCostMultiplier : 4.0;
    }

    public List<String> getExcludeMobs() {
        return excludeMobs != null ? excludeMobs : DEFAULT_EXCLUDES;
    }

    public List<String> getExcludeMods() {
        return excludeMods != null ? excludeMods : List.of();
    }
}
