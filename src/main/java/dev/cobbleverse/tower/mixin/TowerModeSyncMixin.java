package dev.cobbleverse.tower.mixin;

import battle.tower.platform.fabric.NetworkHelperImpl;
import dev.cobbleverse.tower.TowerModes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=NetworkHelperImpl.class,remap=false)
public abstract class TowerModeSyncMixin {
    @Inject(method="sendDataToClient",at=@At("HEAD"))
    private static void competitive$sync(ServerPlayerEntity player,CallbackInfo ci) {TowerModes.sync(player,"");}
}
