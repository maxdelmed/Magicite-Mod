package com.memoria.magicite.net;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.client.RingRenderer;
import com.memoria.magicite.menu.GauntletMenu;
import com.memoria.magicite.spell.SpellWorldInteractions;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

public final class MagiciteNetwork {
    private MagiciteNetwork() {}
    private static final String VERSION = "11";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MemoriaMagicite.ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, UpgradePacket.class, UpgradePacket::encode, UpgradePacket::decode, UpgradePacket::handle);
        CHANNEL.registerMessage(1, RingPacket.class, RingPacket::encode, RingPacket::decode, RingPacket::handle);
        CHANNEL.registerMessage(2, FootprintPacket.class, FootprintPacket::encode, FootprintPacket::decode, FootprintPacket::handle);
        CHANNEL.registerMessage(3, SocketActionPacket.class, SocketActionPacket::encode, SocketActionPacket::decode, SocketActionPacket::handle);
        CHANNEL.registerMessage(4, VisualPacket.class, VisualPacket::encode, VisualPacket::decode, VisualPacket::handle);
        CHANNEL.registerMessage(5, IceWarningPacket.class, IceWarningPacket::encode, IceWarningPacket::decode, IceWarningPacket::handle);
        CHANNEL.registerMessage(6, ClientQualityPacket.class, ClientQualityPacket::encode, ClientQualityPacket::decode, ClientQualityPacket::handle);
        CHANNEL.registerMessage(7, InteractionHintPacket.class, InteractionHintPacket::encode, InteractionHintPacket::decode, InteractionHintPacket::handle);
    }

    public record SocketActionPacket(int action,int a,int b) {
        public static void encode(SocketActionPacket p,FriendlyByteBuf buf) {buf.writeByte(p.action);buf.writeByte(p.a);buf.writeByte(p.b);}
        public static SocketActionPacket decode(FriendlyByteBuf buf) {return new SocketActionPacket(buf.readByte(),buf.readByte(),buf.readByte());}
        public static void handle(SocketActionPacket p,Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx=context.get();
            ctx.enqueueWork(()->{
                ServerPlayer player=ctx.getSender();
                if(player==null || !(player.containerMenu instanceof GauntletMenu menu))return;
                if(p.action==0)menu.requestSwap(player,p.a,p.b);
            });ctx.setPacketHandled(true);
        }
    }
    public static void socketAction(int action,int a,int b) {CHANNEL.sendToServer(new SocketActionPacket(action,a,b));}
    public static void sendUpgrade() { CHANNEL.sendToServer(new UpgradePacket()); }
    public static void sendRing(ServerPlayer viewer, UUID caster, double x, double y, double z,
                                int color, float radius, int charge, int required, String fusion, boolean linked, boolean sacrifice, boolean minorInteraction) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new RingPacket(caster, x, y, z, color, radius, charge, required, fusion, linked, sacrifice, minorInteraction));
    }

    public static void sendFootprint(ServerPlayer viewer, UUID caster, byte element,
                    List<SpellWorldInteractions.FootprintCell> cells) {
        if(cells.isEmpty())return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),new FootprintPacket(caster,element,List.copyOf(cells)));
    }

    public record VisualPacket(UUID caster,int type,double x,double y,double z,int rgb,float radius,String spellId) {
        public static void encode(VisualPacket p,FriendlyByteBuf b) {
            b.writeUUID(p.caster);b.writeByte(p.type);b.writeDouble(p.x);b.writeDouble(p.y);
            b.writeDouble(p.z);b.writeInt(p.rgb);b.writeFloat(p.radius);b.writeUtf(p.spellId,48);
        }
        public static VisualPacket decode(FriendlyByteBuf b) {
            return new VisualPacket(b.readUUID(),b.readByte(),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt(),b.readFloat(),b.readUtf(48));
        }
        public static void handle(VisualPacket p,Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx=c.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->RingRenderer.acceptVisual(p)));
            ctx.setPacketHandled(true);
        }
    }
    public static void sendVisual(net.minecraft.server.level.ServerLevel world,UUID caster,int type,
            double x,double y,double z,int color,float radius) {
        sendVisual(world,caster,type,x,y,z,color,radius,"");
    }

    public static void sendVisual(net.minecraft.server.level.ServerLevel world,UUID caster,int type,
            double x,double y,double z,int color,float radius,String spellId) {
        for(ServerPlayer viewer:world.players()) {
            if(viewer.distanceToSqr(x,y,z)<=64d*64d)
                CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new VisualPacket(caster,type,x,y,z,color,radius,spellId));
        }
    }
    public record IceWarningPacket(List<BlockPos> positions) {
        public static void encode(IceWarningPacket p,FriendlyByteBuf b) {
            b.writeVarInt(p.positions.size());for(BlockPos pos:p.positions)b.writeBlockPos(pos);
        }
        public static IceWarningPacket decode(FriendlyByteBuf b) {
            int count=b.readVarInt();if(count<0 || count>72)throw new IllegalArgumentException("Ice warning overflow");
            List<BlockPos> result=new ArrayList<>();for(int i=0;i<count;i++)result.add(b.readBlockPos());
            return new IceWarningPacket(List.copyOf(result));
        }
        public static void handle(IceWarningPacket p,Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx=c.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->RingRenderer.acceptIceWarnings(p)));
            ctx.setPacketHandled(true);
        }
    }
    public static void sendIceWarnings(ServerPlayer viewer,List<BlockPos> blocks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new IceWarningPacket(List.copyOf(blocks)));
    }
    public record ClientQualityPacket(int quality) {
        public static void encode(ClientQualityPacket p,FriendlyByteBuf b) {b.writeByte(p.quality);}
        public static ClientQualityPacket decode(FriendlyByteBuf b) {return new ClientQualityPacket(b.readByte());}
        public static void handle(ClientQualityPacket p,Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx=c.get();ctx.enqueueWork(()->{
                ServerPlayer sender=ctx.getSender();if(sender!=null)com.memoria.magicite.spell.SpellParticles.setQuality(sender,p.quality);
            });ctx.setPacketHandled(true);
        }
    }
    public static void sendQuality(int quality) {CHANNEL.sendToServer(new ClientQualityPacket(quality));}

    public record InteractionHintPacket() {
        public static void encode(InteractionHintPacket p,FriendlyByteBuf b) {}
        public static InteractionHintPacket decode(FriendlyByteBuf b) {return new InteractionHintPacket();}
        public static void handle(InteractionHintPacket p,Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx=c.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->RingRenderer.acceptInteractionHint(p)));
            ctx.setPacketHandled(true);
        }
    }
    public static void sendInteractionHint(ServerPlayer viewer) {
        CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new InteractionHintPacket());
    }

    public record FootprintPacket(UUID caster, byte element, List<SpellWorldInteractions.FootprintCell> cells) {
        public static void encode(FootprintPacket p, FriendlyByteBuf buf) {
            buf.writeUUID(p.caster);buf.writeByte(p.element);
            buf.writeVarInt(Math.min(160,p.cells.size()));
            for(int i=0;i<Math.min(160,p.cells.size());i++) {
                SpellWorldInteractions.FootprintCell cell=p.cells.get(i);
                buf.writeBlockPos(cell.pos());buf.writeBoolean(cell.valid());
            }
        }
        public static FootprintPacket decode(FriendlyByteBuf buf) {
            UUID caster=buf.readUUID();byte element=buf.readByte();
            int count=buf.readVarInt();
            if(count<0 || count>160)throw new IllegalArgumentException("Invalid footprint preview size");
            List<SpellWorldInteractions.FootprintCell> cells=new ArrayList<>(count);
            for(int i=0;i<count;i++)cells.add(new SpellWorldInteractions.FootprintCell(buf.readBlockPos(),buf.readBoolean()));
            return new FootprintPacket(caster,element,List.copyOf(cells));
        }
        public static void handle(FootprintPacket packet,Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx=context.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->RingRenderer.acceptFootprint(packet)));
            ctx.setPacketHandled(true);
        }
    }
    public record UpgradePacket() {
        public static void encode(UpgradePacket packet, FriendlyByteBuf buf) {}
        public static UpgradePacket decode(FriendlyByteBuf buf) { return new UpgradePacket(); }
        public static void handle(UpgradePacket packet, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null && player.containerMenu instanceof GauntletMenu menu) menu.requestUpgrade(player);
            });
            ctx.setPacketHandled(true);
        }
    }
    public record RingPacket(UUID caster, double x, double y, double z, int color, float radius,
                             int charge, int required, String fusion, boolean linked, boolean sacrifice, boolean minorInteraction) {
        public static void encode(RingPacket p, FriendlyByteBuf buf) {
            buf.writeUUID(p.caster); buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z);
            buf.writeInt(p.color); buf.writeFloat(p.radius); buf.writeVarInt(p.charge); buf.writeVarInt(p.required);
            buf.writeUtf(p.fusion, 40); buf.writeBoolean(p.linked); buf.writeBoolean(p.sacrifice);
            buf.writeBoolean(p.minorInteraction);
        }
        public static RingPacket decode(FriendlyByteBuf buf) {
            return new RingPacket(buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readInt(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(40),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }
        public static void handle(RingPacket packet, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RingRenderer.accept(packet)));
            ctx.setPacketHandled(true);
        }
    }
}
