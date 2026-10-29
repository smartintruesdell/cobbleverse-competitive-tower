package dev.cobbleverse.tower.smoke;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleRegistry;
import battle.tower.battle.BattleTowerManager;
import battle.tower.data.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import java.util.*;

/** Runs only when explicitly enabled in the disposable test instance. */
public final class RealUiServer implements ModInitializer {
    UUID playerId; int ticks; List<UUID> partyIds,expectedNpc;
    public void onInitialize() {
        if(Boolean.getBoolean("ctower.modeTest")){new ModeServerSmoke().register();return;}
        if(!Boolean.getBoolean("ctower.realUiTest"))return;
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{if(h.player.getName().getString().equals("TowerUITest")){playerId=h.player.getUuid();ticks=0;}});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(playerId==null)return;var p=server.getPlayerManager().getPlayer(playerId);if(p==null)return;
            if(++ticks==60) {
                p.changeGameMode(GameMode.CREATIVE);
                BattleTowerManager.forfeitRun(p);
                var party=Cobblemon.INSTANCE.getStorage().getParty(p);party.clearParty();
                String[] species={"pikachu","charizard","rotom","garchomp","incineroar","amoonguss"};
                for(int i=0;i<6;i++)party.set(i,PokemonProperties.Companion.parse(species[i]+" level=50").create());
                String[] held={"leftovers","life_orb","choice_scarf","assault_vest","focus_sash","sitrus_berry"};
                for(int i=0;i<6;i++)party.get(i).swapHeldItem(new net.minecraft.item.ItemStack(Registries.ITEM.get(Identifier.of("cobblemon:"+held[i]))),false,false);
                var team=new ArrayList<com.cobblemon.mod.common.pokemon.Pokemon>();party.forEach(team::add);partyIds=team.stream().map(x->x.getUuid()).toList();
                server.getCommandManager().executeWithPrefix(p.getCommandSource(),"ctower on");
                var snapshot=new TeamSnapshot();snapshot.captureTeam(team,p.getUuid());
                var settings=new BattleSettings();settings.setBattleMode(BattleSettings.BattleMode.DOUBLES);
                if(Boolean.getBoolean("ctower.legacyClientTest")) {
                    if(dev.cobbleverse.tower.SheetNetwork.available(p))throw new AssertionError("Expected actual legacy client");
                    var mode=new dev.cobbleverse.tower.TowerModeNetwork.Lock("LEVEL_50",false,"DOUBLES","NORMAL",2,true);
                    var data=TowerDataManager.getPlayerData(p.getUuid());
                    dev.cobbleverse.tower.TowerModes.fromScreen(p,mode,true,true);
                    if(data.isPartyLockedIn())throw new AssertionError("Legacy client accepted open lock");
                    data.lockInTeam(snapshot,settings);
                    var flag=(dev.cobbleverse.tower.RunModeData)data;flag.competitiveTower$setMode(true);flag.competitiveTower$setOpen(true);
                    BattleTowerManager.startBattle(p,p.getBlockPos());
                    if(data.isRunActive() || BattleTowerManager.hasActiveSession(p.getUuid()))throw new AssertionError("Legacy client started persisted open run");
                    dev.cobbleverse.tower.TowerModes.fromScreen(p,mode);
                    if(!flag.competitiveTower$isOpen())throw new AssertionError("Legacy lock downgraded saved open run");
                    flag.competitiveTower$setOpen(false);
                    dev.cobbleverse.tower.TowerModes.fromScreen(p,mode);
                    if(!data.isPartyLockedIn() || flag.competitiveTower$isOpen())throw new AssertionError("Legacy closed lock failed");
                    System.out.println("LEGACY_CLIENT_GATE_PASS: real 0.4 client rejected for open lock/start/downgrade; closed VGC allowed");
                }
                TowerDataManager.getPlayerData(p.getUuid()).lockInTeam(snapshot,settings);
                ((dev.cobbleverse.tower.RunModeData)TowerDataManager.getPlayerData(p.getUuid())).competitiveTower$setMode(true);
                ((dev.cobbleverse.tower.RunModeData)TowerDataManager.getPlayerData(p.getUuid())).competitiveTower$setOpen(false);
                int testFloor=Integer.getInteger("ctower.testFloor",1);
                if(testFloor>1) {
                    var data=TowerDataManager.getPlayerData(p.getUuid());data.startRun();
                    try {var field=PlayerTowerData.class.getDeclaredField("currentFloor");field.setAccessible(true);field.setInt(data,testFloor);}
                    catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                }
                var pos=p.getBlockPos().add(2,0,0);p.getServerWorld().setBlockState(pos,Registries.BLOCK.get(Identifier.of("cobblemon_battle_tower:holo_battle_tower")).getDefaultState());
                BattleTowerManager.startBattle(p,pos);
                try {
                    var pendingField=dev.cobbleverse.tower.CompetitiveTower.class.getDeclaredField("pending");pendingField.setAccessible(true);
                    var pending=((Map<?,?>)pendingField.get(null)).get(p.getUuid());
                    var slotsField=pending.getClass().getDeclaredField("npcSlots");slotsField.setAccessible(true);
                    var opponentField=pending.getClass().getDeclaredField("opponent");opponentField.setAccessible(true);
                    var generated=(BattleTowerTeams.TeamResult)opponentField.get(pending);
                    int[] slots=(int[])slotsField.get(pending);
                    expectedNpc=dev.cobbleverse.tower.Selection.pick(Arrays.asList(generated.team),slots).stream().map(x->x.getUuid()).toList();
                    if(expectedNpc.size()!=4 || new HashSet<>(expectedNpc).size()!=4)throw new AssertionError("Invalid preselected NPC four");
                    System.out.println("NPC_PREVIEW_SELECTION_PASS: cached ordered NPC selection before player submission "+Arrays.toString(slots));
                }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
            }
            if(ticks>60 && BattleTowerManager.hasActiveSession(p.getUuid())) {
                var battle=BattleRegistry.getBattleByParticipatingPlayer(p);
                if(battle==null)throw new AssertionError("No registry battle");
                for(var actor:battle.getActors()) {
                    if(actor.getPokemonList().size()!=4)throw new AssertionError("Not 4v4");
                    if(actor.getUuid().equals(p.getUuid())) {
                        var actual=actor.getPokemonList().stream().map(x->x.getOriginalPokemon().getUuid()).toList();
                        if(!actual.equals(List.of(partyIds.get(4),partyIds.get(1),partyIds.get(5),partyIds.get(0))))throw new AssertionError("Wrong selected order");
                    } else {
                        var actual=actor.getPokemonList().stream().map(x->x.getOriginalPokemon().getUuid()).toList();
                        if(!actual.equals(expectedNpc))throw new AssertionError("NPC selected order changed before battle");
                        System.out.println("NPC_BATTLE_ORDER_PASS: selected four and opening pair preserved by RCT");
                    }
                }
                System.out.println("PHASE2_REAL_UI_PASS: server preview -> GUI confirm -> ordered 4v4 battle");
                BattleTowerManager.forfeitRun(p);
                var after=new ArrayList<UUID>();Cobblemon.INSTANCE.getStorage().getParty(p).forEach(x->after.add(x.getUuid()));
                if(!after.equals(partyIds))throw new AssertionError("Persistent party changed");
                System.out.println("PHASE2_CLEANUP_PASS: six party UUIDs/order preserved after forfeit");playerId=null;
            }
        });
    }
}
