package com.memoria.magicite.spell;

public final class EnvironmentBalance {
    private EnvironmentBalance() {}
    public static float thunder(float base, boolean wet, boolean thunderstorm,
                                double wetBonus, double stormBonus) {
        return (float)(base * (1d + (wet ? wetBonus : 0d) + (thunderstorm ? stormBonus : 0d)));
    }
    public static float fire(float base, boolean nether, double netherBonus) {
        return (float)(base * (1d + (nether ? netherBonus : 0d)));
    }
}
