package dev.cobbleverse.tower.client;

import dev.cobbleverse.tower.TowerModeNetwork;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class TowerModeClient {
    private TowerModeClient() {}
    private static TowerModeNetwork.State state;
    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->state=null);
        ClientPlayConnectionEvents.JOIN.register((h,s,c)->state=null);
        ClientPlayNetworking.registerGlobalReceiver(TowerModeNetwork.State.ID,(p,c)->c.client().execute(()->{
            state=p;
            if(c.client().currentScreen instanceof TowerModeView view)view.competitiveTower$refreshMode();
            if(!p.message().isEmpty() && c.client().player!=null)c.client().player.sendMessage(Text.literal(p.message()),false);
        }));
    }
    public static boolean supported() {return ClientPlayNetworking.canSend(TowerModeNetwork.Lock.ID);}
    public static boolean available() {return supported() && state!=null && state.enabled();}
    public static boolean lockedCompetitive() {return state!=null && (state.locked() || state.active()) && state.competitive();}
    public static boolean ready() {return state!=null;}
    public static void request() {
        if(ClientPlayNetworking.canSend(TowerModeNetwork.Query.ID))ClientPlayNetworking.send(new TowerModeNetwork.Query());
    }
    public static void lock(String level,boolean legends,String battle,String team,int boss,boolean competitive) {
        ClientPlayNetworking.send(new TowerModeNetwork.Lock(level,legends,battle,team,boss,competitive));
    }
}
