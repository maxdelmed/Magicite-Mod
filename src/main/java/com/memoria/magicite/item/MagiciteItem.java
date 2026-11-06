package com.memoria.magicite.item;

import com.memoria.magicite.spell.MagiciteKind;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import com.memoria.magicite.spell.SpellEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class MagiciteItem extends Item {
    private final MagiciteKind kind;

    public MagiciteItem(MagiciteKind kind) {
        super(new Item.Properties().durability(kind.durability).setNoRepair());
        this.kind = kind;
    }
    public MagiciteKind kind() { return kind; }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (kind == MagiciteKind.PURE_MEMORY) {
            String[] poem = {
                    "The first laugh of a baby,",
                    "the final breath of a dying sun,",
                    "the electric rush of running free,",
                    "and snowy fields where children run."
            };
            for (String line : poem) {
                tooltip.add(Component.literal(line).withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFF55FF)).withItalic(true)));
            }
        } else {
            tooltip.add(Component.literal(com.memoria.magicite.spell.FusionDescriptions.forSingle(kind)).withStyle(ChatFormatting.GRAY));
        }
    }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            com.memoria.magicite.spell.SpellSounds.charge(server,player,java.util.List.of(kind));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity holder, int remaining) {
        if (!level.isClientSide && holder instanceof ServerPlayer player) {
            SpellEvents.castFromRelease(player, player.getUsedItemHand(), false, getUseDuration(stack) - remaining);
        }
    }
}
