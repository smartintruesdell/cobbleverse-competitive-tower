package dev.cobbleverse.tower.client;

import dev.cobbleverse.tower.*;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.client.CobblemonClient;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** Client-only view. No world or party mutations, and no battle cancellation in removed(). */
public final class PreviewScreen extends Screen {
    private PreviewNetwork.Preview preview;
    private final OrderedSelection selection=new OrderedSelection();
    private long deadline;
    private boolean waiting, confirmingCancel;
    private String message="";
    private final List<ItemStack> opponentIcons=new ArrayList<>(), partyIcons=new ArrayList<>();
    private final List<ButtonWidget> partyButtons=new ArrayList<>();
    private ButtonWidget confirm, clear;
    private int left,top,panelWidth,cardWidth,cardHeight,opponentY,partyY,controlsY;

    public PreviewScreen(PreviewNetwork.Preview p) {
        super(Text.literal("Choose your four")); preview=p;
        deadline=System.nanoTime()+p.remainingMs()*1_000_000L;
        p.opponent().forEach(e -> opponentIcons.add(icon(e)));
        p.party().forEach(e -> partyIcons.add(icon(e)));
    }
    public String token() { return preview.token(); }
    public void refresh(PreviewNetwork.Preview p) {
        preview=p; deadline=Math.min(deadline,System.nanoTime()+p.remainingMs()*1_000_000L);
    }
    public void status(PreviewNetwork.Status status) {
        if(status.finished()) {
            if(client!=null && client.currentScreen==this) client.setScreen(null);
        } else { waiting=false; message=status.message(); updateButtons(); }
    }
    private static ItemStack icon(PreviewNetwork.Entry e) {
        try {
            var species=PokemonSpecies.getByIdentifier(Identifier.of(e.species()));
            if(species==null) return ItemStack.EMPTY;
            var form=species.getForms().stream().filter(f -> f.getName().equals(e.form())).findFirst();
            return PokemonItem.from(species,form.map(f -> f.getAspects().toArray(String[]::new)).orElse(new String[0]));
        } catch (RuntimeException ignored) { return ItemStack.EMPTY; }
    }
    @Override protected void init() {
        clearChildren(); partyButtons.clear();
        panelWidth=Math.min(600,width-16); left=(width-panelWidth)/2;
        int panelHeight=Math.min(330,height-8);top=Math.max(4,(height-panelHeight)/2);
        cardWidth=(panelWidth-20)/6;cardHeight=Math.max(26,(panelHeight-114)/2);
        opponentY=top+40;partyY=opponentY+cardHeight+18;controlsY=partyY+cardHeight+14;
        if(confirmingCancel) {
            button("Keep choosing",left+12,top+110,(panelWidth-30)/2,22,()->{confirmingCancel=false;init();});
            button("Forfeit run",left+18+(panelWidth-30)/2,top+110,(panelWidth-30)/2,22,()->{
                waiting=true; confirmingCancel=false; init();
                ClientPlayNetworking.send(new PreviewNetwork.Cancel(token()));
            });
            return;
        }
        for(int i=0;i<6;i++) {
            final int slot=i+1;
            var b=addDrawableChild(new ButtonWidget(left+4+i*(cardWidth+2),partyY,cardWidth,cardHeight,Text.empty(),
                ignored->{selection.toggle(slot);message="";updateButtons();},supplier -> supplier.get()) {
                @Override public void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {
                    // Drawn once by the card renderer; retain native focus, input and narration.
                }
            });
            partyButtons.add(b);
        }
        int bw=(panelWidth-14)/4;
        confirm=button("Confirm",left+4,controlsY,bw,20,()->{
            int[] s=selection.confirmed();waiting=true;message="Waiting for server...";updateButtons();
            ClientPlayNetworking.send(new PreviewNetwork.Submit(token(),s[0],s[1],s[2],s[3]));
        });
        clear=button("Clear",left+6+bw,controlsY,bw,20,()->{selection.clear();message="";updateButtons();});
        button("Use chat",left+8+bw*2,controlsY,bw,20,()->{
            if(client!=null)client.setScreen(null);
        }).setTooltip(Tooltip.of(Text.literal("Hide this screen. The timer continues; use the selection command already in chat.")));
        button("Forfeit",left+10+bw*3,controlsY,bw,20,this::close);
        updateButtons();
    }
    private ButtonWidget button(String text,int x,int y,int w,int h,Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(text),b->action.run()).dimensions(x,y,w,h).build());
    }
    private void updateButtons() {
        if(confirmingCancel || confirm==null)return;
        boolean editable=!waiting && System.nanoTime()<deadline;
        var slots=selection.slots();confirm.active=editable&&selection.complete();clear.active=editable&&!slots.isEmpty();
        for(int i=0;i<partyButtons.size();i++) {
            var b=partyButtons.get(i);int rank=slots.indexOf(i+1);
            b.active=editable&&(rank>=0 || slots.size()<4);
            // Full text is available to narration, even when visual text is compact.
            b.setMessage(Text.literal((rank>=0 ? "Selection "+(rank+1)+": " : "Slot "+(i+1)+": ")+preview.party().get(i).name()));
        }
    }
    private String fit(String s,int width) { return textRenderer.getWidth(s)<=width?s:textRenderer.trimToWidth(s,Math.max(0,width-6))+"…"; }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context,int mouseX,int mouseY,float delta) {}
    @Override public void close() {
        if(waiting)return;
        confirmingCancel=!confirmingCancel;init();
    }
    @Override public void tick() { updateButtons(); }
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta) {
        c.fill(0,0,width,height,0xD0101623);
        c.fill(left,top,left+panelWidth,Math.min(height-2,controlsY+34),0xF01C2838);
        c.fill(left,top,left+panelWidth,top+2,0xFF57D7BE);
        c.drawTextWithShadow(textRenderer,"CHOOSE YOUR FOUR  ·  Floor "+preview.floor(),left+8,top+9,0xFFFFFF);
        long seconds=Math.max(0,(deadline-System.nanoTime()+999_999_999L)/1_000_000_000L);
        String timer=seconds+"s";c.drawTextWithShadow(textRenderer,timer,left+panelWidth-8-textRenderer.getWidth(timer),top+9,seconds<=15?0xFFBD69:0x75E5D1);
        if(confirmingCancel) {
            c.drawCenteredTextWithShadow(textRenderer,"Forfeit this Tower run?",width/2,top+65,0xFFFFFF);
            c.drawCenteredTextWithShadow(textRenderer,"This ends the run. The selection timer continues.",width/2,top+82,0xFFBD69);
            super.render(c,mouseX,mouseY,delta);return;
        }
        c.drawTextWithShadow(textRenderer,"Opponent · species and forms only",left+6,opponentY-11,0xBAC8DA);
        c.drawTextWithShadow(textRenderer,"Your party · click four in lead-first order",left+6,partyY-11,0xBAC8DA);
        super.render(c,mouseX,mouseY,delta);
        for(int i=0;i<6;i++) {
            drawCard(c,preview.opponent().get(i),opponentIcons.get(i),left+4+i*(cardWidth+2),opponentY,-1,false,true,ItemStack.EMPTY);
            var friendly=friendlyPokemon(i);
            drawCard(c,preview.party().get(i),partyIcons.get(i),left+4+i*(cardWidth+2),partyY,selection.slots().indexOf(i+1),partyButtons.get(i).isFocused(),false,
                friendly==null?ItemStack.EMPTY:friendly.heldItem());
        }
        String footer=seconds==0?"Time elapsed — waiting for server.":message.isEmpty()?"1–2 lead · Click to select/remove · Scroll a selection to reorder.":message;
        c.drawTextWithShadow(textRenderer,fit(footer,panelWidth-12),left+6,controlsY+24,0xBAC8DA);
        int friendly=partyAt(mouseX,mouseY);
        int tooltipX=mouseX, tooltipY=mouseY;
        boolean overOpponent=mouseY>=opponentY && mouseY<opponentY+cardHeight
            && mouseX>=left+4 && mouseX<left+panelWidth-4;
        if(friendly<0 && !overOpponent) {
            for(int i=0;i<partyButtons.size();i++) if(partyButtons.get(i).isFocused()) {
                friendly=i;tooltipX=partyButtons.get(i).getX()+cardWidth/2;tooltipY=partyY+cardHeight;break;
            }
        }
        if(friendly>=0) {
            c.drawTooltip(textRenderer,friendlyTooltip(friendly),tooltipX,tooltipY);
        } else {
            for(int i=0;i<6;i++) {
                int x=left+4+i*(cardWidth+2);
                if(mouseX>=x && mouseX<x+cardWidth && mouseY>=opponentY && mouseY<opponentY+cardHeight)
                    c.drawTooltip(textRenderer,Text.literal(label(preview.opponent().get(i))),mouseX,mouseY);
            }
        }
    }
    private static boolean namedForm(PreviewNetwork.Entry e) {
        return !e.form().isBlank() && !e.form().equalsIgnoreCase("normal");
    }
    private static String label(PreviewNetwork.Entry e) {
        return e.name()+(namedForm(e)?" / "+e.form():"");
    }
    private List<Text> friendlyTooltip(int slot) {
        var entry=preview.party().get(slot);
        List<Text> lines=new ArrayList<>();
        lines.add(Text.literal(label(entry)).formatted(Formatting.AQUA));
        // Only consult the player's already-synced client party. Opponent entries never use this path.
        var pokemon=friendlyPokemon(slot);
        if(pokemon!=null) {
            var held=pokemon.heldItem();
            lines.add(held.isEmpty()?Text.literal("No held item").formatted(Formatting.GRAY)
                :Text.literal("Held: ").formatted(Formatting.GRAY).append(held.getName()));
            lines.add(Text.literal("Moves").formatted(Formatting.GRAY));
            var moves=pokemon.getMoveSet().getMoves();
            if(moves.isEmpty())lines.add(Text.literal("No moves learned.").formatted(Formatting.GRAY));
            for(var move:moves)lines.add(Text.literal("• ").append(move.getDisplayName()));
        } else lines.add(Text.literal("Waiting for party details…").formatted(Formatting.GRAY));
        int rank=selection.slots().indexOf(slot+1);
        if(rank>=0)lines.add(Text.literal("Selected #"+(rank+1)+(rank<2?" · opens battle":"")).formatted(Formatting.YELLOW));
        return lines;
    }
    private Pokemon friendlyPokemon(int slot) {
        var entry=preview.party().get(slot);
        var party=CobblemonClient.INSTANCE.getStorage().getParty();
        var pokemon=party==null || slot>=party.getSlots().size()?null:party.get(slot);
        return pokemon!=null && pokemon.getSpecies().getResourceIdentifier().toString().equals(entry.species())
            && pokemon.getForm().getName().equalsIgnoreCase(entry.form())?pokemon:null;
    }
    private int partyAt(double x,double y) {
        if(y<partyY || y>=partyY+cardHeight)return -1;
        for(int i=0;i<6;i++) {
            int cardX=left+4+i*(cardWidth+2);
            if(x>=cardX && x<cardX+cardWidth)return i;
        }
        return -1;
    }
    private boolean moveSelected(int slot,int delta) {
        int rank=selection.slots().indexOf(slot+1);
        if(confirmingCancel || waiting || System.nanoTime()>=deadline || rank<0)return false;
        selection.move(rank,delta);updateButtons();return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        int slot=partyAt(x,y);
        if(slot>=0 && vertical!=0 && moveSelected(slot,vertical>0?-1:1))return true;
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==GLFW.GLFW_KEY_LEFT || key==GLFW.GLFW_KEY_RIGHT) {
            for(int i=0;i<partyButtons.size();i++) {
                if(partyButtons.get(i).isFocused() && moveSelected(i,key==GLFW.GLFW_KEY_LEFT?-1:1))return true;
            }
        }
        return super.keyPressed(key,scan,modifiers);
    }
    private void drawCard(DrawContext c,PreviewNetwork.Entry e,ItemStack icon,int x,int y,int rank,boolean focused,boolean opponent,ItemStack heldItem) {
        int color=opponent?0xFF553139:rank<0?0xFF29394F:rank<2?0xFF24584F:0xFF344E78;
        c.fill(x+1,y+1,x+cardWidth-1,y+cardHeight-1,color);
        if(focused)c.drawBorder(x,y,cardWidth,cardHeight,0xFFFFFFFF);
        if(rank>=0)c.drawBorder(x,y,cardWidth,cardHeight,rank<2?0xFF75E5D1:0xFF9DBDFF);
        int nameY=y+cardHeight-(cardHeight>=44 && namedForm(e)?22:11);
        if(!icon.isEmpty()) {
            float scale=Math.min(4f,Math.min((cardWidth-12)/16f,(nameY-y-8)/16f));
            // Some Cobblemon item models float beyond the normal item square.
            // Clip to the artwork region so no pose can overlap a card border or label.
            c.enableScissor(x+2,y+4,x+cardWidth-2,nameY-3);
            float artX=x+(cardWidth-16*scale)/2;
            float artY=y+4+Math.max(0,(nameY-y-8-16*scale)/2);
            c.getMatrices().push();
            try {
                c.getMatrices().translate(artX,artY,0);
                c.getMatrices().scale(scale,scale,1);
                c.drawItem(icon,0,0);
            } finally {
                c.getMatrices().pop();
                c.disableScissor();
            }
            if(!heldItem.isEmpty()) {
                int badgeX=Math.round(artX+16*scale-16);
                int badgeY=Math.round(artY+16*scale-16);
                c.fill(badgeX-1,badgeY-1,badgeX+17,badgeY+17,0xE815202E);
                c.drawBorder(badgeX-1,badgeY-1,18,18,0xFFB8C9DA);
                c.drawItem(heldItem,badgeX,badgeY);
            }
        }
        if(rank>=0)c.drawTextWithShadow(textRenderer,Integer.toString(rank+1),x+3,y+3,0xFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer,fit(e.name(),cardWidth-5),x+cardWidth/2,nameY,0xFFFFFF);
        if(cardHeight>=44 && namedForm(e))c.drawCenteredTextWithShadow(textRenderer,fit(e.form(),cardWidth-5),x+cardWidth/2,y+cardHeight-11,0xBAC8DA);
        // Form is explicit in tooltip for small screens; never substitute a build-derived label.
    }
}
