package dev.cobbleverse.tower;

import battle.tower.data.*;
import battle.tower.platform.NetworkHelper;
import battle.tower.network.BattleTowerNetwork;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cobbleverse.tower.mixin.TowerLockAccess;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

/** Single server-authoritative lock transaction; the original Tower performs the lock and validation. */
public final class TowerModes {
    private TowerModes() {}
    private static final ThreadLocal<Intent> intent=new ThreadLocal<>();
    private static final class Intent {
        final UUID player;final boolean competitive,open;boolean accepted;
        Intent(UUID player,boolean competitive,boolean open){this.player=player;this.competitive=competitive;this.open=open;}
    }
    public static void fromScreen(ServerPlayerEntity player,TowerModeNetwork.Lock p) {
        fromScreen(player,p,false,false);
    }
    public static void fromScreen(ServerPlayerEntity player,TowerModeNetwork.Lock p,boolean open,boolean sheetsProtocol) {
        if((open && (!p.competitive() || !SheetNetwork.available(player)))
            || (!sheetsProtocol && open(player.getUuid()) && TowerDataManager.getPlayerData(player.getUuid()).isPartyLockedIn())) {
            sync(player,"This Open Team Sheets run requires an updated client. Finish or forfeit it before changing formats.");return;
        }
        if(!Set.of("LEVEL_50","LEVEL_100").contains(p.levelMode()) || !Set.of("SINGLES","DOUBLES","COOP_DOUBLES","BOSS").contains(p.battleMode())
            || !Set.of("NORMAL","RANDOM").contains(p.teamMode()) || p.bossPlayers()<1 || p.bossPlayers()>3
            || p.competitive() && !p.battleMode().equals("DOUBLES")) {sync(player,"Invalid Tower format request.");return;}
        Intent before=intent.get(), current=new Intent(player.getUuid(),p.competitive(),open);intent.set(current);
        try {
            TowerLockAccess.competitive$lock(player,new BattleTowerNetwork.LockInTeamPayload(p.levelMode(),p.legendaries(),p.battleMode(),p.teamMode(),p.bossPlayers()));
            if(!current.accepted) {
                NetworkHelper.sendDataToClient(player);
                sync(player,"Team not locked. Check the Tower requirements shown in chat.");
            }
        } finally {if(before==null)intent.remove();else intent.set(before);}
    }
    public static boolean lock(ServerPlayerEntity player,BattleSettings settings,Operation<Boolean> original) {
        Intent before=intent.get();
        boolean competitive=settings.getBattleMode()==BattleSettings.BattleMode.DOUBLES
            && (before!=null && before.player.equals(player.getUuid())?before.competitive:CompetitiveTower.legacyPreference(player.getUuid()));
        Intent current=before!=null && before.player.equals(player.getUuid())?before:new Intent(player.getUuid(),competitive,false);
        var data=TowerDataManager.getPlayerData(player.getUuid());
        if((current.open && !SheetNetwork.available(player)) || (before==null && open(player.getUuid()) && data.isPartyLockedIn())) {
            player.sendMessage(Text.literal("Open Team Sheets requires an updated client and explicit format selection."),false);return false;
        }
        if(competitive && (!CompetitiveTower.modeAvailable() || !CompetitiveTower.hasSixHealthy(player))) {
            player.sendMessage(Text.literal("Doubles VGC requires the enabled addon and six healthy Pokémon."),false);return false;
        }
        intent.set(current);
        try {boolean accepted=original.call(player,settings);current.accepted=accepted;return accepted;}
        finally {if(before==null)intent.remove();else intent.set(before);}
    }
    public static void commit(PlayerTowerData data) {
        Intent current=intent.get();
        if(current==null || !current.player.equals(data.getPlayerUUID()))throw new IllegalStateException("Tower lock lacks mode context");
        ((RunModeData)data).competitiveTower$setMode(current.competitive);
        ((RunModeData)data).competitiveTower$setOpen(current.open);
    }
    public static boolean competitive(UUID player) {
        var data=TowerDataManager.getPlayerData(player);
        Boolean stored=((RunModeData)data).competitiveTower$getMode();
        return data.getBattleSettings().getBattleMode()==BattleSettings.BattleMode.DOUBLES
            && (stored!=null?stored:CompetitiveTower.legacyPreference(player));
    }
    public static boolean open(UUID player) {
        return competitive(player) && ((RunModeData)TowerDataManager.getPlayerData(player)).competitiveTower$isOpen();
    }
    public static void sync(ServerPlayerEntity player,String message) {
        if(!ServerPlayNetworking.canSend(player,TowerModeNetwork.State.ID))return;
        var data=TowerDataManager.getPlayerData(player.getUuid());
        var state=new TowerModeNetwork.State(CompetitiveTower.modeAvailable(),competitive(player.getUuid()),data.isPartyLockedIn(),data.isRunActive(),message);
        if(ServerPlayNetworking.canSend(player,SheetNetwork.State.ID))
            ServerPlayNetworking.send(player,new SheetNetwork.State(state,open(player.getUuid())));
        else ServerPlayNetworking.send(player,state);
    }
}
