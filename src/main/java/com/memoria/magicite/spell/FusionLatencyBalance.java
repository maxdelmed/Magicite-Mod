package com.memoria.magicite.spell;

public final class FusionLatencyBalance {
    private FusionLatencyBalance() {}
    public static boolean synchronizedRelease(long tickA,long tickB,int baseWindow,int configGrace,int pingA,int pingB) {
        int grace=(pingA>100 || pingB>100)?Math.min(2,Math.max(0,configGrace)):0;
        long delta=tickA>=tickB?tickA-tickB:tickB-tickA;
        return delta<=Math.max(0,baseWindow)+grace;
    }
}
