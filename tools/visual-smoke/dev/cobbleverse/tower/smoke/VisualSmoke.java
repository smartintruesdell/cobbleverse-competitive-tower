package dev.cobbleverse.tower.smoke;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.gui.widget.ButtonWidget;
import dev.cobbleverse.tower.PreviewNetwork;
import dev.cobbleverse.tower.client.PreviewScreen;
import java.util.*;

/** Separate test-only mod, never bundled in the runtime addon. Uses a disposable localhost server. */
public final class VisualSmoke implements ClientModInitializer {
    int phase=0,ticks=0;
    public void onInitializeClient() {
        if(Boolean.getBoolean("ctower.modeTest")){new ModeClientSmoke().register();return;}
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if(Boolean.getBoolean("ctower.realUiTest") && phase>=1) {
                if(c.currentScreen instanceof PreviewScreen screen && phase==1) {
                    if(++ticks<35)return;
                    for(int slot:new int[]{5,2,6,1}) ((net.minecraft.client.gui.screen.Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().startsWith("Slot "+slot+":")).findFirst().ifPresent(ButtonWidget::onPress);
                    var card=((net.minecraft.client.gui.screen.Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().startsWith("Selection 2:")).findFirst().orElseThrow();
                    double x=card.getX()+card.getWidth()/2.0,y=card.getY()+card.getHeight()/2.0;
                    ((net.minecraft.client.gui.screen.Screen)screen).mouseScrolled(x,y,0,1);
                    if(!card.getMessage().getString().startsWith("Selection 1:"))throw new AssertionError("Scroll did not move earlier");
                    ((net.minecraft.client.gui.screen.Screen)screen).mouseScrolled(x,y,0,-1);
                    if(!card.getMessage().getString().startsWith("Selection 2:"))throw new AssertionError("Scroll did not move later");
                    int[] w={0},h={0};org.lwjgl.glfw.GLFW.glfwGetWindowSize(c.getWindow().getHandle(),w,h);
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(c.getWindow().getHandle(),0,0);
                    ((net.minecraft.client.gui.screen.Screen)screen).setFocused(card);card.setFocused(true);
                    if(card.getHeight()<100)throw new AssertionError("Expected enlarged cards in this test viewport");
                    System.out.println("PHASE2_LAYOUT_PASS: card width="+card.getWidth()+", height="+card.getHeight()+", selected sprite target=64px");
                    System.out.println("PHASE2_REORDER_PASS: scroll earlier/later preserves intended order");
                    phase=2;ticks=0;
                } else if(phase==2) {
                    ++ticks;
                    if(ticks==10 && c.currentScreen instanceof PreviewScreen screen) {
                        var card=((net.minecraft.client.gui.screen.Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w)
                            .filter(b->b.getMessage().getString().startsWith("Selection 2:")).findFirst().orElseThrow();
                        ((net.minecraft.client.gui.screen.Screen)screen).mouseClicked(card.getX()+4,card.getY()+4,1);
                    }
                    if(ticks==15)ScreenshotRecorder.saveScreenshot(c.runDirectory,"friendly-reference-v040.png",c.getFramebuffer(),t->{});
                    if(ticks==17 && c.currentScreen instanceof PreviewScreen screen) {
                        c.options.getGuiScale().setValue(4);c.onResolutionChanged();
                        ((net.minecraft.client.gui.screen.Screen)screen).keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN,0,0);
                    }
                    if(ticks==20)ScreenshotRecorder.saveScreenshot(c.runDirectory,"friendly-reference-scrolled-v040.png",c.getFramebuffer(),t->{});
                    if(ticks==22 && c.currentScreen instanceof PreviewScreen screen) {
                        ((net.minecraft.client.gui.screen.Screen)screen).keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
                        System.out.println("FRIENDLY_REFERENCE_INPUT_PASS: right-click, paging and Escape exercised");
                    }
                    if(ticks==25 && c.currentScreen instanceof PreviewScreen screen) {
                        ((net.minecraft.client.gui.screen.Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().equals("Confirm")).findFirst().ifPresent(ButtonWidget::onPress);
                        phase=3;ticks=0;
                    }
                } else if(phase==3 && ++ticks>100) {phase=4;c.scheduleStop();}
                return;
            }
            if(phase==0 && c.currentScreen instanceof TitleScreen) {
                phase=1;ConnectScreen.connect(c.currentScreen,c,ServerAddress.parse("127.0.0.1:25577"),new ServerInfo("Disposable UI test","127.0.0.1:25577",ServerInfo.ServerType.OTHER),false,null);
            } else if(phase==1 && c.world!=null && ++ticks>100) {
                var team=List.of(entry("pikachu","Pikachu","Normal"),entry("charizard","Charizard","Normal"),entry("rotom","Rotom","Wash"),entry("garchomp","Garchomp","Normal"),entry("incineroar","Incineroar","Normal"),entry("amoonguss","Amoonguss","Normal"));
                c.setScreen(new PreviewScreen(new PreviewNetwork.Preview("visual-test",2,120000,team,team)));phase=2;ticks=0;
            } else if(phase==2 && ++ticks==40) {
                ScreenshotRecorder.saveScreenshot(c.runDirectory,"preview-empty.png",c.getFramebuffer(),t->{});
                if(c.currentScreen instanceof PreviewScreen screen) {
                    for(int slot:new int[]{5,2,6,1}) ((net.minecraft.client.gui.screen.Screen)screen).children().stream().filter(w->w instanceof ButtonWidget).map(w->(ButtonWidget)w).filter(b->b.getMessage().getString().startsWith("Slot "+slot+":")).findFirst().ifPresent(ButtonWidget::onPress);
                }
            } else if(phase==2 && ticks==80) {
                ScreenshotRecorder.saveScreenshot(c.runDirectory,"preview-selected.png",c.getFramebuffer(),t->{});
                if(c.currentScreen instanceof PreviewScreen screen)((net.minecraft.client.gui.screen.Screen)screen).close();
            } else if(phase==2 && ticks==100) {
                ScreenshotRecorder.saveScreenshot(c.runDirectory,"preview-forfeit.png",c.getFramebuffer(),t->{});
            } else if(phase==2 && ticks>120) {phase=3;c.scheduleStop();}
        });
    }
    private PreviewNetwork.Entry entry(String id,String name,String form) {return new PreviewNetwork.Entry("cobblemon:"+id,name,form);}
}
