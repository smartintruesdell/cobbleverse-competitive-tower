package dev.cobbleverse.tower.mixin;

import battle.tower.battle.BattleTowerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = BattleTowerManager.class, remap = false)
public interface TowerRecovery {
    @Invoker("preparePlayerForBattleStart")
    static void competitive$recover(ServerPlayerEntity player) { throw new AssertionError("Mixin not applied"); }
}
