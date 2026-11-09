package net.jrdemiurge.skyarena.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public class WitherBossMixin {

    // 26.1.2 port: dropCustomDeathLoot signature is now (ServerLevel, DamageSource, boolean).
    @Inject(method = "dropCustomDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("HEAD"), cancellable = true)
    private void onDropCustomDeathLoot(ServerLevel level, DamageSource pSource, boolean pRecentlyHit, CallbackInfo ci) {
        WitherBoss entity = (WitherBoss) (Object) this;

        Team team = entity.getTeam();
        String teamName = team != null ? team.getName() : "";
        if ("summonedByArenaWithoutLoot".equals(teamName)) {
            ci.cancel();
        }
    }
}
