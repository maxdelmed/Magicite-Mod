package com.memoria.magicite.client;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.item.MagiciteItem;
import com.memoria.magicite.spell.FusionNames;
import com.memoria.magicite.spell.RuneSigils;
import java.util.Locale;
import com.memoria.magicite.net.MagiciteNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MemoriaMagicite.ID, value = Dist.CLIENT)
public final class RingRenderer {
    private static final Map<UUID, Preview> PREVIEWS = new HashMap<>();
    private static Level lastLevel;
    private record EnvironmentPreview(byte element, List<com.memoria.magicite.spell.SpellWorldInteractions.FootprintCell> cells, long expires) {}
    private static final Map<UUID, EnvironmentPreview> ENVIRONMENT_PREVIEWS = new HashMap<>();
    private record Preview(Vec3 pos, int color, float radius, int charge, int required,
                           String fusion, boolean linked, boolean sacrifice, boolean minorInteraction, long expires) {}
    private record VisualFx(int type,Vec3 pos,int color,float radius,long expires,String spellId) {}
    private static final Map<UUID,VisualFx> GAUNTLET_FLASH=new HashMap<>();
    private record IceWarning(long firstSeen,long expires) {}
    private static final Map<BlockPos,IceWarning> ICE_WARNINGS=new HashMap<>();
    private static final java.util.List<VisualFx> AFTERGLOWS=new java.util.ArrayList<>();
    private record PassiveHint(long expires) {}
    private static PassiveHint passiveHint;
    public static void acceptInteractionHint(MagiciteNetwork.InteractionHintPacket packet) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || !mc.player.isShiftKeyDown())return;
        passiveHint=new PassiveHint(mc.level.getGameTime()+6L);
    }

    public static float chargeFraction(UUID caster) {
        if(caster==null)return -1f;
        Preview state=PREVIEWS.get(caster);
        Minecraft mc=Minecraft.getInstance();
        return mc.level==null || state==null || state.expires<mc.level.getGameTime() ? -1f :
            Math.max(0f,Math.min(1f,(float)state.charge/Math.max(1,state.required)));
    }
    public static boolean flash(UUID caster) {return flashStrength(caster)>0f;}
    public static float flashStrength(UUID caster) {
        Minecraft mc=Minecraft.getInstance();
        VisualFx fx=caster==null?null:GAUNTLET_FLASH.get(caster);
        if(mc.level==null || fx==null)return 0f;
        return Math.max(0f,Math.min(1f,(fx.expires-mc.level.getGameTime())/10f));
    }
    public static void acceptVisual(MagiciteNetwork.VisualPacket p) {
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        if(lastLevel!=mc.level) {PREVIEWS.clear();ENVIRONMENT_PREVIEWS.clear();
            ICE_WARNINGS.clear();GAUNTLET_FLASH.clear();AFTERGLOWS.clear();passiveHint=null;lastLevel=mc.level;}
        long now=mc.level.getGameTime();
        if(p.type()==1)GAUNTLET_FLASH.put(p.caster(),new VisualFx(1,new Vec3(p.x(),p.y(),p.z()),p.rgb(),p.radius(),now+10,p.spellId()));
        else if(p.type()==3)AFTERGLOWS.add(new VisualFx(3,new Vec3(p.x(),p.y(),p.z()),p.rgb(),p.radius(),now+32,p.spellId()));
        else if(p.type()==2) {
            for(int i=0;i<6;i++)mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    p.x()+Math.sin(i)*.14,p.y()+Math.cos(i)*.12,p.z()+Math.cos(i*2d)*.14,0,.015,0);
        }
    }
    public static void acceptIceWarnings(MagiciteNetwork.IceWarningPacket p) {
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        for(BlockPos pos:p.positions()) {
            long now=mc.level.getGameTime();
            IceWarning prior=ICE_WARNINGS.get(pos);
            ICE_WARNINGS.put(pos,new IceWarning(prior==null || prior.expires<now?now:prior.firstSeen,now+14));
        }
    }

    public static void acceptFootprint(MagiciteNetwork.FootprintPacket packet) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null)return;
        if(lastLevel!=mc.level){ PREVIEWS.clear();ENVIRONMENT_PREVIEWS.clear();lastLevel=mc.level; }
        ENVIRONMENT_PREVIEWS.put(packet.caster(),new EnvironmentPreview(packet.element(),packet.cells(),mc.level.getGameTime()+9L));
    }
    public static void accept(MagiciteNetwork.RingPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (lastLevel != mc.level) { PREVIEWS.clear(); ENVIRONMENT_PREVIEWS.clear();
            ICE_WARNINGS.clear();GAUNTLET_FLASH.clear();AFTERGLOWS.clear();passiveHint=null;lastLevel = mc.level; }
        Preview previous = PREVIEWS.get(packet.caster());
        boolean newlyLinked = packet.linked() && (previous == null || !previous.linked()
                || !previous.fusion().equals(packet.fusion()));
        PREVIEWS.put(packet.caster(), new Preview(new Vec3(packet.x(), packet.y(), packet.z()),
                packet.color(), packet.radius(), packet.charge(), packet.required(), packet.fusion(),
                packet.linked(), packet.sacrifice(), packet.minorInteraction(), mc.level.getGameTime() + 6));

        if (newlyLinked && packet.caster().equals(mc.player.getUUID())) {
            mc.player.playSound(MemoriaMagicite.RESONANCE_READY.get(), 0.55f, 1.15f);
        }
    }

    private static void clearExpired(Level level) {
        Iterator<Map.Entry<UUID, Preview>> entries = PREVIEWS.entrySet().iterator();
        while (entries.hasNext()) if (entries.next().getValue().expires() < level.getGameTime()) entries.remove();
        ENVIRONMENT_PREVIEWS.values().removeIf(p->p.expires()<level.getGameTime());
        ICE_WARNINGS.values().removeIf(warning->warning.expires<level.getGameTime());
        GAUNTLET_FLASH.values().removeIf(fx->fx.expires<level.getGameTime());
        AFTERGLOWS.removeIf(fx->fx.expires<level.getGameTime());
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { PREVIEWS.clear(); ENVIRONMENT_PREVIEWS.clear(); lastLevel = null; return; }
        if (lastLevel != mc.level) { PREVIEWS.clear(); ENVIRONMENT_PREVIEWS.clear();
            ICE_WARNINGS.clear();GAUNTLET_FLASH.clear();AFTERGLOWS.clear();passiveHint=null;lastLevel = mc.level; }
        clearExpired(mc.level);
        Vec3 camera = event.getCamera().getPosition();
        for(EnvironmentPreview planned : ENVIRONMENT_PREVIEWS.values())
            drawFootprint(event.getPoseStack(),camera,planned);
        drawIceWarnings(event.getPoseStack(),camera,mc.level);
        for(VisualFx fx:AFTERGLOWS)drawRune(event.getPoseStack(),camera,fx,mc.level.getGameTime());

        float rhythm=(float)(.5d+.5d*Math.sin(mc.level.getGameTime()*2d*Math.PI/16d));
        for (Map.Entry<UUID,Preview> entry : PREVIEWS.entrySet()) {
            Preview p=entry.getValue();

            circle(event.getPoseStack(), camera, p.pos(), p.color(), p.radius(), p.linked(), p.charge() >= p.required(),rhythm);
            if(p.linked()) {
                net.minecraft.world.entity.player.Player linkedPlayer=mc.level.getPlayerByUUID(entry.getKey());
                if(linkedPlayer!=null) resonanceBeam(event.getPoseStack(),camera,
                        linkedPlayer.position().add(0,1.2,0),p.pos().add(0,.16,0),rhythm);
            }
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        GuiGraphics gfx = event.getGuiGraphics();

        boolean holdingMagicite = mc.player.getMainHandItem().getItem() instanceof MagiciteItem
                || mc.player.getOffhandItem().getItem() instanceof MagiciteItem
                || mc.player.getMainHandItem().getItem() == MemoriaMagicite.GAUNTLET.get();
        if(!mc.player.isShiftKeyDown() || !holdingMagicite) passiveHint=null;
        boolean passiveAvailable=mc.player.isShiftKeyDown() && holdingMagicite && !mc.player.isUsingItem()
                && passiveHint!=null && passiveHint.expires()>=mc.level.getGameTime();
        if(passiveAvailable) drawInteractionGlyph(gfx,
                event.getWindow().getGuiScaledWidth()/2 + 12,
                event.getWindow().getGuiScaledHeight()/2 + 12);
        Preview own = PREVIEWS.get(mc.player.getUUID());
        if (own == null || own.expires() < mc.level.getGameTime() || !mc.player.isUsingItem()) return;
        int cx = event.getWindow().getGuiScaledWidth() / 2;
        int cy = event.getWindow().getGuiScaledHeight() / 2;
        int color = own.linked() ? 0xFFF2CE70 : own.charge() >= own.required() ? 0xFF8CF2C9 : 0xFF8BC9F2;
        float fraction = Math.max(0, Math.min(1, (float)own.charge() / Math.max(1, own.required())));

        int steps = 36, active = (int)Math.ceil(steps * fraction);
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps - Math.PI / 2;
            int x = cx + (int)Math.round(Math.cos(a) * 20);
            int y = cy + (int)Math.round(Math.sin(a) * 20);
            gfx.fill(x - 1, y - 1, x + 2, y + 2, i < active ? color : 0x88576777);
        }
        String label;
        if (own.linked()) label = "FUSION LINKED: " + FusionNames.display(own.fusion()).toUpperCase(Locale.ROOT);
        else if (fraction < 1) label = "CHARGING " + (int)(fraction * 100) + "%";
        else if (!own.fusion().isEmpty()) label = "READY: " + FusionNames.display(own.fusion()).toUpperCase(Locale.ROOT);
        else label = "READY TO CAST";
        int width = mc.font.width(label);
        gfx.drawString(mc.font, label, cx - width / 2, cy + 33, color, true);

        if (mc.player.isShiftKeyDown() && own.minorInteraction() && !own.linked() && !own.sacrifice())
            drawInteractionGlyph(gfx,cx+12,cy+12);
        if (own.sacrifice()) {
            String warning = "This will have consequences.";
            gfx.drawString(mc.font, warning, cx - mc.font.width(warning) / 2, cy + 47, 0xFFFF7777, true);
        } else if (!own.minorInteraction() && !own.linked() && !own.fusion().isEmpty()) {
            String hint = fraction < 1 ? "Resonating - charge to link" : "Release for solo fusion / sync with allies";
            gfx.drawString(mc.font, hint, cx - mc.font.width(hint) / 2, cy + 47, 0xFFD6DBE4, true);
        }
    }

    private static void drawInteractionGlyph(GuiGraphics gfx,int cx,int cy) {
        final int outline=0xDD26303B;
        final int glow=0xFFE8E0BC;
        gfx.fill(cx-1,cy-5,cx+2,cy-3,outline);
        gfx.fill(cx-3,cy-3,cx+4,cy-1,outline);
        gfx.fill(cx-5,cy-1,cx+6,cy+2,outline);
        gfx.fill(cx-3,cy+2,cx+4,cy+4,outline);
        gfx.fill(cx-1,cy+4,cx+2,cy+6,outline);
        gfx.fill(cx-1,cy-3,cx+2,cy-1,glow);
        gfx.fill(cx-3,cy-1,cx+4,cy+2,glow);
        gfx.fill(cx-1,cy+2,cx+2,cy+4,glow);
    }

    private static void drawFootprint(PoseStack pose,Vec3 camera,EnvironmentPreview planned) {
        if(planned.cells().isEmpty())return;
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        Matrix4f m=pose.last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);
        for(com.memoria.magicite.spell.SpellWorldInteractions.FootprintCell cell:planned.cells()) {
            BlockPos pos=cell.pos();
            int color=cell.valid()?(planned.element()==1?0x80E7FF:planned.element()==3?0xC69AFF:0xFFAC61):0xFF6C71;
            int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
            int alpha=cell.valid()?185:125;
            float x=pos.getX(),z=pos.getZ();
            float y=pos.getY()+1.04f;
            float[] xx={x+0.09f,x+0.91f,x+0.91f,x+0.09f};
            float[] zz={z+0.09f,z+0.09f,z+0.91f,z+0.91f};
            for(int i=0;i<4;i++) {
                int j=(i+1)%4;
                buffer.vertex(m,xx[i],y,zz[i]).color(r,g,b,alpha).endVertex();
                buffer.vertex(m,xx[j],y,zz[j]).color(r,g,b,alpha).endVertex();
            }
            if(!cell.valid()) {
                buffer.vertex(m,x+0.2f,y,z+0.2f).color(r,g,b,alpha).endVertex();
                buffer.vertex(m,x+0.8f,y,z+0.8f).color(r,g,b,alpha).endVertex();
            }
            if(planned.element()==3 && cell.valid()) {

                float[] qx={x+.2f,x+.52f,x+.40f,x+.80f};
                float[] qz={z+.24f,z+.44f,z+.56f,z+.78f};
                for(int j=0;j<3;j++) {
                    buffer.vertex(m,qx[j],y+.035f,qz[j]).color(238,212,255,220).endVertex();
                    buffer.vertex(m,qx[j+1],y+.035f,qz[j+1]).color(238,212,255,220).endVertex();
                }
            }
            if(ClientVisualConfig.HIGH_CONTRAST.get()) {

                for(int j=0;j<(cell.valid()?2:1);j++) {
                    float offset=cell.valid() ? j*.28f : 0f;
                    buffer.vertex(m,x+.22f+offset,y+.025f,z+.18f).color(255,255,255,240).endVertex();
                    buffer.vertex(m,x+.34f+offset,y+.025f,z+.82f).color(255,255,255,240).endVertex();
                }
                if(!cell.valid()) {
                    buffer.vertex(m,x+.8f,y+.025f,z+.2f).color(255,255,255,240).endVertex();
                    buffer.vertex(m,x+.2f,y+.025f,z+.8f).color(255,255,255,240).endVertex();
                }
            }
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();pose.popPose();
    }

    private static void drawIceWarnings(PoseStack pose,Vec3 camera,Level level) {
        if(ICE_WARNINGS.isEmpty())return;
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        Matrix4f m=pose.last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);
        int quality=ClientVisualConfig.PARTICLE_QUALITY.get();
        int drawn=0;
        for(Map.Entry<BlockPos,IceWarning> entry:ICE_WARNINGS.entrySet()) {
            BlockPos pos=entry.getKey();
            if(pos.distSqr(BlockPos.containing(camera))>36d*36d ||
                    (quality==0 && drawn++>24) || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.PACKED_ICE))continue;
            float x=pos.getX()+.5f,y=pos.getY()+1.02f,z=pos.getZ()+.5f;

            long age=level.getGameTime()-entry.getValue().firstSeen;
            int n=(int)Math.max(1,Math.min(4,1+age/40L));
            for(int j=0;j<n;j++) {
                float angle=(float)(j*2.39996d+pos.hashCode()*.002d);
                float x0=x+(float)Math.cos(angle)*.05f,z0=z+(float)Math.sin(angle)*.05f;
                float x1=x+(float)Math.cos(angle)*(.15f+.07f*j),z1=z+(float)Math.sin(angle)*(.15f+.07f*j);
                b.vertex(m,x0,y,z0).color(225,246,255,210).endVertex();
                b.vertex(m,x1,y,z1).color(185,225,250,180).endVertex();
            }
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.disableBlend();pose.popPose();
    }

    private static void drawRune(PoseStack pose,Vec3 camera,VisualFx fx,long now) {
        if(fx.pos.distanceToSqr(camera)>48d*48d)return;
        float remaining=(float)Math.max(0d,Math.min(1d,(fx.expires-now)/32d));
        int alpha=(int)(215*remaining);
        float radius=Math.min(fx.radius*.45f,3.3f);
        double rotation=(32d-(fx.expires-now))*.014d;
        double cos=Math.cos(rotation),sin=Math.sin(rotation);
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        Matrix4f m=pose.last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);
        int c=fx.color,rr=c>>16&255,gg=c>>8&255,bb=c&255;
        float x=(float)fx.pos.x,y=(float)fx.pos.y+.09f,z=(float)fx.pos.z;
        for(RuneSigils.Stroke stroke:RuneSigils.forSpell(fx.spellId())) {
            float weight=stroke.layer()==1?1f:stroke.layer()==2?.58f:.80f;
            int aa=Math.min(255,(int)(alpha*weight));
            float x1=x+(float)((stroke.x1()*cos-stroke.z1()*sin)*radius);
            float z1=z+(float)((stroke.x1()*sin+stroke.z1()*cos)*radius);
            float x2=x+(float)((stroke.x2()*cos-stroke.z2()*sin)*radius);
            float z2=z+(float)((stroke.x2()*sin+stroke.z2()*cos)*radius);
            b.vertex(m,x1,y,z1).color(rr,gg,bb,aa).endVertex();
            b.vertex(m,x2,y,z2).color(rr,gg,bb,aa).endVertex();
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.disableBlend();pose.popPose();
    }

    private static void resonanceBeam(PoseStack pose,Vec3 camera,Vec3 start,Vec3 end,float rhythm) {
        pose.pushPose(); pose.translate(-camera.x,-camera.y,-camera.z);
        Matrix4f matrix=pose.last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);
        int alpha=170+(int)(85f*rhythm);
        buffer.vertex(matrix,(float)start.x,(float)start.y,(float)start.z).color(255,231,135,alpha).endVertex();
        buffer.vertex(matrix,(float)end.x,(float)end.y,(float)end.z).color(255,197,85,alpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();pose.popPose();
    }

    private static void circle(PoseStack pose, Vec3 camera, Vec3 center, int rgb, float radius,
                               boolean linked, boolean ready, float rhythm) {
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = pose.last().pose();
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int rings = linked ? 3 : 2;
        for (int ring = 0; ring < rings; ring++) {
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            double pulse = linked && ready ? 1.0 + 0.05d*rhythm : 1.0;
            double factor = (ring == 0 ? 1 : ring == 1 ? 0.78 : 1.05) * pulse;
            double height = ring == 2 ? 0.065 : 0.05;
            int cr = ring == 2 ? 255 : r, cg = ring == 2 ? 214 : g, cb = ring == 2 ? 107 : b;
            for (int i = 0; i <= 96; i++) {
                double a = 2 * Math.PI * i / 96d;
                buffer.vertex(matrix, (float)(center.x + Math.cos(a) * radius * factor),
                                (float)(center.y + height), (float)(center.z + Math.sin(a) * radius * factor))
                        .color(cr, cg, cb, ring == 1 ? 90 : linked ? 170+(int)(85*rhythm) : 230).endVertex();
            }
            BufferUploader.drawWithShader(buffer.end());
        }
        RenderSystem.enableCull(); RenderSystem.disableBlend();
        pose.popPose();
    }
}
