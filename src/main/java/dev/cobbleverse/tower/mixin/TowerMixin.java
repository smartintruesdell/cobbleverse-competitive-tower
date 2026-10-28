package dev.cobbleverse.tower.mixin;

import battle.tower.battle.BattleTowerManager;
import battle.tower.data.BattleTowerTeams;
import dev.cobbleverse.tower.CompetitiveTower;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.UUID;

@Mixin(value = BattleTowerManager.class, remap = false)
public abstract class TowerMixin {
    @WrapMethod(method = "startBattle")
    private static BattleTowerManager.BattleTowerSession competitive$start(ServerPlayerEntity player, BlockPos pos,
            Operation<BattleTowerManager.BattleTowerSession> original) {
        return CompetitiveTower.start(player,pos,original);
    }
    @Inject(method = "startSoloBattle", at = @At("HEAD"), cancellable = true)
    private static void competitive$preview(CallbackInfoReturnable<BattleTowerManager.BattleTowerSession> cir) {
        if (CompetitiveTower.pause()) cir.setReturnValue(null);
    }
    @WrapOperation(method = "startSoloBattle", at = @At(value = "INVOKE", target = "Lbattle/tower/data/BattleTowerTeams;getTeamForFloor(ILjava/util/UUID;)Lbattle/tower/data/BattleTowerTeams$TeamResult;"), require = 2)
    private static BattleTowerTeams.TeamResult competitive$normal(int floor, UUID player, Operation<BattleTowerTeams.TeamResult> original) {
        var team = CompetitiveTower.cachedTeam(); return team == null ? original.call(floor,player) : team;
    }
    @WrapOperation(method = "startSoloBattle", at = @At(value = "INVOKE", target = "Lbattle/tower/data/BattleTowerTeams;getRandomTeamForFloor(ILjava/util/UUID;Z)Lbattle/tower/data/BattleTowerTeams$TeamResult;"))
    private static BattleTowerTeams.TeamResult competitive$random(int floor, UUID player, boolean legendary, Operation<BattleTowerTeams.TeamResult> original) {
        var team = CompetitiveTower.cachedTeam(); return team == null ? original.call(floor,player,legendary) : team;
    }
    @WrapOperation(method = "startSoloBattle", at = @At(value = "INVOKE", target = "Lbattle/tower/data/BattleTowerTeams;getTeamByName(ILjava/lang/String;)Lbattle/tower/data/BattleTowerTeams$TeamResult;"))
    private static BattleTowerTeams.TeamResult competitive$named(int floor, String name, Operation<BattleTowerTeams.TeamResult> original) {
        var team = CompetitiveTower.cachedTeam(); return team == null ? original.call(floor,name) : team;
    }
    @Inject(method = "forfeitRun", at = @At("RETURN"))
    private static void competitive$forfeit(ServerPlayerEntity player, CallbackInfo ci) { CompetitiveTower.forget(player.getUuid()); }
}
