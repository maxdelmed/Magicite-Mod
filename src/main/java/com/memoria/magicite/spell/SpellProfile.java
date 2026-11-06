package com.memoria.magicite.spell;

import java.util.List;

public final class SpellProfile {
    private SpellProfile() {}
    public static final int SINGLE_CHARGE = 22;
    public static final int GAUNTLET_TIER_1 = 18;
    public static final int GAUNTLET_TIER_2 = 26;
    public static final int GAUNTLET_TIER_3 = 32;
    public static final int GAUNTLET_TIER_4 = 38;
    public static final int SACRIFICE_CHARGE = 39;

    public static int chargeTicks(int tier, boolean sacrificial) {
        if (sacrificial) return SACRIFICE_CHARGE;
        return switch (tier) { case 1 -> GAUNTLET_TIER_1; case 2 -> GAUNTLET_TIER_2; case 3 -> GAUNTLET_TIER_3;
            default -> GAUNTLET_TIER_4; };
    }
    public static int baseChargeTicks() { return SINGLE_CHARGE; }

    public static float radius(MagiciteKind kind) {
        return kind.isSupport() ? 0.90f : 2.2f;
    }
    public static float radius(String spell) {
        if(spell.startsWith("firaga") || spell.startsWith("blizzaga") || spell.startsWith("thundaga"))
            return spell.endsWith("plus2") ? 5.5f : spell.endsWith("plus1") ? 4.8f : 4.2f;
        if(spell.startsWith("fira") || spell.startsWith("blizzara") || spell.startsWith("thundara"))
            return spell.endsWith("plus2") ? 3.9f : spell.endsWith("plus1") ? 3.5f : 3.0f;
        return switch (spell) {
            case "gravity" -> 3.0f;
            case "gravira", "graviga" -> 5.0f;
            case "curaga", "clearaga", "hastega" -> 4.5f;
            case "holy", "slow", "haste", "full_life" -> 1.5f;
            case "sanctuary_of_memories" -> 6.5f;
            case "holyra", "slowga", "stop" -> 3.6f;
            case "holyra_plus1" -> 5.0f;
            default -> 2.2f;
        };
    }
    public static float radius(List<MagiciteKind> ingredients) {
        String spell = FusionRules.result(ingredients);
        if (!spell.isEmpty()) {
            float extra=(spell.equals("curaga") || spell.equals("clearaga") || spell.equals("full_life"))
                    ? Math.max(0,ingredients.size()-2)*0.65f : 0f;
            return radius(spell)+extra;
        }
        float max = 0;
        for (MagiciteKind kind : ingredients) max = Math.max(max, radius(kind));
        return max;
    }

    public static float radius(List<MagiciteKind> ingredients, boolean unstable) {
        return radius(ingredients) + (unstable && !FusionRules.result(ingredients).isEmpty() ? 0.65f : 0f);
    }
    public static float radius(String spell, boolean unstable) {
        return radius(spell) + (unstable ? 0.65f : 0f);
    }
    public static float damageMultiplier(boolean unstable) {
        return unstable ? 1.35f : 1.0f;
    }
    public static int color(List<MagiciteKind> kinds) {
        if (kinds.size() == 1) return kinds.get(0).color;
        String spell = FusionRules.result(kinds);
        if (spell.startsWith("gravi") || spell.equals("gravity")) return 0x9C65D8;
        if (spell.equals("sanctuary_of_memories")) return 0xF6E7FF;
        return kinds.isEmpty() ? 0xD4A7F7 : kinds.get(0).color;
    }
    public static boolean circlesOverlap(double planarDistance, double radiusA, double radiusB) {
        return planarDistance <= radiusA + radiusB;
    }
}
