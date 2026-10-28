package dev.cobbleverse.tower;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

/** The entire preview wire schema. Deliberately has no Pokemon/build/NBT fields. */
public final class PreviewNetwork {
    private PreviewNetwork() {}
    private static <T extends CustomPayload> CustomPayload.Id<T> id(String path) {
        return new CustomPayload.Id<>(Identifier.of("cobbleverse_competitive_tower", path + "_v1"));
    }
    public record Entry(String species, String name, String form) {
        public Entry {
            if (species.length()>128 || name.length()>128 || form.length()>128) throw new IllegalArgumentException("Preview label too long");
        }
        static Entry read(RegistryByteBuf b) { return new Entry(b.readString(128),b.readString(128),b.readString(128)); }
        void write(RegistryByteBuf b) { b.writeString(species,128); b.writeString(name,128); b.writeString(form,128); }
    }
    public record Preview(String token, int floor, int remainingMs, List<Entry> opponent, List<Entry> party) implements CustomPayload {
        public static final Id<Preview> ID = id("preview");
        public Preview {
            opponent=List.copyOf(opponent); party=List.copyOf(party);
            if (token.length()>64 || opponent.size()!=6 || party.size()!=6 || remainingMs<0 || remainingMs>600_000) throw new IllegalArgumentException("Invalid preview");
        }
        public Id<Preview> getId() { return ID; }
        public static final PacketCodec<RegistryByteBuf,Preview> CODEC = new PacketCodec<>() {
            public Preview decode(RegistryByteBuf b) {
                String token=b.readString(64); int floor=b.readVarInt(), ms=b.readVarInt();
                List<Entry> opponent=new ArrayList<>(), party=new ArrayList<>();
                for(int i=0;i<6;i++) opponent.add(Entry.read(b));
                for(int i=0;i<6;i++) party.add(Entry.read(b));
                return new Preview(token,floor,ms,opponent,party);
            }
            public void encode(RegistryByteBuf b, Preview p) {
                b.writeString(p.token,64); b.writeVarInt(p.floor); b.writeVarInt(p.remainingMs);
                p.opponent.forEach(e -> e.write(b)); p.party.forEach(e -> e.write(b));
            }
        };
    }
    public record Submit(String token, int first, int second, int third, int fourth) implements CustomPayload {
        public static final Id<Submit> ID=id("submit");
        public Id<Submit> getId() { return ID; }
        public static final PacketCodec<RegistryByteBuf,Submit> CODEC=new PacketCodec<>() {
            public Submit decode(RegistryByteBuf b) { return new Submit(b.readString(64),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()); }
            public void encode(RegistryByteBuf b,Submit p) { b.writeString(p.token,64); b.writeVarInt(p.first); b.writeVarInt(p.second); b.writeVarInt(p.third); b.writeVarInt(p.fourth); }
        };
    }
    public record Cancel(String token) implements CustomPayload {
        public static final Id<Cancel> ID=id("cancel");
        public Id<Cancel> getId() { return ID; }
        public static final PacketCodec<RegistryByteBuf,Cancel> CODEC=new PacketCodec<>() {
            public Cancel decode(RegistryByteBuf b) { return new Cancel(b.readString(64)); }
            public void encode(RegistryByteBuf b,Cancel p) { b.writeString(p.token,64); }
        };
    }
    public record Status(String token, boolean finished, String message) implements CustomPayload {
        public static final Id<Status> ID=id("status");
        public Id<Status> getId() { return ID; }
        public static final PacketCodec<RegistryByteBuf,Status> CODEC=new PacketCodec<>() {
            public Status decode(RegistryByteBuf b) { return new Status(b.readString(64),b.readBoolean(),b.readString(256)); }
            public void encode(RegistryByteBuf b,Status p) { b.writeString(p.token,64); b.writeBoolean(p.finished); b.writeString(p.message,256); }
        };
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(Preview.ID,Preview.CODEC);
        PayloadTypeRegistry.playS2C().register(Status.ID,Status.CODEC);
        PayloadTypeRegistry.playC2S().register(Submit.ID,Submit.CODEC);
        PayloadTypeRegistry.playC2S().register(Cancel.ID,Cancel.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Submit.ID,(p,c) -> c.server().execute(() -> CompetitiveTower.submitUi(c.player(),p)));
        ServerPlayNetworking.registerGlobalReceiver(Cancel.ID,(p,c) -> c.server().execute(() -> CompetitiveTower.cancelUi(c.player(),p.token())));
    }
    public static boolean available(ServerPlayerEntity player) {
        return ServerPlayNetworking.canSend(player,Preview.ID) && ServerPlayNetworking.canSend(player,Status.ID);
    }
    public static void status(ServerPlayerEntity player,String token,boolean finished,String message) {
        if (player!=null && available(player)) ServerPlayNetworking.send(player,new Status(token,finished,message));
    }
}
