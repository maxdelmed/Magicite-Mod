package com.memoria.magicite.client;

import com.memoria.magicite.MemoriaMagicite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

public final class GauntletArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public GauntletArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack right = player.getMainHandItem();
        ItemStack left = player.getOffhandItem();

        boolean mainRight = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        if (right.is(MemoriaMagicite.GAUNTLET.get())) draw(pose, buffers, packedLight, right, mainRight,player.getUUID());
        if (left.is(MemoriaMagicite.GAUNTLET.get())) draw(pose, buffers, packedLight, left, !mainRight,player.getUUID());
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int packedLight,
                      ItemStack gauntlet, boolean rightArm, java.util.UUID caster) {
        ModelPart arm = rightArm ? getParentModel().rightArm : getParentModel().leftArm;
        if (!arm.visible) return;
        pose.pushPose();
        renderOnArm(pose, buffers, packedLight, gauntlet, arm, rightArm, caster);
        pose.popPose();
    }

    public static void renderOnArm(PoseStack pose, MultiBufferSource buffers, int light,
                                   ItemStack gauntlet, ModelPart arm, boolean rightArm, java.util.UUID caster) {
        pose.pushPose();
        arm.translateAndRotate(pose);

        pose.translate((rightArm ? -1.0f : 1.0f) / 16.0f, 11.2f / 16.0f, 0.0f);

        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(1.17f, 1.06f, 1.12f);
        GauntletRenderer.drawSleeve(gauntlet, pose, buffers, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,caster);
        pose.popPose();
    }
}
