package com.memoria.magicite.client;

import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.menu.GauntletMenu;
import com.memoria.magicite.net.MagiciteNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public final class GauntletScreen extends AbstractContainerScreen<GauntletMenu> {
    private static final int W = 222;
    private static final int H = 279;
    private static final int INVENTORY_FIRST_Y = 198;
    private static final int INVENTORY_HOTBAR_Y = 258;
    private static final int TEXT_W = 180;
    private Button upgrade;
    private int selectedSocket=-1;

    public GauntletScreen(GauntletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
        titleLabelX = 15;
        titleLabelY = 12;
        inventoryLabelX = 30;
        inventoryLabelY = 186;
    }

    @Override protected void init() {
        super.init();
        upgrade = addRenderableWidget(Button.builder(Component.literal("UNLOCK NEXT SOCKET"),
                button -> MagiciteNetwork.sendUpgrade()).bounds(leftPos + 30, topPos + 157, 162, 17).build());
    }

    @Override public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        int tier = menu.currentTier();
        upgrade.active = tier < 4;
        upgrade.setMessage(Component.literal(tier >= 4 ? "ALL SOCKETS UNLOCKED" :
                "UNLOCK SOCKET " + (tier + 1) + " - " +
                (tier == 1 ? MagiciteConfig.TIER_TWO_XP.get() :
                 tier == 2 ? MagiciteConfig.TIER_THREE_XP.get() :
                 MagiciteConfig.TIER_FOUR_XP.get()) + " LEVELS"));
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        rect(gfx, x, y, W, H, 0xFF191715);
        rect(gfx, x + 2, y + 2, W - 4, H - 4, 0xFF9A8060);
        rect(gfx, x + 4, y + 4, W - 8, H - 8, 0xFF464039);
        rect(gfx, x + 6, y + 6, W - 12, H - 12, 0xFF36322D);

        rect(gfx, x + 10, y + 10, 202, 29, 0xFF42392D);
        rect(gfx, x + 11, y + 11, 200, 1, 0xFF65573F);
        rect(gfx, x + 11, y + 38, 200, 1, 0xFF211E19);
        inset(gfx, x + 20, y + 43, 182, 35);
        for (int i = 0; i < 4; i++) {
            int sx = x + 43 + i * 42;
            int sy = y + 48;
            boolean unlocked = i < menu.currentTier();
            rect(gfx, sx - 3, sy - 3, 24, 24, unlocked ? 0xFFAD8650 : 0xFF514A40);
            rect(gfx, sx - 2, sy - 2, 22, 1, unlocked ? 0xFFE0BD7B : 0xFF655C4D);
            rect(gfx, sx - 2, sy - 1, 1, 21, unlocked ? 0xFFCAAA69 : 0xFF655C4D);
            rect(gfx, sx - 1, sy - 1, 20, 20, 0xFF201F1F);
            rect(gfx, sx, sy, 18, 18, unlocked ? 0xFF32302E : 0xFF272624);
            if (!unlocked) {
                rect(gfx, sx + 5, sy + 7, 8, 4, 0xFF403C34);
            }
            if (i==selectedSocket && unlocked) {
                rect(gfx,sx-4,sy-4,26,2,0xFFFFDA86);
                rect(gfx,sx-4,sy+20,26,2,0xFFFFDA86);
                rect(gfx,sx-4,sy-4,2,26,0xFFFFDA86);
                rect(gfx,sx+20,sy-4,2,26,0xFFFFDA86);
            }
        }

        inset(gfx, x + 15, y + 83, 192, 67);
        rect(gfx, x + 24, y + 101, 174, 1, 0xFF514532);

        rivet(gfx, x + 19, y + 87);
        rivet(gfx, x + 201, y + 87);

        rect(gfx, x + 14, y + 180, 194, 1, 0xFF1E1D1B);
        rect(gfx, x + 14, y + 181, 194, 1, 0xFF766346);
        rivet(gfx, x + 15, y + 14);
        rivet(gfx, x + 207, y + 14);
        rivet(gfx, x + 15, y + 268);
        rivet(gfx, x + 207, y + 268);

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 30 + col * 18;
                int sy = y + (row == 3 ? INVENTORY_HOTBAR_Y : INVENTORY_FIRST_Y + row * 18);
                rect(gfx, sx - 1, sy - 1, 18, 18, 0xFF1D1C1A);
                rect(gfx, sx - 1, sy - 1, 17, 1, 0xFF1B1917);
                rect(gfx, sx - 1, sy - 1, 1, 17, 0xFF1B1917);
                rect(gfx, sx, sy, 16, 16, 0xFF575047);
                rect(gfx, sx + 1, sy + 1, 15, 15, 0xFF282623);
            }
        }
    }

    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if (button == 0 && hasAltDown() && !hasShiftDown() && menu.getCarried().isEmpty()) {
            for (int i = 0; i < menu.currentTier(); i++) {
                int x = leftPos + 43 + i * 42, y = topPos + 48;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    if (selectedSocket < 0) selectedSocket = i;
                    else {
                        if (selectedSocket != i) MagiciteNetwork.socketAction(0, selectedSocket, i);
                        selectedSocket = -1;
                    }
                    return true;
                }
            }
        }

        selectedSocket = -1;
        return super.mouseClicked(mouseX,mouseY,button);
    }

    @Override protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, "FUSION GAUNTLET", titleLabelX, titleLabelY, 0xFFE8D3AC, false);
        gfx.drawString(font, "TIER " + roman(menu.currentTier()) + "   " + menu.installedCount() + "/" +
                menu.currentTier() + " MAGICITE LOADED", 15, 27, 0xFFBDB09A, false);

        gfx.drawString(font, "EQUIPPED SPELL", 24, 88, 0xFFC6AC80, false);
        String result = menu.currentFusionPreview();
        List<FormattedCharSequence> titleLines = font.split(Component.literal(result), TEXT_W);
        int titleY = 105;
        int titleColor = menu.hasFusion() ? 0xFFF1CA80 : 0xFFE4DDD0;
        for (FormattedCharSequence line : titleLines) {
            gfx.drawString(font, line, 24, titleY, titleColor, false);
            titleY += 10;
        }

        int descriptionY = Math.max(120, titleY + 2);
        String description = menu.currentFusionDescription();
        if (menu.pureMemoryAlone()) {

            String[] lines = description.split("\\n");
            int[] colors = {0xFFFF55FF, 0xFFFF55FF, 0xFFFF55FF, 0xFFFF55FF};
            int longest = 1;
            for (String line : lines) longest = Math.max(longest, font.width(line));
            float scale = Math.min(0.77f, 177f / (float)longest);
            gfx.pose().pushPose();
            gfx.pose().translate(24, descriptionY, 0);
            gfx.pose().scale(scale, scale, 1f);
            for (int i = 0; i < lines.length; i++) {
                gfx.drawString(font, lines[i], 0, i * 10, colors[Math.min(i, colors.length - 1)], false);
            }
            gfx.pose().popPose();
        } else {
            for (String paragraph : description.split("\\n")) {
                List<FormattedCharSequence> descriptionLines = font.split(Component.literal(paragraph), TEXT_W);
                for (FormattedCharSequence line : descriptionLines) {
                    gfx.drawString(font, line, 24, descriptionY, 0xFFBEBEB5, false);
                    descriptionY += 10;
                }
            }
        }
        gfx.drawString(font, "INVENTORY", inventoryLabelX, inventoryLabelY, 0xFFCFBEA4, false);
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    private static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private static void inset(GuiGraphics g, int x, int y, int w, int h) {
        rect(g, x, y, w, h, 0xFF1A1918);
        rect(g, x + 1, y + 1, w - 2, h - 2, 0xFF766247);
        rect(g, x + 2, y + 2, w - 4, h - 4, 0xFF262522);
        rect(g, x + 3, y + 3, w - 6, 1, 0xFF191918);
    }

    private static void rivet(GuiGraphics g, int x, int y) {
        rect(g, x, y, 3, 3, 0xFF171614);
        rect(g, x + 1, y, 2, 2, 0xFFD0AA71);
        rect(g, x + 1, y + 1, 1, 1, 0xFF765C37);
    }
}
