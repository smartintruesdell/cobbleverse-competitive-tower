package dev.cobbleverse.tower;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class TowerModeNetwork {
    private TowerModeNetwork() {}
    public record Query() implements CustomPayload {
        public static final Id<Query> ID=new Id<>(Identifier.of("cobbleverse_competitive_tower","mode_query_v1"));
        public Id<Query> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,Query> CODEC=new PacketCodec<>() {
            public Query decode(RegistryByteBuf b){return new Query();}
            public void encode(RegistryByteBuf b,Query p){}
        };
    }
    public record Lock(String levelMode,boolean legendaries,String battleMode,String teamMode,int bossPlayers,boolean competitive) implements CustomPayload {
        public static final Id<Lock> ID=new Id<>(Identifier.of("cobbleverse_competitive_tower","mode_lock_v1"));
        public Id<Lock> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,Lock> CODEC=new PacketCodec<>() {
            public Lock decode(RegistryByteBuf b){return new Lock(b.readString(16),b.readBoolean(),b.readString(16),b.readString(16),b.readVarInt(),b.readBoolean());}
            public void encode(RegistryByteBuf b,Lock p){b.writeString(p.levelMode,16);b.writeBoolean(p.legendaries);b.writeString(p.battleMode,16);b.writeString(p.teamMode,16);b.writeVarInt(p.bossPlayers);b.writeBoolean(p.competitive);}
        };
    }
    public record State(boolean enabled,boolean competitive,boolean locked,boolean active,String message) implements CustomPayload {
        public static final Id<State> ID=new Id<>(Identifier.of("cobbleverse_competitive_tower","mode_state_v1"));
        public Id<State> getId(){return ID;}
        public static final PacketCodec<RegistryByteBuf,State> CODEC=new PacketCodec<>() {
            public State decode(RegistryByteBuf b){return new State(b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readString(256));}
            public void encode(RegistryByteBuf b,State p){b.writeBoolean(p.enabled);b.writeBoolean(p.competitive);b.writeBoolean(p.locked);b.writeBoolean(p.active);b.writeString(p.message,256);}
        };
    }
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Query.ID,Query.CODEC);
        PayloadTypeRegistry.playC2S().register(Lock.ID,Lock.CODEC);
        PayloadTypeRegistry.playS2C().register(State.ID,State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Query.ID,(p,c)->c.server().execute(()->TowerModes.sync(c.player(),"")));
        ServerPlayNetworking.registerGlobalReceiver(Lock.ID,(p,c)->c.server().execute(()->TowerModes.fromScreen(c.player(),p)));
    }
}
