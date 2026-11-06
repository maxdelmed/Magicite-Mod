package com.memoria.magicite.spell;

public final class CooperativeBalance {
    private CooperativeBalance() {}
    public static float damageMultiplier(int players, double two, double three, double four) {
        return (float)(players >= 4 ? four : players == 3 ? three : players == 2 ? two : 1d);
    }
    public static float radiusBonus(int players, double perAdditionalPlayer) {
        return (float)(Math.max(0, Math.min(4, players) - 1) * Math.max(0d, perAdditionalPlayer));
    }
    public static float healAmount(float base, float missingHp, double missingFraction,
                                   double bonusCap, int players, double coopPerAdditionalPlayer) {
        float scaled = base + (float)Math.min(Math.max(0d, bonusCap), Math.max(0f, missingHp) * Math.max(0d, missingFraction));
        return scaled * (float)(1d + Math.max(0, Math.min(4, players)-1) * Math.max(0d, coopPerAdditionalPlayer));
    }
    public static int extendedDuration(int baseTicks, int players, int perAdditionalPlayer) {
        return baseTicks + Math.max(0, Math.min(4, players) - 1) * Math.max(0, perAdditionalPlayer);
    }
}
