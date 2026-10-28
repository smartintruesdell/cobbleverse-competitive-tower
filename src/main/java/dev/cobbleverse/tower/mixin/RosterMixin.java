package dev.cobbleverse.tower.mixin;

import com.gitlab.srcmc.rctapi.api.battle.BattleManager;
import com.gitlab.srcmc.rctapi.api.trainer.Trainer;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.cobbleverse.tower.CompetitiveTower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(value = BattleManager.class, remap = false)
public abstract class RosterMixin {
    @WrapOperation(method = "toBattlePokemons", at = @At(value = "INVOKE", target = "Lcom/gitlab/srcmc/rctapi/api/trainer/Trainer;getTeam()[Lcom/cobblemon/mod/common/pokemon/Pokemon;"), require = 2)
    private static Pokemon[] competitive$roster(Trainer trainer, Operation<Pokemon[]> original) {
        return CompetitiveTower.roster(trainer,original.call(trainer));
    }
}
