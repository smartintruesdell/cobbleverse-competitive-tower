package dev.cobbleverse.tower;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreviewNetworkTest {
    private RegistryByteBuf buffer() { return new RegistryByteBuf(Unpooled.buffer(),DynamicRegistryManager.EMPTY); }
    @Test void previewRoundTripHasOnlyPublicLabelsAndFixedTeams() {
        var entries=Collections.nCopies(6,new PreviewNetwork.Entry("cobblemon:rotom","Rotom","wash"));
        var p=new PreviewNetwork.Preview(UUID.randomUUID().toString(),12,119000,entries,entries);
        var b=buffer();try{PreviewNetwork.Preview.CODEC.encode(b,p);assertEquals(p,PreviewNetwork.Preview.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        assertThrows(IllegalArgumentException.class,()->new PreviewNetwork.Preview("t",1,1000,entries.subList(0,5),entries));
        assertThrows(IllegalArgumentException.class,()->new PreviewNetwork.Preview("t",1,600001,entries,entries));
    }
    @Test void orderedConfirmAndCancellationRoundTrip() {
        var b=buffer();try{
            var p=new PreviewNetwork.Submit("token",5,2,6,1);
            PreviewNetwork.Submit.CODEC.encode(b,p);assertEquals(p,PreviewNetwork.Submit.CODEC.decode(b));
            var c=new PreviewNetwork.Cancel("token");PreviewNetwork.Cancel.CODEC.encode(b,c);assertEquals(c,PreviewNetwork.Cancel.CODEC.decode(b));
        }finally{b.release();}
    }
    @Test void oversizedInboundTokenRejected() {
        var b=buffer();try{
            b.writeString("x".repeat(65));
            assertThrows(io.netty.handler.codec.DecoderException.class,()->PreviewNetwork.Submit.CODEC.decode(b));
        }finally{b.release();}
    }
}
