package com.memoria.magicite.menu;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.MagiciteConfig;
import com.memoria.magicite.item.MagiciteItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class GauntletStorage {
    private static final String INVENTORY_KEY = "MemoriaCrystals";
    private static final String TIER_KEY = "MemoriaTier";
    private GauntletStorage() {}

    public static int tier(ItemStack gauntlet) {
        if (gauntlet.isEmpty() || gauntlet.getItem() != MemoriaMagicite.GAUNTLET.get()) return 0;
        return Math.max(1, Math.min(4, gauntlet.getOrCreateTag().getInt(TIER_KEY)));
    }
    public static ItemStack[] read(ItemStack gauntlet) {
        ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        if (gauntlet.isEmpty() || gauntlet.getItem() != MemoriaMagicite.GAUNTLET.get()) return slots;
        ListTag list = gauntlet.getOrCreateTag().getList(INVENTORY_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < 4) {
                ItemStack candidate = ItemStack.of(entry);
                if (candidate.getItem() instanceof MagiciteItem && !candidate.isEmpty()) slots[slot] = candidate;
            }
        }
        return slots;
    }
    public static void write(ItemStack gauntlet, ItemStack[] slots) {
        if (gauntlet.isEmpty() || gauntlet.getItem() != MemoriaMagicite.GAUNTLET.get()) return;
        ListTag list = new ListTag();
        for (int i = 0; i < 4; i++) {
            if (slots[i].isEmpty() || !(slots[i].getItem() instanceof MagiciteItem)) continue;
            CompoundTag item = slots[i].save(new CompoundTag());
            item.putByte("Slot", (byte) i);
            list.add(item);
        }
        gauntlet.getOrCreateTag().put(INVENTORY_KEY, list);
    }
    public static boolean upgrade(ServerPlayer player, ItemStack gauntlet) {
        int tier = tier(gauntlet);
        if (tier < 1 || tier >= 4) return false;
        int cost = tier == 1 ? MagiciteConfig.TIER_TWO_XP.get() : tier == 2 ? MagiciteConfig.TIER_THREE_XP.get() : MagiciteConfig.TIER_FOUR_XP.get();
        if (!player.getAbilities().instabuild && player.experienceLevel < cost) return false;
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-cost);
        gauntlet.getOrCreateTag().putInt(TIER_KEY, tier + 1);
        return true;
    }
}
