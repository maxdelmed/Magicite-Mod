package com.memoria.magicite.spell;

import java.util.List;

public final class SpellBalance {
    private SpellBalance() {}

    public static float elementalDamage(String spell, float dual, float triple,
                                        float minorPlusOne, float minorPlusTwo,
                                        float majorPlusOne, float majorPlusTwo) {
        boolean large = spell.startsWith("firaga") || spell.startsWith("blizzaga") || spell.startsWith("thundaga");
        if (spell.endsWith("plus2")) return large ? majorPlusTwo : minorPlusTwo;
        if (spell.endsWith("plus1")) return large ? majorPlusOne : minorPlusOne;
        return large ? triple : dual;
    }

    public static int cooldownTicks(int contributions, boolean hasCure, boolean hasLife,
                                    int one, int two, int three, int four, int cureMin, int lifeMin) {
        int base = switch (Math.max(1, Math.min(4, contributions))) {
            case 2 -> two;
            case 3 -> three;
            case 4 -> four;
            default -> one;
        };
        if (hasCure) base = Math.max(base, cureMin);
        if (hasLife) base = Math.max(base, lifeMin);
        return base;
    }

    public static int cooldownForSpell(String resolvedSpell, List<MagiciteKind> ingredients,
                                       int one, int two, int three, int four,
                                       int cureMin, int lifeMin) {
        if (ingredients == null || ingredients.isEmpty()) return one;
        boolean singleCure = ingredients.size() == 1 && ingredients.get(0) == MagiciteKind.CURE;
        boolean singleLife = ingredients.size() == 1 && ingredients.get(0) == MagiciteKind.LIFE;
        boolean healing = singleCure || "curaga".equals(resolvedSpell);
        boolean lifeWard = singleLife || "full_life".equals(resolvedSpell);
        return cooldownTicks(ingredients.size(), healing, lifeWard,
                one, two, three, four, cureMin, lifeMin);
    }

    public static double downwardVelocity(double oldY, double impulse) {
        return Math.max(-1.25d, Math.min(0d, oldY) - Math.max(0d, impulse));
    }

    public static boolean controlResistant(float maxHealth, boolean vanillaBoss,
                                            boolean taggedBoss, double threshold) {
        return vanillaBoss || taggedBoss || maxHealth >= threshold;
    }
}
