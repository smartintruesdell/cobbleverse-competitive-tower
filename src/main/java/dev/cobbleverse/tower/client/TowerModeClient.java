package dev.cobbleverse.tower.client;

import dev.cobbleverse.tower.TowerModeNetwork;
import dev.cobbleverse.tower.SheetNetwork;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class TowerModeClient {
    private TowerModeClient() {}
    private static TowerModeNetwork.State state;
    private static boolean open;
    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{state=null;open=false;});
        ClientPlayConnectionEvents.JOIN.register((h,s,c)->{state=null;open=false;});
        ClientPlayNetworking.registerGlobalReceiver(TowerModeNetwork.State.ID,(p,c)->c.client().execute(()->{
            open=false;apply(p);
        }));
        ClientPlayNetworking.registerGlobalReceiver(SheetNetwork.State.ID,(p,c)->c.client().execute(()->{
            open=p.open();apply(p.mode());
        }));
    }
    private static void apply(TowerModeNetwork.State p) {
            state=p;
            var c=MinecraftClient.getInstance();
            if(c.currentScreen instanceof TowerModeView view)view.competitiveTower$refreshMode();
            if(!p.message().isEmpty() && c.player!=null)c.player.sendMessage(Text.literal(p.message()),false);
    }
    public static boolean openSupported(){return ClientPlayNetworking.canSend(SheetNetwork.Lock.ID);}
    public static boolean lockedOpen(){return lockedCompetitive() && open;}
    public static boolean supported() {return ClientPlayNetworking.canSend(TowerModeNetwork.Lock.ID);}
    public static boolean available() {return supported() && state!=null && state.enabled();}
    public static boolean lockedCompetitive() {return state!=null && (state.locked() || state.active()) && state.competitive();}
    public static boolean ready() {return state!=null;}
    public static void request() {
        if(ClientPlayNetworking.canSend(TowerModeNetwork.Query.ID))ClientPlayNetworking.send(new TowerModeNetwork.Query());
    }
    public static void lock(String level,boolean legends,String battle,String team,int boss,boolean competitive) {
        lock(level,legends,battle,team,boss,competitive,false);
    }
    public static void lock(String level,boolean legends,String battle,String team,int boss,boolean competitive,boolean open) {
        var mode=new TowerModeNetwork.Lock(level,legends,battle,team,boss,competitive);
        if(openSupported())ClientPlayNetworking.send(new SheetNetwork.Lock(mode,open));
        else if(!open)ClientPlayNetworking.send(mode);
    }
}
