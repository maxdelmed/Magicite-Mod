package com.memoria.magicite.spell;

public final class HolyBalance {
    private HolyBalance() {}
    public static float adjustedDamage(float baseHp, boolean undead, float undeadMultiplier) {
        if (baseHp < 0f || undeadMultiplier < 1f) throw new IllegalArgumentException("Invalid Holy damage settings");
        return undead ? baseHp * undeadMultiplier : baseHp;
    }
}
