package dev.cobbleverse.tower;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SheetNetworkTest {
    private RegistryByteBuf buffer(){return new RegistryByteBuf(Unpooled.buffer(),DynamicRegistryManager.EMPTY);}
    private PreviewNetwork.Preview closed(){var entries=Collections.nCopies(6,new PreviewNetwork.Entry("cobblemon:rotom","Rotom","wash"));return new PreviewNetwork.Preview("token",1,120000,entries,entries);}
    @Test void openSheetsRoundTripOnlyAllowlistedFields() {
        var sheet=new SheetNetwork.Sheet("levitate","cobblemon:choice_scarf",List.of("hydropump","voltswitch"),"Water");
        var p=new SheetNetwork.Preview(closed(),Collections.nCopies(6,sheet));
        var b=buffer();try{SheetNetwork.Preview.CODEC.encode(b,p);assertEquals(p,SheetNetwork.Preview.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        assertEquals(List.of("ability","item","moves","tera"),Arrays.stream(SheetNetwork.Sheet.class.getRecordComponents()).map(c->c.getName()).toList());
    }
    @Test void closedProtocolHasNoSheetFieldsOrBytes() {
        assertEquals(List.of("token","floor","remainingMs","opponent","party"),Arrays.stream(PreviewNetwork.Preview.class.getRecordComponents()).map(c->c.getName()).toList());
        assertEquals(List.of("species","name","form"),Arrays.stream(PreviewNetwork.Entry.class.getRecordComponents()).map(c->c.getName()).toList());
        assertNotEquals(SheetNetwork.Preview.ID,PreviewNetwork.Preview.ID);
        var b=buffer();try{PreviewNetwork.Preview.CODEC.encode(b,closed());assertEquals(closed(),PreviewNetwork.Preview.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
    }
    @Test void sheetSizesAndInboundMoveCountAreBounded() {
        assertThrows(IllegalArgumentException.class,()->new SheetNetwork.Sheet("a","",Collections.nCopies(5,"move"),"Water"));
        assertThrows(IllegalArgumentException.class,()->new SheetNetwork.Sheet("a".repeat(129),"",List.of(),""));
        assertThrows(IllegalArgumentException.class,()->new SheetNetwork.Preview(closed(),List.of()));
        for(int count:new int[]{-1,5,Integer.MAX_VALUE}) {
            var b=buffer();try{b.writeString("a");b.writeString("");b.writeVarInt(count);assertThrows(IllegalArgumentException.class,()->SheetNetwork.Sheet.read(b));}finally{b.release();}
        }
    }
    @Test void openChoiceAndStateRoundTripWithoutChangingLegacySchema() {
        var mode=new TowerModeNetwork.Lock("LEVEL_50",false,"DOUBLES","NORMAL",2,true);
        for(boolean open:new boolean[]{false,true}) {
            var b=buffer();try{
                var lock=new SheetNetwork.Lock(mode,open);SheetNetwork.Lock.CODEC.encode(b,lock);assertEquals(lock,SheetNetwork.Lock.CODEC.decode(b));
                var state=new SheetNetwork.State(new TowerModeNetwork.State(true,true,true,false,""),open);
                SheetNetwork.State.CODEC.encode(b,state);assertEquals(state,SheetNetwork.State.CODEC.decode(b));assertEquals(0,b.readableBytes());
            }finally{b.release();}
        }
    }
    @Test void selectorOffersOpenOnlyWithServerSupport() {
        var closed=new ModeChoice(1,true);
        assertEquals(new ModeChoice(2,false),closed.next(false));
        assertEquals(new ModeChoice(1,true,true),closed.next(true));
        assertEquals(new ModeChoice(2,false),closed.next(true).next(true));
        assertThrows(IllegalArgumentException.class,()->new ModeChoice(1,false,true));
    }
}
