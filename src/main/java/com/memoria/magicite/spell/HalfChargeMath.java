package com.memoria.magicite.spell;

public final class HalfChargeMath {
    private HalfChargeMath() {}
    public static int totalUnits(int maxDamage) { return Math.max(0,maxDamage)*2; }
    public static int usedUnits(int damage, int halfUsed) { return Math.max(0,damage)*2+Math.max(0,Math.min(1,halfUsed)); }
    public static int consumedFullDamage(int usedHalfUnits) { return Math.max(0,usedHalfUnits)/2; }
    public static int leftoverHalf(int usedHalfUnits) { return Math.max(0,usedHalfUnits)%2; }
    public static boolean breaks(int oldUsed, int cost, int maxDamage) {return oldUsed+cost >= totalUnits(maxDamage);}
}
