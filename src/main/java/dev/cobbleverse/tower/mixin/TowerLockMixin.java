package dev.cobbleverse.tower.mixin;

import battle.tower.battle.BattleTowerManager;
import battle.tower.data.*;
import dev.cobbleverse.tower.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(value=BattleTowerManager.class,remap=false)
public abstract class TowerLockMixin {
    @WrapMethod(method="lockInTeam")
    private static boolean competitive$lock(ServerPlayerEntity player,BattleSettings settings,Operation<Boolean> original) {
        return TowerModes.lock(player,settings,original);
    }
    @WrapOperation(method="lockInTeam",at=@At(value="INVOKE",target="Lbattle/tower/data/PlayerTowerData;lockInTeam(Lbattle/tower/data/TeamSnapshot;Lbattle/tower/data/BattleSettings;)V"))
    private static void competitive$commit(PlayerTowerData data,TeamSnapshot snapshot,BattleSettings settings,Operation<Void> original) {
        TowerModes.commit(data);
        original.call(data,snapshot,settings);
    }
}
