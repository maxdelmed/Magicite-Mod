package com.memoria.magicite.spell;

public final class SacrificeBalance {
    private SacrificeBalance() {}

    public static float healthCostHp(float maxHealthHp, float fraction) {
        if (!Float.isFinite(maxHealthHp) || !Float.isFinite(fraction)
                || maxHealthHp <= 0f || fraction <= 0f)
            throw new IllegalArgumentException("Maximum health and fraction must be finite and positive");
        return maxHealthHp * fraction;
    }

    public static float projectedHealthHp(float currentHp, float maximumHp, float fraction) {
        return currentHp - healthCostHp(maximumHp, fraction);
    }
}
