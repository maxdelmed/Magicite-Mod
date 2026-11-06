package com.memoria.magicite.item;

import com.memoria.magicite.MemoriaMagicite;
import com.memoria.magicite.spell.QuarterChargeMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class MagiciteCharge {
    private MagiciteCharge() {}
    public static final String HALF_KEY="MemoriaHalfChargeUsed";
    public static final String QUARTER_KEY="MemoriaQuarterChargeUsed";
    public static int used(ItemStack stack) {
        int remainder=0;
        if(stack.hasTag()) {
            if(stack.getTag().contains(QUARTER_KEY)) remainder=stack.getTag().getInt(QUARTER_KEY);
            else remainder=stack.getTag().getInt(HALF_KEY)*2;
        }
        return QuarterChargeMath.used(stack.getDamageValue(), remainder);
    }
    public static int remaining(ItemStack stack) {
        return Math.max(0,QuarterChargeMath.total(stack.getMaxDamage())-used(stack));
    }
    public static boolean usable(ItemStack crystal) {return canAfford(crystal,1);}
    public static boolean canAfford(ItemStack crystal,int quarters) {
        return !crystal.isEmpty() && crystal.getItem() instanceof MagiciteItem && remaining(crystal)>=quarters;
    }

    public static boolean consume(ItemStack crystal,ServerPlayer player,int quarters) {
        if (!canAfford(crystal,quarters)) return false;
        int units=used(crystal)+quarters;
        if (units >= QuarterChargeMath.total(crystal.getMaxDamage())) {
            crystal.shrink(1);
            player.serverLevel().playSound(null,player.blockPosition(),MemoriaMagicite.SHATTER.get(), SoundSource.PLAYERS,.65f,1.1f);
        } else {
            crystal.setDamageValue(QuarterChargeMath.fullDamage(units));
            crystal.getOrCreateTag().putInt(QUARTER_KEY,QuarterChargeMath.remainder(units));
            crystal.getOrCreateTag().remove(HALF_KEY);
        }
        return true;
    }
    public static void repair(ItemStack crystal,int damagePoints) {
        int restored=Math.max(0,used(crystal)-4*Math.max(0,damagePoints));
        crystal.setDamageValue(QuarterChargeMath.fullDamage(restored));
        crystal.getOrCreateTag().putInt(QUARTER_KEY,QuarterChargeMath.remainder(restored));
        crystal.getOrCreateTag().remove(HALF_KEY);
    }
}
