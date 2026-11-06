package com.memoria.magicite.spell;

public final class QuarterChargeMath {
    private QuarterChargeMath() {}
    public static int total(int maxDamage) { return Math.max(0,maxDamage)*4; }
    public static int used(int damage, int remainder) { return Math.max(0,damage)*4+Math.max(0,Math.min(3,remainder)); }
    public static int fullDamage(int used) {return Math.max(0,used)/4;}
    public static int remainder(int used) {return Math.max(0,used)%4;}
    public static boolean enough(int used, int cost, int durability) {return cost>0 && used+cost<=total(durability);}
    public static boolean breaks(int used,int cost,int maxDamage) {return used+cost>=total(maxDamage);}
}
