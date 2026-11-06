package com.memoria.magicite.spell;

public final class BlizzardBalance {
    private BlizzardBalance() {}
    public static int freezeRadius(int spellTier) {

        return spellTier <= 1 ? 1 : spellTier <= 3 ? 2 : 3;
    }
    public static boolean inSoloPatch(int tier, int x, int z) {
        int radius = freezeRadius(tier);
        int squared = x*x + z*z;
        if (tier <= 1) return Math.abs(x) <= 1 && Math.abs(z) <= 1;

        return squared <= radius*radius + (tier >= 3 ? 1 : 0);
    }
    public static int patchBlockCount(int tier) {
        int radius = freezeRadius(tier), count = 0;
        for (int x=-radius; x<=radius; x++) for (int z=-radius; z<=radius; z++)
            if (inSoloPatch(tier, x, z)) count++;
        return count;
    }
    public static boolean canBridge(int tier,int distinctPlayers) {
        return distinctPlayers >= 2 && tier >= 2;
    }
    public static int bridgeLength(int tier,int distinctPlayers) {
        if(!canBridge(tier,distinctPlayers))return 0;
        int players=Math.max(2,Math.min(4,distinctPlayers));
        return switch(players) { case 2 -> 16; case 3 -> 24; default -> 32; };
    }
    public static int bridgeWidth(int tier,int players) {
        if(!canBridge(tier,players))return 0;
        return players >= 4 ? 3 : players == 3 ? 2 : 1;
    }
    public static int bridgeDurationTicks(int players) {
        return players >= 4 ? 1200 : players == 3 ? 1000 : players == 2 ? 800 : 500;
    }
}
