package com.memoria.magicite.client;

import com.memoria.magicite.MemoriaMagicite;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MemoriaMagicite.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GauntletFirstPersonLayer {
    private GauntletFirstPersonLayer() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        if (!e.getItemStack().is(MemoriaMagicite.GAUNTLET.get())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isInvisible()) return;
        EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) return;

        AbstractClientPlayer player = mc.player;
        HumanoidArm side = e.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = side == HumanoidArm.RIGHT;
        float handSign = right ? 1.0f : -1.0f;
        final float PI = (float) Math.PI;
        float swing = e.getSwingProgress();
        float swingSqrt = Mth.sqrt(swing);

        PoseStack pose = e.getPoseStack();
        pose.pushPose();

        float swingX = -0.3f * Mth.sin(swingSqrt * PI);
        float swingY = 0.4f * Mth.sin(swingSqrt * (PI * 2.0f));
        float swingZ = -0.4f * Mth.sin(swing * PI);
        pose.translate(handSign * (swingX + 0.64000005f),
                swingY - 0.6f - e.getEquipProgress() * 0.6f,
                swingZ - 0.71999997f);
        pose.mulPose(Axis.YP.rotationDegrees(handSign * 45.0f));
        float swingSquaredSin = Mth.sin(swing * swing * PI);
        float swingArcSin = Mth.sin(swingSqrt * PI);
        pose.mulPose(Axis.YP.rotationDegrees(handSign * swingArcSin * 70.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(handSign * swingSquaredSin * -20.0f));
        pose.translate(handSign * -1.0f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(handSign * 120.0f));
        pose.mulPose(Axis.XP.rotationDegrees(200.0f));
        pose.mulPose(Axis.YP.rotationDegrees(handSign * -135.0f));
        pose.translate(handSign * 5.6f, 0.0f, 0.0f);

        if (right) playerRenderer.renderRightHand(pose, e.getMultiBufferSource(), e.getPackedLight(), player);
        else playerRenderer.renderLeftHand(pose, e.getMultiBufferSource(), e.getPackedLight(), player);

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        GauntletArmLayer.renderOnArm(pose, e.getMultiBufferSource(), e.getPackedLight(),
                e.getItemStack(), right ? model.rightArm : model.leftArm, right,player.getUUID());
        pose.popPose();

        e.setCanceled(true);
    }
}
