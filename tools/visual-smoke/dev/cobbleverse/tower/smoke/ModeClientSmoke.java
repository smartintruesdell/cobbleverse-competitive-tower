package dev.cobbleverse.tower.smoke;

import battle.tower.client.BattleTowerScreen;
import dev.cobbleverse.tower.client.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.gui.widget.ButtonWidget;
import java.lang.reflect.Field;

public final class ModeClientSmoke {
    int phase,ticks;Screen first;
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            try {
                if(phase==0 && c.currentScreen instanceof TitleScreen) {
                    phase=1;ticks=0;ConnectScreen.connect(c.currentScreen,c,ServerAddress.parse("127.0.0.1:25577"),new ServerInfo("Disposable mode test","127.0.0.1:25577",ServerInfo.ServerType.OTHER),false,null);return;
                }
                if(phase>0 && ++ticks>800)throw new AssertionError("Mode test stalled in phase "+phase);
                if(phase==1 && c.currentScreen instanceof BattleTowerScreen screen && TowerModeClient.available()) {
                    first=(Screen)screen;
                    for(int i=0;i<5;i++)click(screen,"battleModeHovered");
                    if(integer(screen,"selectedBattleMode")!=1 || ((TowerModeView)screen).competitiveTower$isVgc())throw new AssertionError("Four-choice cycle failed");
                    System.out.println("MODE_SELECTOR_CYCLE_PASS: Singles -> Doubles -> VGC -> Co-op -> Singles -> Doubles");
                    click(screen,"lockInHovered");phase=2;ticks=0;
                } else if(phase==2 && c.currentScreen instanceof BattleTowerScreen screen && bool(screen,"partyLockedIn") && !((TowerModeView)screen).competitiveTower$isVgc()) {
                    click(screen,"battleModeHovered");phase=3;ticks=0;
                } else if(phase==3 && c.currentScreen instanceof BattleTowerScreen screen && c.currentScreen!=first && bool(screen,"partyLockedIn") && ((TowerModeView)screen).competitiveTower$isVgc()) {
                    if(ticks<15)return;
                    ScreenshotRecorder.saveScreenshot(c.runDirectory,"tower-format-vgc-v030.png",c.getFramebuffer(),t->{});
                    System.out.println("MODE_REOPEN_PASS: fresh Tower screen displays locked VGC");
                    click(screen,"startBattleHovered");phase=4;ticks=0;
                } else if(phase==4 && c.currentScreen instanceof PreviewScreen screen) {
                    for(int slot:new int[]{5,2,6,1})((Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().startsWith("Slot "+slot+":")).findFirst().ifPresent(ButtonWidget::onPress);
                    ((Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().equals("Confirm")).findFirst().ifPresent(ButtonWidget::onPress);
                    phase=5;ticks=0;
                } else if(phase==5 && ticks==35 && c.currentScreen instanceof PreviewScreen screen) {
                    ((Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().equals("Confirm") && b.active).findFirst().ifPresent(ButtonWidget::onPress);
                    System.out.println("MODE_COOLDOWN_RETRY: resubmitted same selection after cooldown");
                } else if(phase==5 && ticks>150) {phase=6;c.scheduleStop();}
            } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        });
    }
    private static Field field(String name)throws ReflectiveOperationException {var f=BattleTowerScreen.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static int integer(Object screen,String name)throws ReflectiveOperationException{return field(name).getInt(screen);}
    private static boolean bool(Object screen,String name)throws ReflectiveOperationException{return field(name).getBoolean(screen);}
    private static void click(BattleTowerScreen screen,String target)throws ReflectiveOperationException {
        for(var f:BattleTowerScreen.class.getDeclaredFields())if(f.getType()==boolean.class && f.getName().endsWith("Hovered")){f.setAccessible(true);f.setBoolean(screen,false);}
        field(target).setBoolean(screen,true);((Screen)screen).mouseClicked(0,0,0);
    }
}
