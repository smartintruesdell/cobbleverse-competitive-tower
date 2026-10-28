package dev.cobbleverse.tower.mixin;

import battle.tower.platform.fabric.NetworkHelperImpl;
import battle.tower.network.BattleTowerNetwork;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value=NetworkHelperImpl.class,remap=false)
public interface TowerLockAccess {
    @Invoker("handleLockInTeam")
    static void competitive$lock(ServerPlayerEntity player,BattleTowerNetwork.LockInTeamPayload payload) {throw new AssertionError("Mixin not applied");}
}
