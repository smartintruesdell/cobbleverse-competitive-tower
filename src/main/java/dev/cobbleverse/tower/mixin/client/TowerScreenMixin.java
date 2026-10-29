package dev.cobbleverse.tower.mixin.client;

import battle.tower.client.BattleTowerScreen;
import battle.tower.data.BattleSettings;
import battle.tower.network.BattleTowerNetwork;
import dev.cobbleverse.tower.ModeChoice;
import dev.cobbleverse.tower.client.*;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

/** Adds a virtual format while all original Tower indices and battle enums remain valid. */
@Mixin(value=BattleTowerScreen.class,remap=false)
public abstract class TowerScreenMixin implements TowerModeView {
    @Shadow private int selectedBattleMode;
    @Shadow private int selectedTeamMode;
    @Shadow private int selectedLevelMode;
    @Shadow private int selectedBossPlayerCount;
    @Shadow private boolean allowLegendaries;
    @Shadow private boolean runActive;
    @Shadow private boolean partyLockedIn;
    @Shadow private boolean battleModeHovered;
    @Shadow private void playClickSound(){throw new AssertionError();}
    @Shadow private boolean isPartyValidForSelectedMode(){throw new AssertionError();}
    @Shadow private String getSelectedTeamModeName(){throw new AssertionError();}
    @Unique private boolean competitiveTower$vgc;
    @Unique private boolean competitiveTower$open;
    @Override public boolean competitiveTower$isOpen(){return competitiveTower$isVgc() && competitiveTower$open;}

    @Override public boolean competitiveTower$isVgc() {
        return competitiveTower$vgc && selectedBattleMode==1 && selectedTeamMode!=2;
    }
    @Override public void competitiveTower$refreshMode() {
        if(TowerModeClient.ready() && (partyLockedIn || runActive)) {
            competitiveTower$vgc=selectedBattleMode==1 && selectedTeamMode!=2 && TowerModeClient.lockedCompetitive();
            competitiveTower$open=competitiveTower$vgc && TowerModeClient.lockedOpen();
        }
    }
    @Inject(method="method_25426",at=@At("TAIL"),remap=false)
    private void competitive$open(CallbackInfo ci){competitiveTower$refreshMode();TowerModeClient.request();}
    @Inject(method="updateRunState",at=@At("TAIL"),remap=false)
    private void competitive$state(CallbackInfo ci){competitiveTower$refreshMode();}
    @Inject(method="method_25402",at=@At("HEAD"),cancellable=true,remap=false)
    private void competitive$cycle(double x,double y,int button,CallbackInfoReturnable<Boolean> cir) {
        if(button!=0 || !battleModeHovered || runActive || selectedTeamMode==2 || !TowerModeClient.available())return;
        playClickSound();
        var next=new ModeChoice(selectedBattleMode,competitiveTower$isVgc(),competitiveTower$isOpen()).next(TowerModeClient.openSupported());
        selectedBattleMode=next.baseMode();competitiveTower$vgc=next.competitive();competitiveTower$open=next.open();
        if(partyLockedIn) {
            if(!isPartyValidForSelectedMode()) {
                BattleTowerNetwork.sendForfeitRun();partyLockedIn=false;
            } else TowerModeClient.lock(selectedLevelMode==1?"LEVEL_100":"LEVEL_50",allowLegendaries,
                selectedBattleMode==1?"DOUBLES":selectedBattleMode==2?"COOP_DOUBLES":"SINGLES",
                getSelectedTeamModeName(),selectedBossPlayerCount,competitiveTower$isVgc(),competitiveTower$isOpen());
        }
        cir.setReturnValue(true);
    }
    @Inject(method="method_25402",at=@At("RETURN"),remap=false)
    private void competitive$normalize(CallbackInfoReturnable<Boolean> cir) {
        if(selectedBattleMode!=1 || selectedTeamMode==2)competitiveTower$vgc=false;
    }
    @WrapOperation(method="drawSettingsSection",at=@At(value="INVOKE",target="Lbattle/tower/util/TranslationHelper;translate(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;",remap=false),remap=false)
    private MutableText competitive$label(String key,Object[] args,Operation<MutableText> original) {
        if(key.equals("gui.settings.mode.doubles") && competitiveTower$isVgc())return Text.literal(competitiveTower$isOpen()?"Doubles VGC Open":"Doubles VGC");
        return original.call(key,args);
    }
    @Inject(method="isPartyValidForSelectedMode",at=@At("HEAD"),cancellable=true,remap=false)
    private void competitive$partySize(CallbackInfoReturnable<Boolean> cir) {
        if(competitiveTower$isVgc())cir.setReturnValue(competitive$sixHealthy());
    }
    @Inject(method="getDisabledLockInTooltip",at=@At("HEAD"),cancellable=true,remap=false)
    private void competitive$requirements(CallbackInfoReturnable<List<Text>> cir) {
        if(competitiveTower$isVgc() && !competitive$sixHealthy())cir.setReturnValue(List.of(Text.literal("Doubles VGC requires six healthy Pokémon.")));
    }
    @Unique private boolean competitive$sixHealthy() {
        var party=CobblemonClient.INSTANCE.getStorage().getParty();
        return party!=null && party.getSlots().size()==6 && party.getSlots().stream().allMatch(p->p!=null && !p.isFainted());
    }
    @WrapOperation(method="method_25402",at=@At(value="INVOKE",target="Lbattle/tower/network/BattleTowerNetwork;sendLockInTeam(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;I)V"),require=4,remap=false)
    private void competitive$lock(String level,boolean legends,String battle,String team,int boss,Operation<Void> original) {
        if(TowerModeClient.supported())TowerModeClient.lock(level,legends,battle,team,boss,competitiveTower$isVgc(),competitiveTower$isOpen());
        else original.call(level,legends,battle,team,boss);
    }
}
