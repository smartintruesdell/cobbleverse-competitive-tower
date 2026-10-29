package dev.cobbleverse.tower;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

/** Separate, capability-gated open format. Closed v1 packets never contain sheets. */
public final class SheetNetwork {
    private SheetNetwork() {}
    private static <T extends CustomPayload> CustomPayload.Id<T> id(String path) {
        return new CustomPayload.Id<>(Identifier.of("cobbleverse_competitive_tower",path+"_v1"));
    }
    /** Explicit allowlist: identifiers only, never Pokemon/NBT/stats/selection/PP state. */
    public record Sheet(String ability,String item,List<String> moves,String tera) {
        public Sheet {
            moves=List.copyOf(moves);
            if(ability.length()>128 || item.length()>128 || tera.length()>128 || moves.size()>4
                || moves.stream().anyMatch(m->m.length()>128))throw new IllegalArgumentException("Invalid sheet");
        }
        void write(RegistryByteBuf b) {
            b.writeString(ability,128);b.writeString(item,128);b.writeVarInt(moves.size());
            moves.forEach(m->b.writeString(m,128));b.writeString(tera,128);
        }
        static Sheet read(RegistryByteBuf b) {
            String ability=b.readString(128),item=b.readString(128);int count=b.readVarInt();
            if(count<0 || count>4)throw new IllegalArgumentException("Invalid sheet move count");
            List<String> moves=new ArrayList<>();for(int i=0;i<count;i++)moves.add(b.readString(128));
            return new Sheet(ability,item,moves,b.readString(128));
        }
    }
    public record Preview(PreviewNetwork.Preview preview,List<Sheet> opponent) implements CustomPayload {
        public static final Id<Preview> ID=id("open_preview");
        public Preview {opponent=List.copyOf(opponent);if(opponent.size()!=6)throw new IllegalArgumentException("Six sheets required");}
        public Id<Preview> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,Preview> CODEC=new PacketCodec<>() {
            public Preview decode(RegistryByteBuf b) {
                var preview=PreviewNetwork.Preview.CODEC.decode(b);List<Sheet> sheets=new ArrayList<>();
                for(int i=0;i<6;i++)sheets.add(Sheet.read(b));return new Preview(preview,sheets);
            }
            public void encode(RegistryByteBuf b,Preview p){PreviewNetwork.Preview.CODEC.encode(b,p.preview);p.opponent.forEach(s->s.write(b));}
        };
    }
    public record Lock(TowerModeNetwork.Lock mode,boolean open) implements CustomPayload {
        public static final Id<Lock> ID=id("sheet_lock");
        public Id<Lock> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,Lock> CODEC=new PacketCodec<>() {
            public Lock decode(RegistryByteBuf b){return new Lock(TowerModeNetwork.Lock.CODEC.decode(b),b.readBoolean());}
            public void encode(RegistryByteBuf b,Lock p){TowerModeNetwork.Lock.CODEC.encode(b,p.mode);b.writeBoolean(p.open);}
        };
    }
    public record State(TowerModeNetwork.State mode,boolean open) implements CustomPayload {
        public static final Id<State> ID=id("sheet_state");
        public Id<State> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,State> CODEC=new PacketCodec<>() {
            public State decode(RegistryByteBuf b){return new State(TowerModeNetwork.State.CODEC.decode(b),b.readBoolean());}
            public void encode(RegistryByteBuf b,State p){TowerModeNetwork.State.CODEC.encode(b,p.mode);b.writeBoolean(p.open);}
        };
    }
    public static boolean available(ServerPlayerEntity player) {
        return PreviewNetwork.available(player) && ServerPlayNetworking.canSend(player,Preview.ID)
            && ServerPlayNetworking.canSend(player,State.ID);
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(Preview.ID,Preview.CODEC);
        PayloadTypeRegistry.playS2C().register(State.ID,State.CODEC);
        PayloadTypeRegistry.playC2S().register(Lock.ID,Lock.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Lock.ID,(p,c)->c.server().execute(()->TowerModes.fromScreen(c.player(),p.mode(),p.open(),true)));
    }
}
