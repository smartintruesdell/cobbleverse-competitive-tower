package dev.cobbleverse.tower.mixin;

import battle.tower.data.PlayerTowerData;
import dev.cobbleverse.tower.RunModeData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value=PlayerTowerData.class,remap=false)
public abstract class RunModeMixin implements RunModeData {
    @Unique private Boolean competitiveTower$mode;
    @Unique private boolean competitiveTower$open;
    @Override public boolean competitiveTower$isOpen(){return competitiveTower$open;}
    @Override public void competitiveTower$setOpen(boolean open){competitiveTower$open=open;}
    @Override public Boolean competitiveTower$getMode() {return competitiveTower$mode;}
    @Override public void competitiveTower$setMode(Boolean competitive) {competitiveTower$mode=competitive;}
    @Inject(method="toNbt",at=@At("RETURN"))
    private void competitive$save(CallbackInfoReturnable<NbtCompound> cir) {
        cir.getReturnValue().putBoolean("cobbleverseOpenSheets",competitiveTower$open);
        if(competitiveTower$mode!=null)cir.getReturnValue().putBoolean("cobbleverseCompetitive",competitiveTower$mode);
    }
    @Inject(method="fromNbt",at=@At("RETURN"))
    private static void competitive$load(UUID owner,NbtCompound tag,RegistryWrapper.WrapperLookup registries,CallbackInfoReturnable<PlayerTowerData> cir) {
        ((RunModeData)cir.getReturnValue()).competitiveTower$setOpen(tag.getBoolean("cobbleverseOpenSheets"));
        ((RunModeData)cir.getReturnValue()).competitiveTower$setMode(tag.contains("cobbleverseCompetitive")?tag.getBoolean("cobbleverseCompetitive"):null);
    }
}
