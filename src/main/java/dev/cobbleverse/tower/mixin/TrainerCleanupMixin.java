package dev.cobbleverse.tower.mixin;

import battle.tower.BattleTower;
import battle.tower.entity.BattleTowerTrainerEntity;
import dev.cobbleverse.tower.CompetitiveTower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(value = BattleTower.class, remap = false)
public abstract class TrainerCleanupMixin {
    @Inject(method = "trackTrainerEntity", at = @At("HEAD"))
    private static void competitive$track(UUID owner, BattleTowerTrainerEntity entity, CallbackInfo ci) {
        CompetitiveTower.trackSpawn(owner,entity);
    }
}
