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
    UUID playerId; int ticks; List<UUID> partyIds;
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
                TowerDataManager.getPlayerData(p.getUuid()).lockInTeam(snapshot,settings);
                var pos=p.getBlockPos().add(2,0,0);p.getServerWorld().setBlockState(pos,Registries.BLOCK.get(Identifier.of("cobblemon_battle_tower:holo_battle_tower")).getDefaultState());
                BattleTowerManager.startBattle(p,pos);
            }
            if(ticks>60 && BattleTowerManager.hasActiveSession(p.getUuid())) {
                var battle=BattleRegistry.getBattleByParticipatingPlayer(p);
                if(battle==null)throw new AssertionError("No registry battle");
                for(var actor:battle.getActors()) {
                    if(actor.getPokemonList().size()!=4)throw new AssertionError("Not 4v4");
                    if(actor.getUuid().equals(p.getUuid())) {
                        var actual=actor.getPokemonList().stream().map(x->x.getOriginalPokemon().getUuid()).toList();
                        if(!actual.equals(List.of(partyIds.get(4),partyIds.get(1),partyIds.get(5),partyIds.get(0))))throw new AssertionError("Wrong selected order");
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
