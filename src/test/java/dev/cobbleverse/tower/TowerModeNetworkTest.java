package dev.cobbleverse.tower;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TowerModeNetworkTest {
    private RegistryByteBuf buffer(){return new RegistryByteBuf(Unpooled.buffer(),DynamicRegistryManager.EMPTY);}
    @Test void lockTransmitsFormatAndCompetitiveChoiceTogether() {
        for(boolean competitive:new boolean[]{false,true}) {
            var b=buffer();try{
                var p=new TowerModeNetwork.Lock("LEVEL_50",false,"DOUBLES","NORMAL",2,competitive);
                TowerModeNetwork.Lock.CODEC.encode(b,p);assertEquals(p,TowerModeNetwork.Lock.CODEC.decode(b));assertEquals(0,b.readableBytes());
            }finally{b.release();}
        }
    }
    @Test void stateRoundTripPreservesLockedRun() {
        var b=buffer();try{
            var p=new TowerModeNetwork.State(true,true,true,true,"");
            TowerModeNetwork.State.CODEC.encode(b,p);assertEquals(p,TowerModeNetwork.State.CODEC.decode(b));
        }finally{b.release();}
    }
    @Test void overlongModeFieldIsRejected() {
        var b=buffer();try{
            b.writeString("X".repeat(17));
            assertThrows(io.netty.handler.codec.DecoderException.class,()->TowerModeNetwork.Lock.CODEC.decode(b));
        }finally{b.release();}
    }
}
