package com.memoria.magicite.client;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.item.MagiciteItem;
import com.memoria.magicite.menu.GauntletStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class GauntletRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation METAL = tex("gauntlet_metal");
    private static final ResourceLocation DARK = tex("gauntlet_dark");
    private static final ResourceLocation GOLD = tex("gauntlet_gold");
    private static final ResourceLocation SOCKET = tex("gauntlet_socket");
    private static final ResourceLocation JEWEL = tex("gauntlet_jewel");
    private static final ResourceLocation RUNE = tex("gauntlet_rune");

    public GauntletRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(MemoriaMagicite.ID, "textures/item/" + name + ".png");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {

        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return;

        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {

            pose.translate(0.50f, 0.91f, 0.50f);
            pose.mulPose(Axis.YP.rotationDegrees(-12.0f));
            pose.mulPose(Axis.XP.rotationDegrees(8.0f));
            pose.scale(1.48f, 1.48f, 1.48f);
            drawSleeve(stack, pose, buffers, light, overlay);
        } else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {

            pose.translate(0.50f, 0.89f, 0.50f);
            pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            pose.scale(0.76f, 0.76f, 0.76f);
            drawSleeve(stack, pose, buffers, light, overlay);
        } else {
            pose.translate(0.5f, 0.92f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(-15.0f));
            pose.scale(1.26f, 1.26f, 1.26f);
            drawSleeve(stack, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    public static void drawSleeve(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        drawSleeve(stack,pose,buffers,light,overlay,null);
    }
    public static void drawSleeve(ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                                  int light, int overlay, java.util.UUID caster) {
        pose.pushPose();

        pose.scale(1.0f, 1.06f, 1.0f);

        box(pose,buffers,light,overlay,DARK, -0.130f,-0.348f,-0.139f, 0.130f,0.045f,0.139f);

        box(pose,buffers,light,overlay,GOLD, -0.145f,-0.354f,-0.150f, 0.145f,-0.313f,0.159f);
        box(pose,buffers,light,overlay,METAL,-0.133f,-0.311f,-0.149f, 0.133f,-0.280f,0.150f);

        bevelPlate(pose,buffers,light,overlay,GOLD,-0.140f,-0.330f,0.140f,-0.118f,0.177f,0.025f);
        bevelPlate(pose,buffers,light,overlay,METAL,-0.119f,-0.308f,0.119f,-0.140f,0.187f,0.023f);
        bevelPlate(pose,buffers,light,overlay,DARK,-0.085f,-0.285f,0.085f,-0.162f,0.196f,0.020f);
        jewelFace(pose,buffers,light,overlay,RUNE,-0.046f,-0.270f,0.207f,0.046f,-0.177f);

        for (float side : new float[]{-1f,1f}) {
            float x = side * 0.134f;
            box(pose,buffers,light,overlay,GOLD,
                    x-0.017f,-0.246f,-0.119f, x+0.017f,-0.200f,0.135f);
            box(pose,buffers,light,overlay,METAL,
                    x-0.015f,-0.215f,-0.117f, x+0.015f,-0.190f,0.137f);
        }

        box(pose,buffers,light,overlay,DARK,
                -0.121f,-0.151f,-0.137f, 0.121f,0.065f,0.198f);

        box(pose,buffers,light,overlay,GOLD,
                -0.145f,-0.147f,-0.151f, 0.145f,-0.111f,0.208f);

        bevelPlate(pose,buffers,light,overlay,GOLD,-0.156f,-0.135f,0.156f,0.066f,0.203f,0.032f);
        bevelPlate(pose,buffers,light,overlay,METAL,-0.141f,-0.122f,0.141f,0.054f,0.213f,0.025f);
        bevelPlate(pose,buffers,light,overlay,DARK,-0.127f,-0.108f,0.127f,0.045f,0.224f,0.021f);

        ItemStack[] crystals = GauntletStorage.read(stack);
        int tier = GauntletStorage.tier(stack);
        int installed=0;for(int i=0;i<tier;i++)if(crystals[i].getItem() instanceof MagiciteItem)installed++;
        float progress=RingRenderer.chargeFraction(caster);
        float releaseGlow=RingRenderer.flashStrength(caster);
        int lit=progress<0f?0:Math.min(installed,(int)Math.ceil(progress*installed));
        int seen=0;

        final float[] sx = {-0.070f,0.070f,0.0f};
        final float[] sy = {-0.084f,-0.084f,0.016f};
        for (int i = 0; i < 3; ++i) {
            float x=sx[i], y=sy[i];
            bevelPlate(pose,buffers,light,overlay,GOLD,x-0.051f,y-0.046f,x+0.051f,y+0.046f,0.233f,0.012f);
            bevelPlate(pose,buffers,light,overlay,SOCKET,x-0.044f,y-0.040f,x+0.044f,y+0.040f,0.241f,0.009f);
            if (i < tier && crystals[i].getItem() instanceof MagiciteItem magic) {
                ResourceLocation sprite = tex(magic.kind().id + "_magicite");
                int illuminated=++seen<=lit?0xF000F0:fadeGemLight(light,releaseGlow);
                jewelFace(pose,buffers,illuminated,overlay,sprite,x-0.033f,y-0.029f,0.248f,x+0.033f,y+0.029f);
            }
        }

        for (float x : new float[]{-0.092f,-0.030f,0.032f,0.094f}) {
            box(pose,buffers,light,overlay,GOLD,x-0.020f,0.052f,0.157f,x+0.020f,0.085f,0.188f);
            box(pose,buffers,light,overlay,METAL,x-0.015f,0.070f,0.183f,x+0.015f,0.083f,0.190f);
        }

        if (tier >= 4) {
            float y = -0.015f;

            box(pose,buffers,light,overlay,DARK,
                    -0.106f,-0.119f,-0.174f, 0.106f,0.073f,-0.132f);

            box(pose,buffers,light,overlay,GOLD,
                    -0.126f,-0.123f,-0.173f, -0.106f,0.065f,-0.144f);
            box(pose,buffers,light,overlay,GOLD,
                     0.106f,-0.123f,-0.173f,  0.126f,0.065f,-0.144f);

            box(pose,buffers,light,overlay,GOLD,
                    -0.089f,y-0.082f,-0.187f, 0.089f,y+0.082f,-0.171f);
            box(pose,buffers,light,overlay,SOCKET,
                    -0.067f,y-0.060f,-0.207f, 0.067f,y+0.060f,-0.184f);
            box(pose,buffers,light,overlay,DARK,
                    -0.054f,y-0.049f,-0.217f, 0.054f,y+0.049f,-0.206f);
            if (crystals[3].getItem() instanceof MagiciteItem magic) {
                ResourceLocation sprite = tex(magic.kind().id + "_magicite");
                int illuminated=++seen<=lit?0xF000F0:fadeGemLight(light,releaseGlow);
                jewelFaceBack(pose,buffers,illuminated,overlay,sprite,
                        -0.041f,y-0.037f,-0.229f,0.041f,y+0.037f);
            }
        }
        pose.popPose();
    }

    private static int fadeGemLight(int original,float strength) {
        if(strength<=0f)return original;
        int block=original&0xFFFF,sky=(original>>>16)&0xFFFF;
        block=Math.min(240,Math.round(block+(240-block)*strength));
        sky=Math.min(240,Math.round(sky+(240-sky)*strength));
        return (sky<<16)|block;
    }

    private static void bevelPlate(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                   ResourceLocation texture, float x0, float y0, float x1, float y1,
                                   float z, float bevel) {
        float b=Math.min(bevel, Math.min((x1-x0)/3f,(y1-y0)/3f));
        float[] xs={x0+b,x1-b,x1,x1,x1-b,x0+b,x0,x0};
        float[] ys={y0,y0,y0+b,y1-b,y1,y1,y1-b,y0+b};
        float cx=(x0+x1)/2f,cy=(y0+y1)/2f;
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for(int i=0;i<8;i++) {
            int j=(i+1)%8;
            quad(pose,v,light,overlay,cx,cy,z,xs[i],ys[i],z,xs[j],ys[j],z,cx,cy,z,0f,0f,1f);
        }
    }

    private static void jewelFaceBack(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                      ResourceLocation texture, float x0,float y0,float z,float x1,float y1) {
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        quad(pose,v,light,overlay,x1,y0,z,x0,y0,z,x0,y1,z,x1,y1,z,0,0,-1);
    }

    private static void jewelFace(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                  ResourceLocation texture, float x0,float y0,float z,float x1,float y1) {

        VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        quad(pose, v, light, overlay, x0,y0,z, x1,y0,z, x1,y1,z, x0,y1,z, 0,0,1);
    }

    private static void box(PoseStack pose, MultiBufferSource buffers, int light,int overlay,ResourceLocation tex,
                            float x0,float y0,float z0,float x1,float y1,float z1) {
        VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        quad(pose,v,light,overlay,x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, 0,0,-1);
        quad(pose,v,light,overlay,x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, 0,0,1);
        quad(pose,v,light,overlay,x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, -1,0,0);
        quad(pose,v,light,overlay,x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, 1,0,0);
        quad(pose,v,light,overlay,x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, 0,1,0);
        quad(pose,v,light,overlay,x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, 0,-1,0);
    }
    private static void quad(PoseStack pose, VertexConsumer out, int light,int overlay,
                             float x0,float y0,float z0,float x1,float y1,float z1,
                             float x2,float y2,float z2,float x3,float y3,float z3,
                             float nx,float ny,float nz) {
        Matrix4f m = pose.last().pose(); Matrix3f normal = pose.last().normal();
        vertex(out,m,normal,x0,y0,z0,0,1,nx,ny,nz,light,overlay);
        vertex(out,m,normal,x1,y1,z1,1,1,nx,ny,nz,light,overlay);
        vertex(out,m,normal,x2,y2,z2,1,0,nx,ny,nz,light,overlay);
        vertex(out,m,normal,x3,y3,z3,0,0,nx,ny,nz,light,overlay);
    }
    private static void vertex(VertexConsumer v, Matrix4f m, Matrix3f n, float x,float y,float z,
                               float u,float w,float nx,float ny,float nz,int light,int overlay) {
        v.vertex(m,x,y,z).color(255,255,255,255).uv(u,w).overlayCoords(overlay).uv2(light)
                .normal(n,nx,ny,nz).endVertex();
    }
}
