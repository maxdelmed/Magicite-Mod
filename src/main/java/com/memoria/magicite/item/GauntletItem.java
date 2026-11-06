package com.memoria.magicite.item;

import com.memoria.magicite.menu.GauntletMenu;
import com.memoria.magicite.spell.SpellEvents;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.memoria.magicite.client.GauntletRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class GauntletItem extends Item {
    public GauntletItem() { super(new Item.Properties().stacksTo(1)); }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GauntletRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new GauntletRenderer();
                return renderer;
            }
        });
    }
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Allows the fusion of magicite for solo play,").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("cooperative casting gives stronger results.").withStyle(ChatFormatting.GRAY));
    }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                java.util.List<com.memoria.magicite.spell.MagiciteKind> kinds=new java.util.ArrayList<>();
                ItemStack[] gems=com.memoria.magicite.menu.GauntletStorage.read(stack);
                for(int i=0;i<com.memoria.magicite.menu.GauntletStorage.tier(stack);i++)
                    if(gems[i].getItem() instanceof MagiciteItem m)kinds.add(m.kind());
                com.memoria.magicite.spell.SpellSounds.charge(server,player,kinds);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            MenuProvider provider = new SimpleMenuProvider((id, inv, p) -> new GauntletMenu(id, inv, hand), Component.translatable("item.memoria_magicite.fusion_gauntlet"));
            NetworkHooks.openScreen(sp, provider, buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity holder, int remaining) {
        if (!level.isClientSide && holder instanceof ServerPlayer player) {
            SpellEvents.castFromRelease(player, player.getUsedItemHand(), true, getUseDuration(stack) - remaining);
        }
    }
}
