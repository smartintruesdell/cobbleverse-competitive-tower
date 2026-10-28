package dev.cobbleverse.tower.smoke;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleRegistry;
import battle.tower.battle.BattleTowerManager;
import battle.tower.data.*;
import battle.tower.platform.NetworkHelper;
import dev.cobbleverse.tower.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import java.util.*;

/** Test-only real Tower UI transaction and persisted-format checks. */
public final class ModeServerSmoke {
    UUID owner;int ticks,stage;BlockPos terminal;List<UUID> ids;
    public void register() {
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{if(h.player.getName().getString().equals("TowerUITest")){owner=h.player.getUuid();ticks=0;stage=0;}});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(owner==null)return;var p=server.getPlayerManager().getPlayer(owner);if(p==null)return;
            var data=TowerDataManager.getPlayerData(owner);
            if(++ticks==60) {
                p.changeGameMode(GameMode.CREATIVE);BattleTowerManager.forfeitRun(p);
                var party=Cobblemon.INSTANCE.getStorage().getParty(p);party.clearParty();
                String[] species={"pikachu","charizard","rotom","garchomp","incineroar","amoonguss"};
                for(int i=0;i<6;i++)party.set(i,PokemonProperties.Companion.parse(species[i]+" level=50").create());
                var team=new ArrayList<com.cobblemon.mod.common.pokemon.Pokemon>();party.forEach(team::add);ids=team.stream().map(x->x.getUuid()).toList();
                // A previous global /ctower on must not turn the explicit ordinary Doubles choice into VGC.
                server.getCommandManager().executeWithPrefix(p.getCommandSource(),"ctower on");
                Boolean previous=((RunModeData)data).competitiveTower$getMode();
                var sixth=party.get(5);party.remove(sixth);
                TowerModes.fromScreen(p,new TowerModeNetwork.Lock("LEVEL_50",false,"DOUBLES","NORMAL",2,true));
                if(data.isPartyLockedIn() || !Objects.equals(previous,((RunModeData)data).competitiveTower$getMode()))throw new AssertionError("Five-member VGC changed lock state");
                party.set(5,sixth);
                TowerModes.fromScreen(p,new TowerModeNetwork.Lock("LEVEL_50",false,"SINGLES","NORMAL",2,true));
                if(data.isPartyLockedIn())throw new AssertionError("Invalid VGC Singles accepted");
                System.out.println("MODE_VALIDATION_PASS: insufficient party and malformed VGC request rejected");
                terminal=p.getBlockPos().add(2,0,0);p.getServerWorld().setBlockState(terminal,Registries.BLOCK.get(Identifier.of("cobblemon_battle_tower:holo_battle_tower")).getDefaultState());
                NetworkHelper.sendBattleTowerOpenScreen(p,terminal);stage=1;
            }
            if(stage==1 && data.isPartyLockedIn()) {
                if(TowerModes.competitive(owner) || !Boolean.FALSE.equals(((RunModeData)data).competitiveTower$getMode()))throw new AssertionError("Ordinary Doubles inherited legacy VGC");
                var copy=PlayerTowerData.fromNbt(owner,data.toNbt(server.getRegistryManager()),server.getRegistryManager());
                if(!Boolean.FALSE.equals(((RunModeData)copy).competitiveTower$getMode()))throw new AssertionError("Normal flag not persisted");
                System.out.println("MODE_NORMAL_LOCK_PASS: GUI ordinary Doubles overrides legacy preference and persists false");stage=2;
            }
            if(stage==2 && data.isPartyLockedIn() && TowerModes.competitive(owner)) {
                var copy=PlayerTowerData.fromNbt(owner,data.toNbt(server.getRegistryManager()),server.getRegistryManager());
                if(!Boolean.TRUE.equals(((RunModeData)copy).competitiveTower$getMode()))throw new AssertionError("VGC flag not persisted");
                System.out.println("MODE_VGC_PERSIST_PASS: GUI VGC lock persists with original Tower data");
                NetworkHelper.sendBattleTowerOpenScreen(p,terminal);stage=3;
            }
            if(stage==3 && BattleTowerManager.hasActiveSession(owner)) {
                var battle=BattleRegistry.getBattleByParticipatingPlayer(p);
                for(var actor:battle.getActors()) {
                    if(actor.getPokemonList().size()!=4)throw new AssertionError("VGC not 4v4");
                    if(actor.getUuid().equals(owner) && !actor.getPokemonList().stream().map(x->x.getOriginalPokemon().getUuid()).toList().equals(List.of(ids.get(4),ids.get(1),ids.get(5),ids.get(0))))throw new AssertionError("Wrong VGC order");
                }
                System.out.println("MODE_VGC_BATTLE_PASS: reopened Tower VGC selection starts ordered 4v4");
                BattleTowerManager.forfeitRun(p);
                TowerModes.fromScreen(p,new TowerModeNetwork.Lock("LEVEL_50",false,"DOUBLES","NORMAL",2,false));
                if(TowerModes.competitive(owner))throw new AssertionError("Could not leave VGC");
                // Avoid the standard one-second initiation cooldown before the regression battle.
                stage=4; ticks=0;
            }
            if(stage==4 && ticks==30) {
                BattleTowerManager.startBattle(p,terminal);
                var battle=BattleRegistry.getBattleByParticipatingPlayer(p);
                if(battle==null)throw new AssertionError("Normal battle missing");
                for(var actor:battle.getActors())if(actor.getPokemonList().size()!=6)throw new AssertionError("Ordinary Doubles not 6v6");
                BattleTowerManager.forfeitRun(p);
                var after=new ArrayList<UUID>();Cobblemon.INSTANCE.getStorage().getParty(p).forEach(x->after.add(x.getUuid()));
                if(!after.equals(ids))throw new AssertionError("Party UUIDs/order changed");
                System.out.println("MODE_SIX_V_SIX_PASS: returning to ordinary Doubles starts 6v6 and preserves six party members");
                owner=null;
            }
        });
    }
}
