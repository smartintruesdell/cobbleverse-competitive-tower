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
    private final List<SheetNetwork.Sheet> opponentSheets;
    private final List<ButtonWidget> opponentButtons=new ArrayList<>();
    private final OrderedSelection selection=new OrderedSelection();
    private long deadline;
    private boolean waiting, confirmingCancel;
    private int referenceSlot=-1, referenceScroll;
    private String message="";
    private final List<ItemStack> opponentIcons=new ArrayList<>(), partyIcons=new ArrayList<>();
    private final List<ButtonWidget> partyButtons=new ArrayList<>();
    private ButtonWidget confirm, clear;
    private int left,top,panelWidth,cardWidth,cardHeight,opponentY,partyY,controlsY;

    public PreviewScreen(PreviewNetwork.Preview p) {this(p,List.of());}
    public PreviewScreen(PreviewNetwork.Preview p,List<SheetNetwork.Sheet> sheets) {
        super(Text.literal("Choose your four")); preview=p;
        opponentSheets=List.copyOf(sheets);
        if(!sheets.isEmpty() && sheets.size()!=6)throw new IllegalArgumentException("Six open sheets required");
        deadline=System.nanoTime()+p.remainingMs()*1_000_000L;
        p.opponent().forEach(e -> opponentIcons.add(icon(e)));
        p.party().forEach(e -> partyIcons.add(icon(e)));
    }
    public boolean openSheets(){return !opponentSheets.isEmpty();}
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
        clearChildren(); partyButtons.clear();opponentButtons.clear();
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
        if(!opponentSheets.isEmpty())for(int i=0;i<6;i++) {
            var b=addDrawableChild(new ButtonWidget(left+4+i*(cardWidth+2),opponentY,cardWidth,cardHeight,
                Text.literal("Open sheet: "+label(preview.opponent().get(i))),ignored->{},supplier->supplier.get()) {
                @Override public void renderWidget(DrawContext context,int mouseX,int mouseY,float delta) {}
            });
            opponentButtons.add(b);
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
        button("Forfeit",left+4,controlsY,bw,20,this::close);
        clear=button("Clear",left+6+bw,controlsY,bw,20,()->{selection.clear();message="";updateButtons();});
        button("Use chat",left+8+bw*2,controlsY,bw,20,()->{
            if(ClientPlayNetworking.canSend(PreviewNetwork.ChatRequest.ID))ClientPlayNetworking.send(new PreviewNetwork.ChatRequest(token()));
            if(client!=null)client.setScreen(null);
        }).setTooltip(Tooltip.of(Text.literal("Show selection details and the command in chat. The timer continues.")));
        confirm=button("Confirm",left+10+bw*3,controlsY,bw,20,()->{
            int[] s=selection.confirmed();waiting=true;message="Waiting for server...";updateButtons();
            ClientPlayNetworking.send(new PreviewNetwork.Submit(token(),s[0],s[1],s[2],s[3]));
        });
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
        if(referenceSlot>=0) { referenceSlot=-1; return; }
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
        c.drawTextWithShadow(textRenderer,opponentSheets.isEmpty()?"Opponent · CLOSED sheets · species/forms only":"Opponent · OPEN sheets · hover for details",left+6,opponentY-11,0xBAC8DA);
        c.drawTextWithShadow(textRenderer,"Your party · click four in lead-first order",left+6,partyY-11,0xBAC8DA);
        super.render(c,mouseX,mouseY,delta);
        for(int i=0;i<6;i++) {
            drawCard(c,preview.opponent().get(i),opponentIcons.get(i),left+4+i*(cardWidth+2),opponentY,-1,!opponentButtons.isEmpty() && opponentButtons.get(i).isFocused(),true,opponentSheets.isEmpty()?ItemStack.EMPTY:sheetItem(opponentSheets.get(i)));
            var friendly=friendlyPokemon(i);
            drawCard(c,preview.party().get(i),partyIcons.get(i),left+4+i*(cardWidth+2),partyY,selection.slots().indexOf(i+1),partyButtons.get(i).isFocused(),false,
                friendly==null?ItemStack.EMPTY:friendly.heldItem());
        }
        String footer=seconds==0?"Time elapsed — waiting for server.":message.isEmpty()?"1–2 lead · Click to select/remove · Scroll a selection to reorder.":message;
        c.drawTextWithShadow(textRenderer,fit(footer,panelWidth-12),left+6,controlsY+24,0xBAC8DA);
        if(referenceSlot>=0) { drawReference(c); return; }
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
                if(mouseX>=x && mouseX<x+cardWidth && mouseY>=opponentY && mouseY<opponentY+cardHeight) {
                    if(opponentSheets.isEmpty())c.drawTooltip(textRenderer,Text.literal(label(preview.opponent().get(i))),mouseX,mouseY);
                    else c.drawTooltip(textRenderer,sheetLines(i),mouseX,mouseY);
                }
            }
        }
        if(friendly<0 && !overOpponent)for(int i=0;i<opponentButtons.size();i++) {
            var b=opponentButtons.get(i);
            if(b.isFocused()) {c.drawTooltip(textRenderer,sheetLines(i),b.getX()+cardWidth/2,opponentY+cardHeight);break;}
        }
    }
    private static boolean namedForm(PreviewNetwork.Entry e) {
        return !e.form().isBlank() && !e.form().equalsIgnoreCase("normal");
    }
    private static String label(PreviewNetwork.Entry e) {
        return e.name()+(namedForm(e)?" / "+e.form():"");
    }
    private List<Text> friendlyTooltip(int slot) { return referenceLines(slot,false); }
    private static ItemStack sheetItem(SheetNetwork.Sheet sheet) {
        var id=Identifier.tryParse(sheet.item());
        return id==null?ItemStack.EMPTY:new ItemStack(net.minecraft.registry.Registries.ITEM.get(id));
    }
    private List<Text> sheetLines(int slot) {
        var sheet=opponentSheets.get(slot);
        List<Text> lines=new ArrayList<>();
        lines.add(Text.literal(label(preview.opponent().get(slot))).formatted(Formatting.RED));
        var ability=com.cobblemon.mod.common.api.abilities.Abilities.get(sheet.ability());
        lines.add(Text.literal("Ability: ").formatted(Formatting.GRAY)
            .append(ability==null?Text.literal(sheet.ability()):Text.translatable(ability.getDisplayName())));
        var item=sheetItem(sheet);
        lines.add(item.isEmpty()?Text.literal("No held item").formatted(Formatting.GRAY)
            :Text.literal("Held: ").formatted(Formatting.GRAY).append(item.getName()));
        lines.add(Text.literal("Tera type: "+sheet.tera()).formatted(Formatting.GRAY));
        lines.add(Text.literal("Moves").formatted(Formatting.GRAY));
        for(String name:sheet.moves()) {
            var move=com.cobblemon.mod.common.api.moves.Moves.getByName(name);
            lines.add(Text.literal("• ").append(move==null?Text.literal(name):move.getDisplayName()));
        }
        if(sheet.moves().isEmpty())lines.add(Text.literal("No moves learned.").formatted(Formatting.GRAY));
        return lines;
    }
    private List<Text> referenceLines(int slot,boolean detailed) {
        var entry=preview.party().get(slot);
        List<Text> lines=new ArrayList<>();
        lines.add(Text.literal(label(entry)).formatted(Formatting.AQUA));
        // Only consult the player's already-synced client party. Opponent entries never use this path.
        var pokemon=friendlyPokemon(slot);
        if(pokemon!=null) {
            lines.add(Text.literal("Ability: ").formatted(Formatting.GRAY)
                .append(Text.translatable(pokemon.getAbility().getDisplayName())));
            if(detailed)lines.add(Text.translatable(pokemon.getAbility().getDescription()).formatted(Formatting.GRAY));
            var held=pokemon.heldItem();
            lines.add(held.isEmpty()?Text.literal("No held item").formatted(Formatting.GRAY)
                :Text.literal("Held: ").formatted(Formatting.GRAY).append(held.getName()));
            lines.add(Text.literal("Tera type: ").formatted(Formatting.GRAY).append(pokemon.getTeraType().getDisplayName()));
            if(detailed && !held.isEmpty()) {
                String key=held.getItem().getTranslationKey()+".tooltip";
                if(net.minecraft.client.resource.language.I18n.hasTranslation(key))
                    lines.add(Text.translatable(key).formatted(Formatting.GRAY));
                else lines.add(Text.literal("No item description supplied.").formatted(Formatting.GRAY));
            }
            if(detailed) {
                lines.add(Text.literal("Party stats · level "+pokemon.getLevel()).formatted(Formatting.YELLOW));
                lines.add(Text.literal("HP "+pokemon.getMaxHealth()+"  Atk "+pokemon.getAttack()+"  Def "+pokemon.getDefence()));
                lines.add(Text.literal("SpA "+pokemon.getSpecialAttack()+"  SpD "+pokemon.getSpecialDefence()+"  Spe "+pokemon.getSpeed()));
                lines.add(Text.literal("Party values only; Tower normalization and battle effects may change them.").formatted(Formatting.GRAY));
            }
            lines.add(Text.literal("Moves").formatted(Formatting.GRAY));
            var moves=pokemon.getMoveSet().getMoves();
            if(moves.isEmpty())lines.add(Text.literal("No moves learned.").formatted(Formatting.GRAY));
            for(var move:moves) {
                lines.add(Text.literal("• ").append(move.getDisplayName()));
                if(detailed) {
                    lines.add(Text.empty().append(move.getType().getDisplayName()).append(" · ")
                        .append(move.getDamageCategory().getDisplayName()).formatted(Formatting.GRAY));
                    lines.add(Text.literal("Power "+number(move.getPower())+" · Accuracy "+number(move.getAccuracy())
                        +" · PP "+move.getCurrentPp()+"/"+move.getMaxPp()).formatted(Formatting.GRAY));
                    lines.add(move.getDescription().copy().formatted(Formatting.GRAY));
                }
            }
        } else lines.add(Text.literal("Waiting for party details…").formatted(Formatting.GRAY));
        int rank=selection.slots().indexOf(slot+1);
        if(rank>=0)lines.add(Text.literal("Selected #"+(rank+1)+(rank<2?" · opens battle":"")).formatted(Formatting.YELLOW));
        if(!detailed)lines.add(Text.literal("Right-click or R for full reference").formatted(Formatting.YELLOW));
        return lines;
    }
    private static String number(double value) {
        return value<=0?"—":value==Math.rint(value)?Long.toString((long)value):Double.toString(value);
    }
    private void drawReference(DrawContext c) {
        // Item models render above ordinary screen text; keep the entire modal above them.
        c.getMatrices().push();
        c.getMatrices().translate(0,0,400);
        try { drawReferenceContent(c); }
        finally { c.getMatrices().pop(); }
    }
    private void drawReferenceContent(DrawContext c) {
        int w=Math.max(80,Math.min(390,width-16)), x=(width-w)/2, y=12, bottom=height-12;
        c.fill(0,0,width,height,0xB0000000);
        c.fill(x,y,x+w,bottom,0xFF1C2838);
        c.drawBorder(x,y,w,bottom-y,0xFF75E5D1);
        c.drawTextWithShadow(textRenderer,"YOUR TEAM · REFERENCE",x+10,y+9,0x75E5D1);
        String timer=Math.max(0,(deadline-System.nanoTime()+999_999_999L)/1_000_000_000L)+"s";
        c.drawTextWithShadow(textRenderer,timer,x+w-10-textRenderer.getWidth(timer),y+9,0xFFBD69);
        var wrapped=new ArrayList<net.minecraft.text.OrderedText>();
        for(var line:referenceLines(referenceSlot,true))wrapped.addAll(textRenderer.wrapLines(line,w-24));
        int contentTop=y+27,contentBottom=bottom-27;
        int visible=Math.max(1,(contentBottom-contentTop)/12);
        referenceScroll=Math.max(0,Math.min(referenceScroll,Math.max(0,wrapped.size()-visible)));
        c.enableScissor(x+6,contentTop,x+w-6,contentBottom);
        try {
            for(int i=referenceScroll;i<Math.min(wrapped.size(),referenceScroll+visible);i++)
                c.drawTextWithShadow(textRenderer,wrapped.get(i),x+12,contentTop+(i-referenceScroll)*12,0xFFFFFF);
        } finally { c.disableScissor(); }
        c.drawTextWithShadow(textRenderer,fit("Scroll / ↑ ↓ · Esc or click to return",w-20),x+10,bottom-17,0xBAC8DA);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(referenceSlot>=0) { referenceSlot=-1; return true; }
        int slot=partyAt(x,y);
        if(button==1 && slot>=0 && !confirmingCancel) { referenceSlot=slot; referenceScroll=0; return true; }
        return super.mouseClicked(x,y,button);
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
        if(referenceSlot>=0) { referenceScroll-= (int)Math.signum(vertical)*3; return true; }
        int slot=partyAt(x,y);
        if(slot>=0 && vertical!=0 && moveSelected(slot,vertical>0?-1:1))return true;
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(referenceSlot>=0) {
            if(key==GLFW.GLFW_KEY_ESCAPE || key==GLFW.GLFW_KEY_R)referenceSlot=-1;
            else if(key==GLFW.GLFW_KEY_DOWN)referenceScroll++;
            else if(key==GLFW.GLFW_KEY_UP)referenceScroll--;
            else if(key==GLFW.GLFW_KEY_PAGE_DOWN)referenceScroll+=8;
            else if(key==GLFW.GLFW_KEY_PAGE_UP)referenceScroll-=8;
            return true;
        }
        if(key==GLFW.GLFW_KEY_R && !confirmingCancel) {
            for(int i=0;i<partyButtons.size();i++)if(partyButtons.get(i).isFocused()) {
                referenceSlot=i;referenceScroll=0;return true;
            }
        }
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
